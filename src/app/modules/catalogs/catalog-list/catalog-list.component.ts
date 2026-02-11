import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';

import { CatalogService } from '../../../core/services/catalog.service';
import { TipoParticipacionDTO } from '../../../core/models/catalog-types';
import { GenericCatalogService } from '../../../core/services/generic-catalog.service';
import { MessageService } from '../../../core/services/message.service';
import { ListControlsComponent } from '../../../shared/components/list-controls/list-controls.component';
import { ViewMode } from '../../../shared/components/view-mode-selector/view-mode-selector.component';
import { ViewModeService } from '../../../core/services/view-mode.service';
import { CatalogType, CatalogTypeInfo, CatalogItem } from '../../../core/models/catalog-types';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-catalog-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatTabsModule,
    MatChipsModule,
    ListControlsComponent
  ],
  templateUrl: './catalog-list.component.html',
  styleUrls: ['./catalog-list.component.scss']
})
export class CatalogListComponent implements OnInit {
  viewMode: ViewMode = 'card';
  filteredItems: CatalogItem[] = [];
  isSearching: boolean = false;
  currentCatalogType: CatalogType = CatalogType.PARTICIPATION_TYPES;
  catalogTypes: CatalogTypeInfo[] = [];
  allItems: CatalogItem[] = [];
  selectedTabIndex: number = 0;
  currentAddRoute: string = '';
  currentBaseRoute: string = '';

  constructor(
    private catalogService: CatalogService,
    private genericCatalogService: GenericCatalogService,
    private messageService: MessageService,
    private viewModeService: ViewModeService,
    private route: ActivatedRoute,
    private router: Router
  ) {}
  
  
  participationTypes: TipoParticipacionDTO[] = [];
  filteredParticipationTypes: TipoParticipacionDTO[] = [];

  ngOnInit(): void {
    console.log('🚀 CatalogListComponent initialized');
    this.catalogTypes = this.genericCatalogService.getCatalogTypes();
    this.detectCatalogTypeFromRoute();
    this.loadCurrentCatalogType();
    // Get initial view mode
    this.viewModeService.getViewMode().subscribe(mode => {
      this.viewMode = mode;
    });
  }

  private detectCatalogTypeFromRoute(): void {
    const url = this.router.url;
    console.log('🔍 Detecting catalog type from URL:', url);
    
    // Detect catalog type from URL path
    if (url.includes('/catalogs/participation-types')) {
      this.currentCatalogType = CatalogType.PARTICIPATION_TYPES;
      console.log('✅ Detected catalog type: PARTICIPATION_TYPES');
    } else if (url.includes('/catalogs/product-types')) {
      this.currentCatalogType = CatalogType.PRODUCT_TYPES;
      console.log('✅ Detected catalog type: PRODUCT_TYPES');
    } else if (url.includes('/catalogs/researcher-types')) {
      this.currentCatalogType = CatalogType.RESEARCHER_TYPES;
      console.log('✅ Detected catalog type: RESEARCHER_TYPES');
    } else if (url.includes('/catalogs/journals')) {
      this.currentCatalogType = CatalogType.JOURNALS;
      console.log('✅ Detected catalog type: JOURNALS');
    } else {
      // Default to participation types for /catalogs
      this.currentCatalogType = CatalogType.PARTICIPATION_TYPES;
      console.log('⚠️ Default catalog type: PARTICIPATION_TYPES');
    }
    
    // Set the correct tab index based on detected catalog type
    this.setTabIndexFromCatalogType();
    
    // Update the add route
    this.updateCurrentAddRoute();
  }

  private setTabIndexFromCatalogType(): void {
    const index = this.catalogTypes.findIndex(ct => ct.type === this.currentCatalogType);
    this.selectedTabIndex = index >= 0 ? index : 0;
    console.log('📑 Set tab index to:', this.selectedTabIndex, 'for catalog type:', this.currentCatalogType);
  }

  private updateCurrentAddRoute(): void {
    const info = this.catalogTypes.find(ct => ct.type === this.currentCatalogType);
    this.currentBaseRoute = info?.route || '';
    this.currentAddRoute = this.currentBaseRoute + '/new';
    console.log('🔗 Updated currentAddRoute to:', this.currentAddRoute, 'for catalog type:', this.currentCatalogType);
  }

  loadCurrentCatalogType(): void {
    console.log('📦 Loading catalog type:', this.currentCatalogType);
    this.genericCatalogService.getItems(this.currentCatalogType).subscribe({
      next: (items) => {
        console.log('✅ Loaded items for', this.currentCatalogType, ':', items.length, 'items');
        console.log('📋 Items data:', items);
        // Log each item to see the structure
        items.forEach((item, index) => {
          console.log(`📄 Item ${index}:`, {
            id: item.id,
            nombre: item.nombre,
            descripcion: item.descripcion,
            tipoProductoNombre: (item as any).tipoProductoNombre
          });
        });
        this.allItems = items;
        this.filteredItems = items;
      },
      error: (error) => {
        console.error(`Error loading ${this.currentCatalogType}:`, error);
        this.allItems = [];
        this.filteredItems = [];
      }
    });
  }

  loadParticipationTypes(): void {
    this.catalogService.getParticipationTypes().subscribe({
      next: (types) => {
        this.participationTypes = types;
        this.filteredParticipationTypes = types;
      },
      error: (error) => {
        console.error('Error loading participation types:', error);
        // Use default types if service fails
        this.participationTypes = this.getDefaultParticipationTypes();
        this.filteredParticipationTypes = this.participationTypes;
      }
    });
  }


  deleteParticipationType(type: TipoParticipacionDTO): void {
    this.messageService.confirm(
      `Are you sure you want to delete participation type "${type.nombre}"?`,
      (accepted: boolean) => {
        if (accepted) {
          const index = this.participationTypes.findIndex(t => t.id === type.id);
          if (index > -1) {
            this.participationTypes.splice(index, 1);
            this.messageService.success(`Participation type "${type.nombre}" has been successfully removed.`);
          }
        }
      },
      'Delete Participation Type'
    );
  }


  private getDefaultParticipationTypes(): TipoParticipacionDTO[] {
    return [
      {
        id: 1,
        nombre: 'Author',
        descripcion: 'Main author of the publication',
        activo: true,
        idDescripcion: 'PARTICIPATION_001',
        idTipoProducto: 1,
        tipoProductoNombre: 'Publications',
        aplicableProductos: 'PUBLICATIONS',
        puedeSerCorresponding: true
      },
      {
        id: 2,
        nombre: 'Co-Author',
        descripcion: 'Collaborating author',
        activo: true,
        idDescripcion: 'PARTICIPATION_002',
        idTipoProducto: 1,
        tipoProductoNombre: 'Publications',
        aplicableProductos: 'PUBLICATIONS',
        puedeSerCorresponding: true
      },
      {
        id: 3,
        nombre: 'Principal Investigator',
        descripcion: 'Lead researcher of the project',
        activo: true,
        idDescripcion: 'PARTICIPATION_003',
        idTipoProducto: 2,
        tipoProductoNombre: 'Research Projects',
        aplicableProductos: 'ALL',
        puedeSerCorresponding: false
      },
      {
        id: 4,
        nombre: 'Co-Investigator',
        descripcion: 'Collaborating researcher',
        activo: true,
        idDescripcion: 'PARTICIPATION_004',
        idTipoProducto: 2,
        tipoProductoNombre: 'Research Projects',
        aplicableProductos: 'ALL',
        puedeSerCorresponding: false
      },
      {
        id: 5,
        nombre: 'Supervisor',
        descripcion: 'Academic supervisor for thesis students',
        activo: true,
        idDescripcion: 'PARTICIPATION_005',
        idTipoProducto: 3,
        tipoProductoNombre: 'Thesis Students',
        aplicableProductos: 'THESIS_STUDENTS',
        puedeSerCorresponding: false
      }
    ];
  }


  getApplicableProductsColor(aplicableProductos: string): 'primary' | 'accent' | 'warn' {
    switch (aplicableProductos) {
      case 'ALL': return 'primary';
      case 'PUBLICATIONS': return 'accent';
      case 'THESIS_STUDENTS': return 'primary';
      case 'TECHNOLOGY_TRANSFER': return 'warn';
      default: return 'primary';
    }
  }

  // Generic methods for all catalog types


  switchCatalogType(catalogType: CatalogType): void {
    this.currentCatalogType = catalogType;
    this.loadCurrentCatalogType();
  }

  onTabChange(event: any): void {
    const selectedIndex = event.index;
    const selectedCatalogType = this.catalogTypes[selectedIndex];
    console.log('🔄 Tab changed to:', selectedCatalogType.name, 'Type:', selectedCatalogType.type);
    console.log('🔄 Tab change details:', {
      selectedIndex,
      selectedCatalogType,
      currentCatalogTypeBefore: this.currentCatalogType
    });
    
    if (selectedCatalogType) {
      this.currentCatalogType = selectedCatalogType.type;
      this.currentBaseRoute = selectedCatalogType.route;
      this.currentAddRoute = selectedCatalogType.route + '/new';
      console.log('🔄 Updated currentCatalogType to:', this.currentCatalogType);
      console.log('🔄 Updated currentAddRoute to:', this.currentAddRoute);
      this.loadCurrentCatalogType();
    }
  }

  onSearchResults(filteredItems: any[]): void {
    this.filteredItems = filteredItems;
    this.isSearching = filteredItems.length < this.allItems.length;
  }

  onSearchTermChange(searchTerm: string): void {
    this.isSearching = searchTerm.length > 0;
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.viewModeService.setViewMode(mode);
  }

  // Generic delete method for all catalog types
  deleteItem(item: CatalogItem): void {
    this.messageService.confirm(
      `Are you sure you want to delete "${item.nombre}"?`,
      (confirmed: boolean) => {
        if (confirmed) {
          this.genericCatalogService.deleteItem(this.currentCatalogType, item.id).subscribe({
            next: () => {
              this.messageService.success(`Item deleted successfully`);
              this.loadCurrentCatalogType(); // Reload the list
            },
            error: (error) => {
              console.error('Error deleting item:', error);
              this.messageService.error('Error deleting item');
            }
          });
        }
      },
      'Delete Item'
    );
  }

  // Helper methods for template
  getItemCodigo(item: CatalogItem): string {
    return (item as any).codigo || '';
  }

  getItemAplicableProductos(item: CatalogItem): string {
    return (item as any).aplicableProductos || '';
  }

  getItemPuedeSerCorresponding(item: CatalogItem): boolean {
    return (item as any).puedeSerCorresponding || false;
  }

  hasCodigo(item: CatalogItem): boolean {
    return !!(item as any).codigo;
  }

  hasAplicableProductos(item: CatalogItem): boolean {
    return !!(item as any).aplicableProductos;
  }

  hasPuedeSerCorresponding(item: CatalogItem): boolean {
    return (item as any).puedeSerCorresponding !== undefined;
  }

  getTipoProductoNombre(item: CatalogItem): string {
    const tipoProductoNombre = (item as any).tipoProductoNombre;
    console.log('🔍 getTipoProductoNombre for item:', item.id, '-> tipoProductoNombre:', tipoProductoNombre);
    return tipoProductoNombre || 'Not specified';
  }

  // Helper methods for safe property access
  getItemAbbreviation(item: CatalogItem): string {
    return (item as any).abbreviation || 'N/A';
  }

  getItemIssn(item: CatalogItem): string {
    return (item as any).issn || 'N/A';
  }

  // Check if current catalog type is read-only
  isReadOnlyCatalogType(): boolean {
    // These catalog types are read-only (view only)
    return this.currentCatalogType === CatalogType.PARTICIPATION_TYPES ||
           this.currentCatalogType === CatalogType.PRODUCT_TYPES ||
           this.currentCatalogType === CatalogType.RESEARCHER_TYPES ||
           this.currentCatalogType === CatalogType.JOURNALS;
  }

  // Legacy methods for participation types (to maintain compatibility)
  onSearchResultsLegacy(filteredItems: any[]): void {
    this.filteredParticipationTypes = filteredItems;
    this.isSearching = filteredItems.length < this.participationTypes.length;
  }
}

