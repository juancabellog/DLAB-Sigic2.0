import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  template: `
    <div class="search-bar-container">
      <mat-form-field appearance="outline" class="search-field">
        <mat-label>{{ placeholder }}</mat-label>
        <input 
          matInput 
          [(ngModel)]="searchTerm" 
          (input)="onSearchChange()"
          [placeholder]="placeholder"
          autocomplete="off">
        <mat-icon matSuffix>search</mat-icon>
        <button 
          *ngIf="searchTerm" 
          matSuffix 
          mat-icon-button 
          (click)="clearSearch()"
          matTooltip="Clear search">
          <mat-icon>close</mat-icon>
        </button>
      </mat-form-field>
      
      <div *ngIf="shouldShowCounter()" class="search-results-info">
        <div class="results-count">
          <span class="count-text">{{ getCountText() }}</span>
        </div>
        <div *ngIf="getCount() === 0" class="results-hint">
          {{ getHintText() }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .search-bar-container {
      margin-bottom: 20px;
    }

    .search-field {
      width: 100%;
      max-width: 400px;
    }

    .search-results-info {
      margin-top: 8px;
      padding: 8px 12px;
      background-color: #f5f5f5;
      border-radius: 4px;
      border-left: 4px solid #1976d2;
    }

    .results-count {
      font-size: 14px;
      color: #666;
      font-weight: 500;
    }

    .count-text {
      display: block;
    }

    .results-hint {
      margin-top: 8px;
      font-size: 12px;
      color: #999;
      font-style: italic;
    }

    @media (max-width: 768px) {
      .search-field {
        max-width: 100%;
      }
    }
  `]
})
export class SearchBarComponent implements OnInit, OnChanges {
  @Input() placeholder: string = 'Search...';
  @Input() searchFields: string[] = [];
  @Input() items: any[] = [];
  @Input() debounceTime: number = 300;
  @Input() finalFilteredCount: number | null = null; // Número final de resultados después de todos los filtros
  @Input() itemType: string = 'publication'; // Tipo de item para el mensaje (publication, thesis, etc.)
  @Input() hasBasalFilter: boolean = false; // Si hay filtro Basal Only activo
  @Input() addButtonLabel: string = 'Add'; // Texto del botón para el mensaje de ayuda
  @Input() initialSearchTerm: string = ''; // Término de búsqueda inicial (para restaurar estado)
  
  @Output() searchResults = new EventEmitter<any[]>();
  @Output() searchTermChange = new EventEmitter<string>();

  searchTerm: string = '';
  filteredCount: number | null = null;
  private searchTimeout: any;
  private hasRestoredSearchTerm: boolean = false;

  ngOnInit(): void {
    // Restaurar término de búsqueda inicial si existe
    if (this.initialSearchTerm) {
      this.searchTerm = this.initialSearchTerm;
      this.hasRestoredSearchTerm = true;
      // Si ya hay items disponibles, ejecutar la búsqueda inmediatamente
      if (this.items && this.items.length > 0) {
        setTimeout(() => {
          this.performSearch();
        }, 0);
      }
    } else {
      // Emit initial results (all items)
      if (this.items && this.items.length > 0) {
        this.searchResults.emit(this.items);
      }
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Si los items cambian (incluyendo la primera vez) y tenemos un término de búsqueda restaurado, ejecutar la búsqueda
    if (changes['items']) {
      const itemsChanged = changes['items'].currentValue && changes['items'].currentValue.length > 0;
      const previousItemsEmpty = !changes['items'].previousValue || changes['items'].previousValue.length === 0;
      
      // Si los items se cargaron (pasaron de vacío a tener datos) y hay un término restaurado, ejecutar búsqueda
      if (itemsChanged && (previousItemsEmpty || this.hasRestoredSearchTerm)) {
        if (this.hasRestoredSearchTerm && this.searchTerm) {
          setTimeout(() => {
            this.performSearch();
          }, 0);
        } else if (!this.searchTerm) {
          // Si no hay término de búsqueda, emitir todos los items
          this.searchResults.emit(this.items);
        }
      }
    }
    
    // Si el término inicial cambia, actualizar y ejecutar búsqueda
    if (changes['initialSearchTerm']) {
      if (this.initialSearchTerm) {
        this.searchTerm = this.initialSearchTerm;
        this.hasRestoredSearchTerm = true;
        if (this.items && this.items.length > 0) {
          setTimeout(() => {
            this.performSearch();
          }, 0);
        }
      }
    }
  }

  onSearchChange(): void {
    // Clear previous timeout
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    // Set new timeout for debouncing
    this.searchTimeout = setTimeout(() => {
      this.performSearch();
    }, this.debounceTime);
  }

  private performSearch(): void {
    this.searchTermChange.emit(this.searchTerm);
    
    if (!this.searchTerm.trim()) {
      this.filteredCount = null;
      this.searchResults.emit(this.items);
      return;
    }

    const filteredItems = this.filterItems(this.items, this.searchTerm.toLowerCase());
    this.filteredCount = filteredItems.length;
    this.searchResults.emit(filteredItems);
  }

  private filterItems(items: any[], searchTerm: string): any[] {
    if (!searchTerm || !items) {
      return items;
    }

    return items.filter(item => {
      // If specific search fields are provided, search only in those fields
      if (this.searchFields.length > 0) {
        return this.searchFields.some(field => {
          const value = this.getNestedProperty(item, field);
          return value && value.toString().toLowerCase().includes(searchTerm);
        });
      }

      // Otherwise, search in all string properties
      return this.searchInAllProperties(item, searchTerm);
    });
  }

  private getNestedProperty(obj: any, path: string): any {
    return path.split('.').reduce((current, prop) => {
      return current && current[prop] !== undefined ? current[prop] : null;
    }, obj);
  }

  private searchInAllProperties(obj: any, searchTerm: string): boolean {
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        const value = obj[key];
        
        if (typeof value === 'string' && value.toLowerCase().includes(searchTerm)) {
          return true;
        }
        
        if (typeof value === 'number' && value.toString().includes(searchTerm)) {
          return true;
        }
        
        if (Array.isArray(value)) {
          if (value.some(item => this.searchInAllProperties(item, searchTerm))) {
            return true;
          }
        }
        
        if (typeof value === 'object' && value !== null) {
          if (this.searchInAllProperties(value, searchTerm)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.filteredCount = null;
    this.searchResults.emit(this.items);
    this.searchTermChange.emit('');
  }

  /**
   * Determina si se debe mostrar el contador
   * Siempre mostrar si hay finalFilteredCount (viene del componente padre)
   * O si hay búsqueda activa con filteredCount
   */
  shouldShowCounter(): boolean {
    // Siempre mostrar si tenemos el contador final del componente padre
    if (this.finalFilteredCount !== null) {
      return true;
    }
    // Mostrar solo si hay búsqueda activa
    const hasSearch = this.searchTerm && this.searchTerm.trim().length > 0;
    return Boolean(hasSearch && this.filteredCount !== null);
  }

  /**
   * Obtiene el número de resultados actual
   */
  getCount(): number {
    return this.finalFilteredCount !== null ? this.finalFilteredCount : (this.filteredCount || 0);
  }

  /**
   * Genera el texto del contador según el contexto
   */
  getCountText(): string {
    const count = this.getCount();
    const hasSearch = this.searchTerm && this.searchTerm.trim().length > 0;
    const hasBasal = this.hasBasalFilter;
    
    if (hasSearch) {
      // Caso 2 o 3: Con búsqueda
      const resultText = count === 1 ? 'result' : 'results';
      let text = `${count} ${resultText} found for "${this.searchTerm.trim()}"`;
      if (hasBasal) {
        text += ' (Basal only)';
      }
      return text;
    } else {
      // Caso 1 o 4: Sin búsqueda
      const itemTypeText = this.itemType + (count !== 1 ? 's' : '');
      let text = `${count} ${itemTypeText} found`;
      if (hasBasal) {
        text += ' (Basal only)';
      }
      return text;
    }
  }

  /**
   * Genera el texto de ayuda cuando hay 0 resultados
   */
  getHintText(): string {
    const hasSearch = this.searchTerm && this.searchTerm.trim().length > 0;
    
    if (hasSearch) {
      return 'Try adjusting your search term or filters.';
    } else {
      return `No ${this.itemType}s yet. Use "${this.addButtonLabel}" to create one.`;
    }
  }
}




