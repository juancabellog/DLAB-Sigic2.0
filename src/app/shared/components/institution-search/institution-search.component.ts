import { Component, Input, Output, EventEmitter, OnInit, OnChanges, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, of, debounceTime, distinctUntilChanged, switchMap, Subject } from 'rxjs';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { InstitucionDTO } from '../../../core/models/backend-dtos';
import { catchError } from 'rxjs/operators';
import { CreateInstitutionDialogComponent } from '../create-institution-dialog/create-institution-dialog.component';

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
    MatProgressSpinnerModule,
    MatDialogModule
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
  selectedInstitutionForAutocomplete: InstitucionDTO | null = null;

  constructor(
    private baseHttp: BaseHttpService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadInstitutions();

    if (this.selectedInstitution) {
      this.inputValue = this.getInstitutionDisplayName(this.selectedInstitution);
      this.searchTerm = this.getInstitutionDisplayName(this.selectedInstitution);
    }

    this.filteredInstitutions$ = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(searchTerm => {
        if (searchTerm && searchTerm.length >= 2) {
          return of(this.filterInstitutions(searchTerm));
        } else if (searchTerm === '') {
          return of(this.allInstitutions.slice(0, 50));
        } else {
          return of([]);
        }
      })
    );
  }

  ngOnChanges(changes: any): void {
    if (changes.selectedInstitution) {
      const institution = changes.selectedInstitution.currentValue;
      if (institution) {
        const displayName = this.getInstitutionDisplayName(institution);
        this.inputValue = displayName;
        this.searchTerm = displayName;
        if (this.searchInput) {
          this.searchInput.nativeElement.value = displayName;
        }
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

  loadInstitutions(selectAfterLoad?: InstitucionDTO): void {
    this.loading = true;
    this.baseHttp.get<InstitucionDTO[]>('/catalogs/institutions').pipe(
      catchError(() => of([]))
    ).subscribe(institutions => {
      this.allInstitutions = institutions || [];
      this.loading = false;
      if (selectAfterLoad) {
        const resolved = this.allInstitutions.find(i => i.id === selectAfterLoad.id) || selectAfterLoad;
        this.onInstitutionSelected(resolved);
      } else if (this.inputValue) {
        // refresh current filter results
        this.searchSubject.next(this.normalizeText(this.inputValue));
      }
    });
  }

  onSearch(event: any): void {
    const value = event.target.value;
    this.inputValue = value;
    this.searchTerm = value;
    if (this.selectedInstitution && value !== this.getInstitutionDisplayName(this.selectedInstitution)) {
      this.selectedInstitution = null;
      this.selectedInstitutionValue = null;
      this.selectedInstitutionForAutocomplete = null;
    }
    const normalizedValue = this.normalizeText(value);
    this.searchSubject.next(normalizedValue);
  }

  private normalizeText(text: string): string {
    return (text || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
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
      const country = this.normalizeText(this.getCountryLabel(inst));
      return descripcion.includes(normalizedSearch)
        || idDescripcion.includes(normalizedSearch)
        || country.includes(normalizedSearch);
    }).slice(0, 50);
  }

  onFocus(): void {
    if (this.inputValue.length === 0) {
      this.searchSubject.next('');
    }
  }

  onInstitutionSelected(institution: InstitucionDTO | null | undefined): void {
    if (!institution) {
      return;
    }

    this.selectedInstitution = institution;
    this.selectedInstitutionValue = institution;
    this.selectedInstitutionForAutocomplete = institution;
    const displayName = this.getInstitutionDisplayName(institution);

    this.inputValue = displayName;
    this.searchTerm = displayName;

    if (this.searchInput) {
      this.searchInput.nativeElement.value = displayName;
    }

    setTimeout(() => {
      this.cdr.detectChanges();
    }, 0);

    this.institutionSelected.emit(institution);
  }

  openCreateInstitutionDialog(): void {
    const dialogRef = this.dialog.open(CreateInstitutionDialogComponent, {
      width: '560px',
      maxWidth: '90vw',
      disableClose: true,
      data: {
        initialName: this.inputValue?.trim() || '',
        existingInstitutions: this.allInstitutions
      }
    });

    dialogRef.afterClosed().subscribe((created: InstitucionDTO | null) => {
      if (created) {
        // Reload catalog so future searches include the new institution, then select it
        this.loadInstitutions(created);
      }
    });
  }

  displayWith = (institution: InstitucionDTO | string | null): string => {
    if (typeof institution === 'string') {
      return institution;
    }
    if (institution && typeof institution === 'object') {
      return this.getInstitutionDisplayName(institution);
    }
    return '';
  };

  getOptionLabel(institution: InstitucionDTO): string {
    const name = this.getInstitutionDisplayName(institution);
    const country = this.getCountryLabel(institution);
    return country ? `${name} — ${country}` : name;
  }

  private getInstitutionDisplayName(institution: InstitucionDTO): string {
    return institution.descripcion || institution.idDescripcion || '';
  }

  private getCountryLabel(institution: InstitucionDTO): string {
    return (institution.countryLabel || institution.codigoPais || '').trim();
  }
}
