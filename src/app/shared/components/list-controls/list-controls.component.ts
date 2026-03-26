import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { SearchBarComponent } from '../search-bar/search-bar.component';
import { ViewModeSelectorComponent, ViewMode } from '../view-mode-selector/view-mode-selector.component';
import { ListStateService } from '../../../core/services/list-state.service';

@Component({
  selector: 'app-list-controls',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    SearchBarComponent,
    ViewModeSelectorComponent
  ],
  templateUrl: './list-controls.component.html',
  styleUrls: ['./list-controls.component.scss']
})
export class ListControlsComponent implements OnInit, OnChanges {
  @Input() searchPlaceholder: string = 'Search...';
  @Input() addButtonLabel: string = 'Add';
  @Input() addRoute: string = '';
  @Input() items: any[] = [];
  @Input() readonly: boolean = false;
  @Input() currentViewMode: ViewMode = 'card';
  @Input() showBasalFilter: boolean = false; // Mostrar filtro Basal Only
  @Input() showPendingFilter: boolean = false; // Mostrar filtro Pending Only
  @Input() finalFilteredCount: number | null = null; // Número final de resultados filtrados
  @Input() itemType: string = 'publication'; // Tipo de item para el mensaje
  @Input() listType: string = ''; // Tipo de lista para guardar estado (ej: 'publications', 'thesis-students')
  @Input() showExportButton: boolean = false; // Mostrar botón de export
  @Input() exportLabel: string = 'Export Excel';
  @Input() exportLoading: boolean = false; // Estado de carga para export


  @Output() searchResults = new EventEmitter<any[]>();
  @Output() searchTermChange = new EventEmitter<string>();
  @Output() viewModeChange = new EventEmitter<ViewMode>();
  @Output() basalFilterChange = new EventEmitter<boolean>();
  @Output() pendingFilterChange = new EventEmitter<boolean>();
  @Output() exportRequested = new EventEmitter<void>();

  basalOnly: boolean = false;
  pendingOnly: boolean = false;
  initialSearchTerm: string = '';

  constructor(private listStateService: ListStateService) {}

  ngOnInit(): void {
    if (this.listType) {
      const state = this.listStateService.getState(this.listType);
      this.basalOnly = state.basalOnly;
      this.pendingOnly = state.pendingOnly;
      this.initialSearchTerm = state.searchTerm;
      
      // Emitir eventos para que el componente padre aplique los filtros restaurados
      // Usar setTimeout con un delay mayor para asegurar que el componente padre haya cargado los datos
      setTimeout(() => {
        if (this.basalOnly) {
          this.basalFilterChange.emit(this.basalOnly);
        }
        if (this.pendingOnly) {
          this.pendingFilterChange.emit(this.pendingOnly);
        }
      }, 200);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['listType'] && this.listType && !changes['listType'].firstChange) {
      const state = this.listStateService.getState(this.listType);
      this.basalOnly = state.basalOnly;
      this.pendingOnly = state.pendingOnly;
      this.initialSearchTerm = state.searchTerm;
    }
  }

  onSearchResults(results: any[]): void {
    this.searchResults.emit(results);
  }

  onSearchTermChange(term: string): void {
    if (this.listType) {
      this.listStateService.saveState(this.listType, { searchTerm: term });
    }
    this.searchTermChange.emit(term);
  }

  onViewModeChange(mode: ViewMode): void {
    this.currentViewMode = mode;
    this.viewModeChange.emit(mode);
  }

  onBasalFilterChange(): void {
    if (this.listType) {
      this.listStateService.saveState(this.listType, { basalOnly: this.basalOnly });
    }
    this.basalFilterChange.emit(this.basalOnly);
  }

  onPendingFilterChange(): void {
    if (this.listType) {
      this.listStateService.saveState(this.listType, { pendingOnly: this.pendingOnly });
    }
    this.pendingFilterChange.emit(this.pendingOnly);
  }

  onExportClick(): void {
    this.exportRequested.emit();
  }
}
