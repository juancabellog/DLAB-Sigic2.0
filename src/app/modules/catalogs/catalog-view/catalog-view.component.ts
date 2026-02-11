import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MessageService } from '../../../core/services/message.service';
import { TipoParticipacionDTO } from '../../../core/models/catalog-types';
import { GenericCatalogService } from '../../../core/services/generic-catalog.service';
import { CatalogType, CatalogItem } from '../../../core/models/catalog-types';

@Component({
  selector: 'app-catalog-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './catalog-view.component.html',
  styleUrls: ['./catalog-view.component.scss']
})
export class CatalogViewComponent implements OnInit {
  catalog: CatalogItem | null = null;
  catalogId: string | null = null;
  loading: boolean = true;
  currentCatalogType: CatalogType = CatalogType.PARTICIPATION_TYPES;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private genericCatalogService: GenericCatalogService
  ) {}

  ngOnInit(): void {
    this.detectCatalogTypeFromRoute();
    this.catalogId = this.route.snapshot.paramMap.get('id');
    this.loadCatalog();
  }

  private detectCatalogTypeFromRoute(): void {
    const url = this.router.url;
    
    // Detect catalog type from URL path
    if (url.includes('/catalogs/participation-types')) {
      this.currentCatalogType = CatalogType.PARTICIPATION_TYPES;
    } else if (url.includes('/catalogs/product-types')) {
      this.currentCatalogType = CatalogType.PRODUCT_TYPES;
    } else if (url.includes('/catalogs/researcher-types')) {
      this.currentCatalogType = CatalogType.RESEARCHER_TYPES;
    } else if (url.includes('/catalogs/journals')) {
      this.currentCatalogType = CatalogType.JOURNALS;
    } else {
      // Default to participation types for /catalogs
      this.currentCatalogType = CatalogType.PARTICIPATION_TYPES;
    }
  }

  private loadCatalog(): void {
    if (this.catalogId) {
      console.log('Loading catalog item:', {
        catalogType: this.currentCatalogType,
        catalogId: this.catalogId,
        catalogIdNumber: parseInt(this.catalogId)
      });
      
      this.loading = true;
      
      this.genericCatalogService.getItem(this.currentCatalogType, parseInt(this.catalogId)).subscribe({
        next: (item) => {
          console.log('Catalog item loaded:', item);
          this.catalog = item || null;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading catalog item:', error);
          this.messageService.error('Error loading catalog item');
          this.loading = false;
        }
      });
    } else {
      console.warn('Missing catalogId:', this.catalogId);
    }
  }

  goBack(): void {
    // Navigate back to the appropriate catalog type list
    const catalogTypeInfo = this.genericCatalogService.getCatalogTypes().find(ct => ct.type === this.currentCatalogType);
    if (catalogTypeInfo) {
      this.router.navigate([catalogTypeInfo.route]);
    } else {
      this.router.navigate(['/catalogs']);
    }
  }

  editCatalog(): void {
    if (this.catalog) {
      const catalogTypeInfo = this.genericCatalogService.getCatalogTypes().find(ct => ct.type === this.currentCatalogType);
      if (catalogTypeInfo) {
        this.router.navigate([catalogTypeInfo.route, this.catalog.id, 'edit']);
      } else {
        this.router.navigate(['/catalogs', this.catalog.id, 'edit']);
      }
    }
  }

  deleteCatalog(): void {
    if (this.catalog) {
      this.messageService.confirm(
        `Are you sure you want to delete "${this.catalog.nombre}"?`,
        (confirmed: boolean) => {
          if (confirmed) {
            this.genericCatalogService.deleteItem(this.currentCatalogType, this.catalog!.id).subscribe({
              next: () => {
                this.messageService.success('Item deleted successfully');
                this.goBack();
              },
              error: (error) => {
                console.error('Error deleting catalog item:', error);
                this.messageService.error('Error deleting catalog item');
              }
            });
          }
        },
        'Delete Item'
      );
    }
  }

  getCatalogTypeName(): string {
    switch (this.currentCatalogType) {
      case CatalogType.PARTICIPATION_TYPES:
        return 'Participation Types';
      case CatalogType.PRODUCT_TYPES:
        return 'Product Types';
      case CatalogType.RESEARCHER_TYPES:
        return 'Researcher Types';
      case CatalogType.JOURNALS:
        return 'Journals';
      default:
        return 'Catalogs';
    }
  }

  // Helper methods for template
  getItemCodigo(item: CatalogItem): string {
    return (item as any).codigo || '';
  }

  hasCodigo(item: CatalogItem): boolean {
    return !!(item as any).codigo;
  }

  getItemAplicableProductos(item: CatalogItem): string {
    return (item as any).aplicableProductos || '';
  }

  getItemPuedeSerCorresponding(item: CatalogItem): boolean {
    return (item as any).puedeSerCorresponding || false;
  }

  getTipoProductoNombre(item: CatalogItem): string {
    return (item as any).tipoProductoNombre || 'Not specified';
  }

  // Helper methods for safe property access
  getItemAbbreviation(item: CatalogItem): string {
    return (item as any).abbreviation || 'N/A';
  }

  getItemIssn(item: CatalogItem): string {
    return (item as any).issn || 'N/A';
  }

  hasAplicableProductos(item: CatalogItem): boolean {
    return !!(item as any).aplicableProductos;
  }

  hasPuedeSerCorresponding(item: CatalogItem): boolean {
    return (item as any).puedeSerCorresponding !== undefined;
  }

  isReadOnlyCatalogType(): boolean {
    // These catalog types are read-only (view only)
    return this.currentCatalogType === CatalogType.PARTICIPATION_TYPES ||
           this.currentCatalogType === CatalogType.PRODUCT_TYPES ||
           this.currentCatalogType === CatalogType.RESEARCHER_TYPES ||
           this.currentCatalogType === CatalogType.JOURNALS;
  }
}
