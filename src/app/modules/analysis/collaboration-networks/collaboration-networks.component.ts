import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { AnalysisService, GraphQueryRequest, GraphResponse, GraphNode, GraphLink } from '../../../core/services/analysis.service';
import { MessageService } from '../../../core/services/message.service';
import { PublicationDetailsDialogComponent } from './publication-details-dialog/publication-details-dialog.component';

// ECharts se importará dinámicamente
let echarts: any = null;

@Component({
  selector: 'app-collaboration-networks',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule
  ],
  templateUrl: './collaboration-networks.component.html',
  styleUrls: ['./collaboration-networks.component.scss']
})
export class CollaborationNetworksComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('graphContainer', { static: true }) graphContainer!: ElementRef;
  
  private chartInstance: any;
  loading = false;
  
  // Filtros
  selectedPeriods: number[] = [];
  selectedRRHHTypes: number[] = [];
  availablePeriods: any[] = [];
  availableRRHHTypes: any[] = [];
  roleSearchTerm: string = '';
  showAllRoles = false;
  
  // Roles relevantes por defecto (nombres que deben coincidir con descripcion)
  private readonly relevantRoleNames = [
    'Investigador Asociado',
    'Investigador Principal',
    'Investigador Principal Head',
    'Investigador Postdoctoral',
    'Estudiante Doctorado'
  ];
  
  // Datos del grafo
  graphData: GraphResponse | null = null;
  
  // Métricas
  metrics = {
    nodeCount: 0,
    linkCount: 0,
    centralNode: '',
    dominantCluster: '',
    density: 0
  };
  
  fontSize = 8;
  mouseDown = 0;
  hindex = false;
  citations = false;
  maxValueIndex = 0;
  selectedLegends: boolean[] | null = null;
  private resizeHandler?: () => void;
  filtersPanelOpen = false;

  constructor(
    private analysisService: AnalysisService,
    private messageService: MessageService,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFilters();
  }

  ngAfterViewInit(): void {
    // Importación dinámica de ECharts
    import('echarts').then((echartsModule) => {
      echarts = echartsModule;
      this.initChart();
      
      // Redimensionar cuando cambia el tamaño de la ventana
      this.resizeHandler = () => {
        if (this.chartInstance) {
          this.chartInstance.resize();
        }
      };
      window.addEventListener('resize', this.resizeHandler);
    }).catch((error) => {
      console.error('Error loading ECharts:', error);
      this.messageService.error('ECharts is not available. Please install: npm install echarts');
    });
  }

  ngOnDestroy(): void {
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
    // Remover listener de resize
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler);
    }
  }

  private initChart(): void {
    if (!this.graphContainer || !echarts) return;
    
    this.chartInstance = echarts.init(this.graphContainer.nativeElement);
    
    // Configurar eventos
    this.chartInstance.on('click', (params: any) => {
      if (params.dataType === 'edge') {
        this.showPublicationDetails(params);
      } else if (params.dataType === 'node' && this.mouseDown === 0) {
        this.mouseDown = 1;
        this.showSubgraph(params.data);
      }
    });

    // Reset al hacer click fuera
    this.graphContainer.nativeElement.addEventListener('click', (event: MouseEvent) => {
      if (event.target === this.graphContainer.nativeElement) {
        if (this.mouseDown === 2) {
          this.mouseDown = 0;
          this.showAllGraph();
        } else if (this.mouseDown === 1) {
          this.mouseDown = 2;
        }
      }
    });

    // Zoom handler
    this.chartInstance.on('graphRoam', (params: any) => {
      if (params.zoom != null) {
        const viewRoot = this.chartInstance.getZr().storage.getDisplayList();
        this.fontSize = Math.max(8, Math.min(20, this.fontSize + (params.zoom > 1 ? 0.1 : -0.1)));
        viewRoot.forEach((element: any) => {
          if (element.parent && element.parent.type === 'text') {
            element.style.font = `normal normal ${this.fontSize}px sans-serif`;
            if (element.parent.style) {
              element.parent.style.fontSize = this.fontSize;
            }
          }
        });
        this.chartInstance.getZr().refresh();
      }
    });
  }

  private loadFilters(): void {
    // Cargar períodos
    this.analysisService.getPeriods().subscribe({
      next: (periods: any[]) => {
        this.availablePeriods = periods;
      },
      error: (error: any) => {
        console.error('Error loading periods:', error);
      }
    });

    // Cargar tipos de RRHH
    this.analysisService.getRRHHTypes().subscribe({
      next: (types: any[]) => {
        this.availableRRHHTypes = types;
        // Seleccionar roles relevantes por defecto
        this.selectRelevantRoles();
        // Cargar el grafo automáticamente con los filtros predeterminados
        setTimeout(() => {
          this.loadGraph();
        }, 500);
      },
      error: (error: any) => {
        console.error('Error loading RRHH types:', error);
      }
    });
  }

  /**
   * Selecciona los roles relevantes por defecto
   */
  private selectRelevantRoles(): void {
    this.selectedRRHHTypes = [];
    this.availableRRHHTypes.forEach(type => {
      if (this.relevantRoleNames.some(relevantName => 
        type.descripcion?.includes(relevantName) || 
        type.descripcion === relevantName
      )) {
        this.selectedRRHHTypes.push(type.id);
      }
    });
  }

  loadGraph(): void {
    if (!echarts || !this.chartInstance) {
      this.messageService.error('Chart is not initialized. Please wait for ECharts to load.');
      return;
    }

    this.loading = true;
    this.mouseDown = 0;
    this.chartInstance.clear();
    
    const request: GraphQueryRequest = {
      type: 1, // RRHH Graph
      periods: this.selectedPeriods.length > 0 ? this.selectedPeriods : undefined,
      rrhhTypes: this.selectedRRHHTypes.length > 0 ? this.selectedRRHHTypes : undefined
    };

    this.analysisService.queryGraph(request).subscribe({
      next: (response) => {
        console.log('Response:', response);
        this.graphData = response;
        this.setRRHHGraph(response);
        this.calculateMetrics(response);
        this.loading = false;
        this.cdr.detectChanges();
        
        // Redimensionar el canvas después de que se renderice el grafo
        setTimeout(() => {
          if (this.chartInstance) {
            this.chartInstance.resize();
          }
        }, 100);
      },
      error: (error) => {
        console.error('Error loading graph:', error);
        this.messageService.error('Error loading graph');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private setRRHHGraph(result: GraphResponse): void {
    const categories = this.selectedRRHHTypes.length > 0
      ? this.availableRRHHTypes.filter(t => this.selectedRRHHTypes.includes(t.id))
      : this.availableRRHHTypes;

    const filterCategories: any[] = [];
    
    // Procesar nodos
    const nodes = result.nodes.map((node: any, idx: number) => {
      node.tiposRRHH = typeof node.tiposRRHH === 'string' 
        ? JSON.parse(node.tiposRRHH) 
        : node.tiposRRHH || [];
      node.name = `${node.id}-${node.name}`;
      node._id = node.id;
      node.id = idx;
      
      const matchingCategory = categories.find(cat => 
        node.tiposRRHH.includes(cat.id)
      );
      
      if (matchingCategory) {
        if (!filterCategories.find(c => c.id === matchingCategory.id)) {
          filterCategories.push({
            ...matchingCategory,
            name: matchingCategory.descripcion || matchingCategory.name
          });
        }
        node._category = matchingCategory;
      }
      
      return node;
    });

    filterCategories.sort((a, b) => a.name.localeCompare(b.name));

    const option = {
      tooltip: {
        show: true,
        formatter: (params: any) => {
          if (params.dataType === 'edge') {
            const sourceNode = result.nodes[params.data.source];
            const targetNode = result.nodes[params.data.target];
            return `<b>${sourceNode.initials}</b> (${params.data.weight}) <b>${targetNode.initials}</b>`;
          } else {
            const category = filterCategories[params.data.category];
            const indexProp = this.hindex ? 'hindex' : this.citations ? 'citations' : null;
            let tooltip = `<b>${category?.name || 'Sin categoría'}</b><br>${params.data.name}`;
            if (indexProp && params.data[indexProp]) {
              tooltip += `<br><b>${indexProp}:</b> ${params.data[indexProp]} / ${this.maxValueIndex}`;
            }
            return tooltip;
          }
        }
      },
      series: [{
        type: 'graph',
        layout: 'force',
        legendHoverLink: true,
        force: {
          edgeLength: 60,
          repulsion: 70,
          gravity: 0.1,
          layoutAnimation: true
        },
        draggable: false,
        roam: true,
        symbolSize: 18,
        edgeSymbolSize: [4, 10],
        label: {
          show: true,
          fontSize: this.fontSize,
          formatter: (params: any) => params.data.initials || params.data.name
        },
        data: nodes.map((node: any) => {
          node.category = filterCategories.findIndex(c => c.id === node._category?.id);
          if ([54, 55, 19].includes(node._category?.id)) {
            node.symbol = 'roundRect';
            node.symbolSize = 24;
          }
          return node;
        }),
        edges: result.links.map((link: any) => ({
          ...link,
          lineStyle: { width: link.weight || 1 }
        })),
        emphasis: {
          focus: 'adjacency'
        },
        categories: filterCategories
      }],
      legend: [{
        data: filterCategories.map(c => c.name)
      }]
    };

    this.chartInstance.setOption(option);
    
    // Redimensionar después de establecer la opción
    setTimeout(() => {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    }, 50);
  }

  private calculateMetrics(response: GraphResponse): void {
    this.metrics.nodeCount = response.nodes.length;
    this.metrics.linkCount = response.links.length;
    
    // Calcular densidad
    const n = response.nodes.length;
    const maxLinks = n * (n - 1) / 2;
    this.metrics.density = maxLinks > 0 ? response.links.length / maxLinks : 0;
    
    // Encontrar nodo central (más conexiones)
    const connectionCount: { [key: number]: number } = {};
    response.links.forEach(link => {
      connectionCount[link.source] = (connectionCount[link.source] || 0) + 1;
      connectionCount[link.target] = (connectionCount[link.target] || 0) + 1;
    });
    
    let maxConnections = 0;
    let centralNodeId = 0;
    Object.entries(connectionCount).forEach(([id, count]) => {
      if (count > maxConnections) {
        maxConnections = count;
        centralNodeId = parseInt(id);
      }
    });
    
    const centralNode = response.nodes.find(n => n.id === centralNodeId);
    this.metrics.centralNode = centralNode?.name || 'N/A';
  }

  private showSubgraph(data: any): void {
    if (!this.graphData) return;
    
    const neighbors = new Set<number>();
    this.graphData.links.forEach(link => {
      if (link.source === data.id) neighbors.add(link.target);
      if (link.target === data.id) neighbors.add(link.source);
    });

    const viewRoot = this.chartInstance.getZr().storage.getDisplayList();
    viewRoot.forEach((element: any) => {
      if (element.type === 'ec-line' || (element.type === 'path' && element.culling)) {
        let property: string | undefined;
        for (let i = 0; i < 100; i++) {
          if (element[`__ec_inner_${i}`]) {
            property = `__ec_inner_${i}`;
            break;
          }
        }
        
        if (property) {
          if (element.type === 'ec-line') {
            const link = this.graphData!.links[element[property].dataIndex];
            element.style.opacity = (link.source === data.id || link.target === data.id) ? 1 : 0.1;
          } else {
            const node = this.graphData!.nodes[element[property].dataIndex];
            element.style.opacity = (node.id === data.id || neighbors.has(node.id)) ? 1 : 0.1;
          }
        }
      }
    });
    
    this.chartInstance.getZr().wakeUp();
    this.chartInstance.getZr().refresh();
  }

  private showAllGraph(): void {
    const viewRoot = this.chartInstance.getZr().storage.getDisplayList();
    viewRoot.forEach((element: any) => {
      if (element.type === 'ec-line' || (element.type === 'path' && element.culling)) {
        element.style.opacity = 1;
      }
    });
    this.chartInstance.getZr().wakeUp();
    this.chartInstance.getZr().refresh();
  }

  private showPublicationDetails(params: any): void {
    if (!this.graphData) return;
    
    const sourceNode = this.graphData.nodes[params.data.source];
    const targetNode = this.graphData.nodes[params.data.target];
    
    const request = {
      filter: {
        type: 1,
        periods: this.selectedPeriods,
        from: sourceNode._id || sourceNode.id,
        to: targetNode._id || targetNode.id
      }
    };

    this.analysisService.queryPublications(request).subscribe({
      next: (publications) => {
        this.dialog.open(PublicationDetailsDialogComponent, {
          width: '800px',
          data: { publications, sourceNode, targetNode }
        });
      },
      error: (error) => {
        console.error('Error loading publications:', error);
        this.messageService.error('Error loading publications');
      }
    });
  }

  togglePeriod(periodId: number): void {
    const index = this.selectedPeriods.indexOf(periodId);
    if (index >= 0) {
      this.selectedPeriods.splice(index, 1);
    } else {
      this.selectedPeriods.push(periodId);
    }
    // Filtros reactivos: actualizar el grafo automáticamente
    this.loadGraph();
  }

  toggleRRHHType(typeId: number): void {
    const index = this.selectedRRHHTypes.indexOf(typeId);
    if (index >= 0) {
      this.selectedRRHHTypes.splice(index, 1);
    } else {
      this.selectedRRHHTypes.push(typeId);
    }
    // Filtros reactivos: actualizar el grafo automáticamente
    this.loadGraph();
  }

  showAllRolesToggle(): void {
    this.showAllRoles = !this.showAllRoles;
  }

  toggleFiltersPanel(): void {
    this.filtersPanelOpen = !this.filtersPanelOpen;
    // Redimensionar el canvas cuando se abre/cierra el panel
    setTimeout(() => {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    }, 300); // Esperar a que termine la animación
  }

  /**
   * Filtra los tipos de RRHH basado en el término de búsqueda y si mostrar todos
   */
  get filteredRRHHTypes(): any[] {
    let filtered = this.availableRRHHTypes;
    
    // Si no está en modo "mostrar todos", solo mostrar roles relevantes
    if (!this.showAllRoles) {
      filtered = filtered.filter(type => 
        this.relevantRoleNames.some(relevantName => 
          type.descripcion?.includes(relevantName) || 
          type.descripcion === relevantName
        )
      );
    }
    
    // Aplicar búsqueda si hay término
    if (this.roleSearchTerm && this.roleSearchTerm.trim() !== '') {
      const searchTerm = this.roleSearchTerm.toLowerCase().trim();
      filtered = filtered.filter(type => 
        type.descripcion?.toLowerCase().includes(searchTerm)
      );
    }
    
    return filtered;
  }
  
  /**
   * Obtiene los roles relevantes para mostrar por defecto
   */
  get relevantRoles(): any[] {
    return this.availableRRHHTypes.filter(type => 
      this.relevantRoleNames.some(relevantName => 
        type.descripcion?.includes(relevantName) || 
        type.descripcion === relevantName
      )
    );
  }

  clearRoleSearch(): void {
    this.roleSearchTerm = '';
  }

  exportPNG(): void {
    if (this.chartInstance) {
      const url = this.chartInstance.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff'
      });
      const link = document.createElement('a');
      link.download = 'collaboration-networks.png';
      link.href = url;
      link.click();
    }
  }

  resetView(): void {
    if (this.chartInstance) {
      this.chartInstance.dispatchAction({
        type: 'restore'
      });
    }
  }
}
