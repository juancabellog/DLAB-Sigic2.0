import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CatalogService } from '../../../core/services/catalog.service';
import { MessageService } from '../../../core/services/message.service';
import { GenericCatalogService } from '../../../core/services/generic-catalog.service';
import { CatalogType, CatalogItem, TipoParticipacionDTO, TipoProductoDTO, EstadoProductoDTO, TipoInvestigadorDTO, RevistaDTO, TipoIndizacionDTO } from '../../../core/models/catalog-types';

@Component({
  selector: 'app-catalog-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './catalog-edit.component.html',
  styleUrls: ['./catalog-edit.component.scss']
})
export class CatalogEditComponent implements OnInit {
  // Variables para controlar el modo
  isEditMode: boolean = false;
  catalogId: number | null = null;
  loading: boolean = false;
  currentCatalogType: CatalogType = CatalogType.PARTICIPATION_TYPES;

  // Formulario genérico para todos los tipos de catálogo
  catalogForm = {
    codigo: '',
    nombre: '',
    descripcion: '',
    activo: true,
    // Campos específicos para diferentes tipos de catálogo
    esActivo: true,
    estadoInicial: false,
    issn: '',
    factorImpacto: 0,
    idTipoProducto: null as number | null,
    idDescripcion: '',
    tipoProductoNombre: '',
    aplicableProductos: 'ALL',
    puedeSerCorresponding: false,
    abbreviation: ''
  };

  // Lista de tipos de producto para el dropdown
  productTypes: any[] = [];

  // Exponer CatalogType enum para el template
  catalogTypeEnum = CatalogType;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private catalogService: CatalogService,
    private messageService: MessageService,
    private genericCatalogService: GenericCatalogService
  ) {}

  ngOnInit(): void {
    console.log('🚀 CatalogEditComponent initialized');
    this.detectCatalogTypeFromRoute();
    this.catalogId = this.route.snapshot.paramMap.get('id') ? 
      parseInt(this.route.snapshot.paramMap.get('id')!) : null;
    
    this.isEditMode = !!this.catalogId;
    
    console.log('📝 CatalogEditComponent state:', {
      currentCatalogType: this.currentCatalogType,
      catalogId: this.catalogId,
      isEditMode: this.isEditMode,
      url: this.router.url
    });
    
    // Cargar tipos de producto si es participation-types
    if (this.currentCatalogType === CatalogType.PARTICIPATION_TYPES) {
      console.log('🔄 Loading product types for participation types...');
      this.loadProductTypes();
    } else {
      console.log('⏭️ Skipping product types load for catalog type:', this.currentCatalogType);
    }
    
    if (this.isEditMode) {
      this.loadCatalog();
    }
  }

  private detectCatalogTypeFromRoute(): void {
    const url = this.router.url;
    console.log('🔍 CatalogEditComponent detecting catalog type from URL:', url);
    
    // Detect catalog type from URL path
    if (url.includes('/catalogs/participation-types')) {
      this.currentCatalogType = CatalogType.PARTICIPATION_TYPES;
      console.log('✅ CatalogEditComponent detected: PARTICIPATION_TYPES');
    } else if (url.includes('/catalogs/product-types')) {
      this.currentCatalogType = CatalogType.PRODUCT_TYPES;
      console.log('✅ CatalogEditComponent detected: PRODUCT_TYPES');
    } else if (url.includes('/catalogs/researcher-types')) {
      this.currentCatalogType = CatalogType.RESEARCHER_TYPES;
      console.log('✅ CatalogEditComponent detected: RESEARCHER_TYPES');
    } else if (url.includes('/catalogs/journals')) {
      this.currentCatalogType = CatalogType.JOURNALS;
      console.log('✅ CatalogEditComponent detected: JOURNALS');
    } else {
      // Default to participation types for /catalogs
      this.currentCatalogType = CatalogType.PARTICIPATION_TYPES;
      console.log('⚠️ CatalogEditComponent default: PARTICIPATION_TYPES');
    }
  }

  private loadCatalog(): void {
    if (this.catalogId) {
      console.log('Loading catalog item for edit:', {
        catalogType: this.currentCatalogType,
        catalogId: this.catalogId
      });
      
      this.loading = true;
      
      this.genericCatalogService.getItem(this.currentCatalogType, this.catalogId).subscribe({
        next: (item) => {
          console.log('Catalog item loaded for edit:', item);
          if (item) {
            this.catalogForm = {
              codigo: (item as any).codigo || '',
              nombre: item.nombre,
              descripcion: item.descripcion || '',
              activo: item.activo || true,
              // Campos específicos según el tipo
              esActivo: (item as any).esActivo || true,
              estadoInicial: (item as any).estadoInicial || false,
              issn: (item as any).issn || '',
              factorImpacto: (item as any).factorImpacto || 0,
              idTipoProducto: (item as any).idTipoProducto || null,
              idDescripcion: (item as any).idDescripcion || '',
              tipoProductoNombre: (item as any).tipoProductoNombre || '',
              aplicableProductos: (item as any).aplicableProductos || 'ALL',
              puedeSerCorresponding: (item as any).puedeSerCorresponding || false,
              abbreviation: (item as any).abbreviation || ''
            };
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading catalog item:', error);
          this.messageService.error('Error loading catalog item');
          this.loading = false;
        }
      });
    } else {
      console.warn('Missing catalogId for edit:', this.catalogId);
    }
  }

  saveCatalog(): void {
    if (!this.catalogForm.nombre.trim()) {
      this.messageService.error('El nombre es requerido');
      return;
    }
    if (!this.catalogForm.codigo.trim()) {
      this.messageService.error('El código es requerido');
      return;
    }

    this.loading = true;

    // Add the appropriate catalog item based on type
    const catalogData = this.addCatalogItem();

    const operation = this.isEditMode 
      ? this.genericCatalogService.updateItem(this.currentCatalogType, this.catalogId!, catalogData)
      : this.genericCatalogService.createItem(this.currentCatalogType, catalogData);

    operation.subscribe({
      next: () => {
        this.loading = false;
        const action = this.isEditMode ? 'actualizado' : 'creado';
        const typeName = this.getCatalogTypeName();
        this.messageService.success(`${typeName} ${action} exitosamente`);
        this.goBack();
      },
      error: (error: any) => {
        console.error('Error saving catalog item:', error);
        this.messageService.error('Error saving catalog item');
        this.loading = false;
      }
    });
  }

  private addCatalogItem(): CatalogItem {
    // Add specific fields based on catalog type
    switch (this.currentCatalogType) {
      case CatalogType.PARTICIPATION_TYPES:
        return {
          id: this.catalogId || 0,
          nombre: this.catalogForm.nombre.trim(),
          descripcion: this.catalogForm.descripcion.trim(),
          activo: this.catalogForm.activo,
          idDescripcion: this.catalogForm.idDescripcion || '',
          idTipoProducto: this.catalogForm.idTipoProducto || 0,
          tipoProductoNombre: this.catalogForm.tipoProductoNombre || '',
          aplicableProductos: this.catalogForm.aplicableProductos || 'ALL',
          puedeSerCorresponding: this.catalogForm.puedeSerCorresponding || false
        } as TipoParticipacionDTO;

      case CatalogType.PRODUCT_TYPES:
        return {
          id: this.catalogId || 0,
          nombre: this.catalogForm.nombre.trim(),
          descripcion: this.catalogForm.descripcion.trim(),
          activo: this.catalogForm.activo,
          idDescripcion: this.catalogForm.idDescripcion || ''
        } as TipoProductoDTO;

      case CatalogType.JOURNALS:
        return {
          id: this.catalogId || 0,
          nombre: this.catalogForm.nombre.trim(),
          descripcion: this.catalogForm.descripcion.trim(),
          activo: this.catalogForm.activo,
          idDescripcion: this.catalogForm.idDescripcion || '',
          abbreviation: this.catalogForm.abbreviation || '',
          issn: this.catalogForm.issn || ''
        } as RevistaDTO;

      case CatalogType.RESEARCHER_TYPES:
        return {
          id: this.catalogId || 0,
          codigo: this.catalogForm.codigo.trim(),
          nombre: this.catalogForm.nombre.trim(),
          descripcion: this.catalogForm.descripcion.trim(),
          activo: this.catalogForm.activo,
          nivel: 'PROFESSOR',
          requiereAfiliacion: true
        } as TipoInvestigadorDTO;

      default:
        return {
          id: this.catalogId || 0,
          codigo: this.catalogForm.codigo.trim(),
          nombre: this.catalogForm.nombre.trim(),
          descripcion: this.catalogForm.descripcion.trim(),
          activo: this.catalogForm.activo
        } as any;
    }
  }

  getCatalogTypeName(): string {
    switch (this.currentCatalogType) {
      case CatalogType.PARTICIPATION_TYPES:
        return 'Participation Type';
      case CatalogType.PRODUCT_TYPES:
        return 'Product Type';
      case CatalogType.RESEARCHER_TYPES:
        return 'Researcher Type';
      case CatalogType.JOURNALS:
        return 'Journal';
      default:
        return 'Catalog Item';
    }
  }

  cancelEdit(): void {
    this.goBack();
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

  private loadProductTypes(): void {
    console.log('🔄 Loading product types...');
    this.genericCatalogService.getItems(CatalogType.PRODUCT_TYPES).subscribe({
      next: (productTypes) => {
        console.log('📦 Raw product types from service:', productTypes);
        this.productTypes = productTypes.map(pt => ({
          id: pt.id,
          nombre: pt.nombre || pt.descripcion
        }));
        console.log('📦 Processed product types:', this.productTypes);
      },
      error: (error) => {
        console.error('❌ Error loading product types:', error);
        this.messageService.error('Error loading product types');
      }
    });
  }

  isFormInvalid(): boolean {
    if (this.currentCatalogType === CatalogType.PARTICIPATION_TYPES) {
      return !this.catalogForm.nombre?.trim() || !this.catalogForm.idTipoProducto;
    } else {
      return !this.catalogForm.codigo?.trim() || !this.catalogForm.nombre?.trim();
    }
  }
}
