import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, of, debounceTime, distinctUntilChanged, switchMap, Subject } from 'rxjs';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { InstitucionDTO } from '../../../core/models/backend-dtos';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-institution-search',
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
  templateUrl: './institution-search.component.html',
  styleUrls: ['./institution-search.component.scss']
})
export class InstitutionSearchComponent implements OnInit, OnChanges {
  @Input() disabled = false;
  @Input() selectedInstitution: InstitucionDTO | null = null;
  @Input() label = 'Search Institutions';
  @Input() placeholder = 'Search by name...';
  @Output() institutionSelected = new EventEmitter<InstitucionDTO>();
  @ViewChild('searchInput') searchInput!: ElementRef;

  searchTerm = '';
  inputValue = '';
  selectedInstitutionValue: InstitucionDTO | null = null;
  allInstitutions: InstitucionDTO[] = [];
  filteredInstitutions$: Observable<InstitucionDTO[]> = of([]);
  private searchSubject = new Subject<string>();
  loading = false;
  
  // Variable to hold the selected institution for the autocomplete
  selectedInstitutionForAutocomplete: InstitucionDTO | null = null;

  constructor(
    private baseHttp: BaseHttpService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Load all institutions
    this.loadInstitutions();
    
    // If there's a selected institution, show it in the field
    if (this.selectedInstitution) {
      this.inputValue = this.getInstitutionDisplayName(this.selectedInstitution);
      this.searchTerm = this.getInstitutionDisplayName(this.selectedInstitution);
    }
    
    // Configure search stream with debounce
    this.filteredInstitutions$ = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(searchTerm => {
        if (searchTerm && searchTerm.length >= 2) {
          // Filter with 2 or more characters
          return of(this.filterInstitutions(searchTerm));
        } else if (searchTerm === '') {
          // When field is empty, show all institutions (limited to first 50 for performance)
          return of(this.allInstitutions.slice(0, 50));
        } else {
          // When less than 2 characters, show no results
          return of([]);
        }
      })
    );
  }

  ngOnChanges(changes: any): void {
    // Update inputValue when selectedInstitution changes from outside
    if (changes.selectedInstitution) {
      const institution = changes.selectedInstitution.currentValue;
      if (institution) {
        const displayName = this.getInstitutionDisplayName(institution);
        this.inputValue = displayName;
        this.searchTerm = displayName;
        // Update the input directly
        if (this.searchInput) {
          this.searchInput.nativeElement.value = displayName;
        }
        // Force change detection
        this.cdr.detectChanges();
      } else {
        this.inputValue = '';
        this.searchTerm = '';
        if (this.searchInput) {
          this.searchInput.nativeElement.value = '';
        }
      }
    }
  }

  loadInstitutions(): void {
    this.loading = true;
    this.baseHttp.get<InstitucionDTO[]>('/catalogs/institutions').pipe(
      catchError(() => of([]))
    ).subscribe(institutions => {
      this.allInstitutions = institutions;
      this.loading = false;
    });
  }

  onSearch(event: any): void {
    const value = event.target.value;
    this.inputValue = value;
    this.searchTerm = value;
    // Clear selected institution when user types
    if (this.selectedInstitution && value !== this.getInstitutionDisplayName(this.selectedInstitution)) {
      this.selectedInstitution = null;
      this.selectedInstitutionValue = null;
      this.selectedInstitutionForAutocomplete = null;
    }
    // Normalize text for more friendly search
    const normalizedValue = this.normalizeText(value);
    this.searchSubject.next(normalizedValue);
  }

  private normalizeText(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // Remove accents
      .trim();
  }

  private filterInstitutions(searchTerm: string): InstitucionDTO[] {
    if (!searchTerm || searchTerm.length < 2) {
      return [];
    }
    
    const normalizedSearch = this.normalizeText(searchTerm);
    return this.allInstitutions.filter(inst => {
      const descripcion = this.normalizeText(inst.descripcion || '');
      const idDescripcion = this.normalizeText(inst.idDescripcion || '');
      return descripcion.includes(normalizedSearch) || idDescripcion.includes(normalizedSearch);
    }).slice(0, 50); // Limit to 50 results for performance
  }

  onFocus(): void {
    // When user focuses on the field, show all institutions if empty
    if (this.inputValue.length === 0) {
      this.searchSubject.next('');
    }
  }

  onInstitutionSelected(institution: InstitucionDTO): void {
    if (!institution) {
      return;
    }
    
    this.selectedInstitution = institution;
    this.selectedInstitutionValue = institution;
    this.selectedInstitutionForAutocomplete = institution;
    const displayName = this.getInstitutionDisplayName(institution);
    
    // Set the input value - this will trigger the autocomplete to close
    this.inputValue = displayName;
    this.searchTerm = displayName;
    
    // Update the input directly to ensure the value is displayed
    if (this.searchInput) {
      this.searchInput.nativeElement.value = displayName;
    }
    
    // Use setTimeout to ensure the value is set after the autocomplete closes
    setTimeout(() => {
      // Force change detection to ensure the value is displayed
      this.cdr.detectChanges();
    }, 0);
    
    // Emit the selected institution
    this.institutionSelected.emit(institution);
  }

  displayWith = (institution: InstitucionDTO | string | null): string => {
    // Handle case where displayWith receives a string (from input value)
    if (typeof institution === 'string') {
      // If it's a string, return it as is (it's already the display value)
      return institution;
    }
    // Handle case where displayWith receives the object
    if (institution && typeof institution === 'object' && 'descripcion' in institution) {
      return institution.descripcion || institution.idDescripcion || '';
    }
    return '';
  }

  private getInstitutionDisplayName(institution: InstitucionDTO): string {
    return institution.descripcion || institution.idDescripcion || '';
  }
}
