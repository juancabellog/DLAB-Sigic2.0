import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AnalysisService, GraphQueryRequest, GraphResponse, GraphNode, GraphLink } from '../../../core/services/analysis.service';
import { MessageService } from '../../../core/services/message.service';

// ECharts se importará dinámicamente
let echarts: any = null;

@Component({
  selector: 'app-research-knowledge-map',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './research-knowledge-map.component.html',
  styleUrls: ['./research-knowledge-map.component.scss']
})
export class ResearchKnowledgeMapComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('graphContainer', { static: true }) graphContainer!: ElementRef;
  
  private chartInstance: any;
  loading = false;
  
  // Filtros
  selectedPeriods: number[] = [];
  availablePeriods: any[] = [];
  
  // Datos del grafo
  graphData: GraphResponse | null = null;
  areas: { [key: number]: string } = {};
  
  // Métricas
  metrics = {
    nodeCount: 0,
    linkCount: 0,
    categoryCount: 0
  };
  
  fontSize = 8;
  selectedLegends: boolean[] | null = null;
  private resizeHandler?: () => void;
  filtersPanelOpen = false;

  constructor(
    private analysisService: AnalysisService,
    private messageService: MessageService,
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
    this.chartInstance.on('legendselectchanged', (params: any) => {
      this.selectedLegends = [];
      for (const key in params.selected) {
        this.selectedLegends.push(params.selected[key]);
      }
      this.hideKeywords();
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
        // Cargar el grafo automáticamente
        setTimeout(() => {
          this.loadGraph();
        }, 500);
      },
      error: (error: any) => {
        console.error('Error loading periods:', error);
      }
    });
  }

  loadGraph(): void {
    if (!echarts || !this.chartInstance) {
      this.messageService.error('Chart is not initialized. Please wait for ECharts to load.');
      return;
    }

    this.loading = true;
    this.chartInstance.clear();
    
    const request: GraphQueryRequest = {
      type: 3, // Research Line Graph
      periods: this.selectedPeriods.length > 0 ? this.selectedPeriods : undefined
    };

    this.analysisService.queryGraph(request).subscribe({
      next: (response) => {
        console.log('Response:', response);
        this.graphData = response;
        
        // Construir mapa de áreas
        if (response.categories) {
          response.categories.forEach((cat: any) => {
            if (cat.id !== 0 && cat.descripcion) {
              this.areas[cat.id] = cat.descripcion;
            }
          });
        }
        
        this.setResearchLineGraph(response);
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

  private prepareNodesResearchLine(nodes: any[], links: any[]): void {
    if (!this.graphData || !this.graphData.categories) return;
    
    const result = this.graphData;
    const areas: number[] = this.selectedLegends ? [] : result.categories!.map((c: any) => c.id).filter((id: number) => id !== 0);
    
    if (this.selectedLegends) {
      for (let i = 0; i < result.categories!.length; i++) {
        if (this.selectedLegends[i] && result.categories![i].id !== 0) {
          areas.push(result.categories![i].id);
        }
      }
    }
    
    // Reset idArea para keywords
    nodes.forEach((e: any) => {
      if (e.category === 0) {
        e.idArea = undefined;
      }
    });
    
    // Asignar idArea a keywords basado en las áreas de las líneas de investigación conectadas
    links.forEach((link: any) => {
      const keyword = nodes[link.target];
      const node = nodes[link.source];
      
      if (areas.includes(node.idArea)) {
        if (!keyword.idArea) {
          keyword.idArea = node.idArea;
        } else {
          keyword.idArea = keyword.idArea === node.idArea ? keyword.idArea : -1;
        }
      }
    });
    
    // Aplicar estilos basados en idArea
    nodes.forEach((node: any) => {
      if (node.idArea) {
        if (node.category > 0) {
          node.symbolSize = [70, 20];
        }
        const category = result.categories!.find((c: any) => c.id === node.idArea);
        node.itemStyle = {
          color: node.idArea === -1 ? '#999' : (category?.color || '#999')
        };
        if (node.category === 0 && node.idArea !== -1) {
          node.label = { color: '#666' };
        }
      }
    });
  }

  private setResearchLineGraph(result: GraphResponse): void {
    if (!result.nodes || !result.links || !result.categories) {
      console.error('Invalid graph data structure');
      return;
    }
    
    // Preparar nodos
    this.prepareNodesResearchLine(result.nodes, result.links);
    
    // Preparar categorías
    const categories = result.categories.map((cat: any) => {
      return {
        ...cat,
        itemStyle: { color: cat.id !== 0 ? cat.color : '#999' }
      };
    });
    
    const option = {
      tooltip: {
        show: true,
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            if (params.data.category > 0) {
              const areaName = this.areas[params.data.idArea] || 'Unknown Area';
              return `<span class="tooltip"><b>${areaName}</b><br>${params.data.descripcion || params.data.name}</span>`;
            } else {
              return params.data.name;
            }
          } else {
            const sourceNode = result.nodes[params.data.source];
            const targetNode = result.nodes[params.data.target];
            return `${sourceNode.name} - ${targetNode.name}`;
          }
        }
      },
      series: [{
        type: 'graph',
        layout: 'force',
        legendHoverLink: true,
        force: {
          edgeLength: 170,
          repulsion: 170,
          gravity: 0.2
        },
        roam: true,
        symbolSize: 18,
        edgeSymbolSize: [4, 10],
        label: {
          show: true,
          fontSize: this.fontSize
        },
        data: result.nodes,
        edges: result.links,
        categories: categories
      }],
      legend: [{
        data: categories.map((c: any) => c.name || c.descripcion)
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

  private hideKeywords(): void {
    if (!this.graphData || !this.selectedLegends) return;
    
    const currentOption = this.chartInstance.getOption();
    const data = currentOption.series[0].data;
    const links = currentOption.series[0].edges;
    
    if (!this.selectedLegends[0]) {
      return;
    }
    
    this.prepareNodesResearchLine(data, links);
    
    const keywords = new Set<number>();
    links.forEach((link: any) => {
      if (this.selectedLegends && this.selectedLegends[data[link.source].category]) {
        keywords.add(link.target);
      }
    });
    
    data.forEach((node: any) => {
      if (node.category === 0) {
        if (!node.itemStyle) {
          node.itemStyle = {};
        }
        if (keywords.has(node.id)) {
          node.symbolSize = 10;
          node.itemStyle.opacity = 1;
        } else {
          node.symbolSize = 0;
          node.itemStyle.opacity = 0;
        }
      }
    });
    
    this.chartInstance.setOption(currentOption);
  }

  private calculateMetrics(response: GraphResponse): void {
    this.metrics.nodeCount = response.nodes.length;
    this.metrics.linkCount = response.links.length;
    this.metrics.categoryCount = response.categories?.length || 0;
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

  toggleFiltersPanel(): void {
    this.filtersPanelOpen = !this.filtersPanelOpen;
    // Redimensionar el canvas cuando se abre/cierra el panel
    setTimeout(() => {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    }, 300); // Esperar a que termine la animación
  }

  exportPNG(): void {
    if (this.chartInstance) {
      const url = this.chartInstance.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff'
      });
      const link = document.createElement('a');
      link.download = 'research-knowledge-map.png';
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
