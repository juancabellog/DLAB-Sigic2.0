import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, debounceTime, distinctUntilChanged, switchMap, catchError, Subject } from 'rxjs';
import { ResearcherService } from '../../../core/services/researcher.service';
import { RRHHDTO } from '../../../core/models/backend-dtos';
import { CreateResearcherDialogComponent } from '../create-researcher-dialog/create-researcher-dialog.component';

@Component({
  selector: 'app-researcher-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './researcher-search.component.html',
  styleUrls: ['./researcher-search.component.scss']
})
export class ResearcherSearchComponent implements OnInit, OnChanges {
  @Input() disabled = false;
  @Input() clearOnSelect = true; // Por defecto limpia después de seleccionar (comportamiento para participantes)
  @Input() selectedResearcher: RRHHDTO | null = null; // Para mostrar un investigador ya seleccionado
  @Output() researcherSelected = new EventEmitter<RRHHDTO>();
  @ViewChild('searchInput') searchInput!: ElementRef;

  searchTerm = '';
  inputValue = ''; // Valor mostrado en el input
  filteredResearchers$: Observable<RRHHDTO[]> = of([]);
  private searchSubject = new Subject<string>();

  constructor(
    private researcherService: ResearcherService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    // Si hay un investigador seleccionado, mostrarlo en el campo
    if (this.selectedResearcher) {
      this.inputValue = this.selectedResearcher.fullname || '';
      this.searchTerm = this.selectedResearcher.fullname || '';
    }
    
    // Configurar el stream de búsqueda con debounce
    this.filteredResearchers$ = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(searchTerm => {
        if (searchTerm && searchTerm.length >= 2) {
          // Buscar con 2 o más caracteres
          return this.researcherService.searchResearchers(searchTerm).pipe(
            catchError(() => of([]))
          );
        } else if (searchTerm === '') {
          // Cuando el campo está vacío, cargar todos los investigadores
          return this.researcherService.getResearchers().pipe(
            switchMap((response: any) => of(response.content || [])),
            catchError(() => of([]))
          );
        } else {
          // Cuando hay menos de 2 caracteres, no mostrar resultados
          return of([]);
        }
      })
    );
  }

  ngOnChanges(): void {
    // Actualizar el inputValue cuando selectedResearcher cambia desde fuera
    if (this.selectedResearcher) {
      this.inputValue = this.selectedResearcher.fullname || '';
      this.searchTerm = this.selectedResearcher.fullname || '';
      // Actualizar el input directamente
      if (this.searchInput) {
        this.searchInput.nativeElement.value = this.inputValue;
      }
    } else if (!this.selectedResearcher && this.clearOnSelect) {
      this.inputValue = '';
      this.searchTerm = '';
    }
  }

  onSearch(event: any): void {
    const value = event.target.value;
    this.inputValue = value;
    this.searchTerm = value;
    // Normalizar el texto para búsqueda más amigable
    const normalizedValue = this.normalizeText(value);
    this.searchSubject.next(normalizedValue);
  }

  private normalizeText(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // Quitar acentos
      .trim();
  }

  onFocus(): void {
    // Cuando el usuario hace focus en el campo, mostrar todos los investigadores si está vacío
    if (!this.inputValue || this.inputValue.length === 0) {
      this.searchSubject.next('');
    }
  }

  onResearcherSelected(researcher: RRHHDTO | null | undefined): void {
    if (!researcher) {
      console.warn('onResearcherSelected called with null/undefined researcher');
      return;
    }
    
    this.selectedResearcher = researcher;
    const displayName = researcher?.fullname || '';
    this.inputValue = displayName;
    this.searchTerm = displayName;
    
    // Actualizar el input directamente para asegurar que se muestre el valor
    if (this.searchInput) {
      this.searchInput.nativeElement.value = displayName;
    }
    
    // Forzar detección de cambios para asegurar que el valor se muestre
    this.cdr.detectChanges();
    
    this.researcherSelected.emit(researcher);
    
    // Solo limpiar si clearOnSelect es true (comportamiento por defecto para participantes)
    if (this.clearOnSelect) {
      // Usar setTimeout para que el valor se muestre antes de limpiar
      setTimeout(() => {
        this.clearSearch();
      }, 100);
    }
  }

  private clearSearch(): void {
    this.inputValue = '';
    this.searchTerm = '';
    this.selectedResearcher = null;
    this.searchSubject.next('');
    // Forzar la actualización del input
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
      this.searchInput.nativeElement.focus();
    }
  }

  displayWith(researcher: RRHHDTO): string {
    return researcher ? researcher.fullname || '' : '';
  }

  openCreateResearcherDialog(): void {
    const dialogRef = this.dialog.open(CreateResearcherDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((createdResearcher: RRHHDTO | null) => {
      if (createdResearcher) {
        // Mark as newly created for highlighting
        (createdResearcher as any)._newlyCreated = true;
        
        // If fullname is not available, reload the researcher from the backend
        if (!createdResearcher.fullname && createdResearcher.id) {
          this.researcherService.getResearcher(createdResearcher.id).subscribe({
            next: (reloadedResearcher: RRHHDTO) => {
              if (reloadedResearcher) {
                // Mark as newly created
                (reloadedResearcher as any)._newlyCreated = true;
                this.onResearcherSelected(reloadedResearcher);
              } else {
                // Fallback: use the created researcher even without fullname
                this.onResearcherSelected(createdResearcher);
              }
            },
            error: (error: any) => {
              console.error('Error reloading researcher:', error);
              // Fallback: use the created researcher even without fullname
              this.onResearcherSelected(createdResearcher);
            }
          });
        } else {
          // fullname is available, proceed normally
          this.onResearcherSelected(createdResearcher);
        }
      }
    });
  }
}