import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, finalize } from 'rxjs/operators';
import { of, firstValueFrom, forkJoin } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { ThesisService } from '../../../core/services/thesis.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { TesisDTO, RRHHDTO, TipoSectorDTO, ResourceDTO } from '../../../core/models/backend-dtos';
import { TipoParticipacionDTO } from '../../../core/models/catalog-types';

@Component({
  selector: 'app-thesis-student-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './thesis-student-view.component.html',
  styleUrls: ['./thesis-student-view.component.scss']
})
export class ThesisStudentViewComponent implements OnInit {
  private readonly RESOURCE_OTHER_ID = 4;

  thesis: TesisDTO | null = null;
  loading: boolean = false;
  participants: Array<{ name: string; role: string; roleName: string; corresponding: boolean }> = [];
  sectorTypes: TipoSectorDTO[] = [];
  resourceCatalog: ResourceDTO[] = [];
  participationTypes: TipoParticipacionDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private thesisService: ThesisService,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService,
    private catalogService: CatalogService
  ) {}

  ngOnInit(): void {
    this.loadSectorTypes();
    this.loadResources();
    this.loadParticipationTypes();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadThesis(parseInt(id));
    }
  }

  loadSectorTypes(): void {
    this.baseHttp.get<TipoSectorDTO[]>('/catalogs/sector-types').pipe(
      catchError(error => {
        console.error('Error loading sector types:', error);
        return of([]);
      })
    ).subscribe(items => {
      this.sectorTypes = items;
    });
  }

  loadResources(): void {
    this.baseHttp.get<ResourceDTO[]>('/catalogs/resources').pipe(
      catchError(error => {
        console.error('Error loading resources:', error);
        return of([]);
      })
    ).subscribe(items => {
      this.resourceCatalog = items || [];
    });
  }

  loadParticipationTypes(): void {
    this.catalogService.getParticipationTypes('thesis-students').pipe(
      catchError(error => {
        console.error('Error loading participation types:', error);
        return of([]);
      })
    ).subscribe(types => {
      this.participationTypes = types;
    });
  }

  loadThesis(id: number): void {
    this.loading = true;
    
    // Esperar a que se carguen los tipos de participación y el thesis en paralelo
    forkJoin({
      thesis: this.thesisService.getThesisById(id).pipe(
        catchError(error => {
          console.error('Error loading thesis:', error);
          this.messageService.error('Error loading thesis. Please try again later.');
          this.goBack();
          return of(null);
        })
      ),
      participationTypes: this.participationTypes.length > 0 
        ? of(this.participationTypes)
        : this.catalogService.getParticipationTypes('thesis-students').pipe(
            catchError(error => {
              console.error('Error loading participation types:', error);
              return of([]);
            })
          )
    }).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(async ({ thesis, participationTypes }) => {
      // Actualizar tipos de participación si se cargaron
      if (participationTypes.length > 0) {
        this.participationTypes = participationTypes;
      }
      
      this.thesis = thesis;
      
      if (!thesis) {
        this.messageService.error('Thesis not found');
        this.goBack();
        return;
      }

      // Cargar participantes (ahora los tipos ya están disponibles)
      if (thesis.participantes && thesis.participantes.length > 0) {
        await this.loadParticipants(thesis.participantes);
      }
    });
  }

  async loadParticipants(participantes: any[]): Promise<void> {
    this.participants = [];
    
    for (const p of participantes) {
      if (p.rrhhId) {
        try {
          const researcher: RRHHDTO = await firstValueFrom(this.researcherService.getResearcher(p.rrhhId));
          if (researcher.id) {
            // Buscar el nombre del tipo de participación
            const participationType = this.participationTypes.find(t => t.id === p.tipoParticipacionId);
            const roleName = participationType 
              ? (participationType.descripcion || participationType.nombre || 'Unknown')
              : 'Unknown';
            
            this.participants.push({
              name: researcher.fullname || 'Unknown',
              role: 'Participant',
              roleName: roleName,
              corresponding: p.corresponding || false
            });
          }
        } catch (error) {
          console.error('Error loading researcher:', error);
        }
      }
    }
  }

  getThesisTitle(): string {
    return this.thesis?.nombreCompletoTitulo || this.thesis?.descripcion || 'Untitled Thesis';
  }

  getStatusColor(status: string | undefined): 'primary' | 'accent' | 'warn' {
    if (!status) return 'primary';
    switch (status.toUpperCase()) {
      case 'FINISHED': return 'accent';
      case 'IN PROGRESS': return 'primary';
      default: return 'primary';
    }
  }

  getSectorTypes(): string[] {
    if (!this.thesis?.tipoSector) return [];
    try {
      const sectorIds = this.thesis.tipoSector.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
      // Mapear los IDs a las descripciones reales desde el catálogo
      return sectorIds.map(id => {
        const sectorType = this.sectorTypes.find(st => st.id === id);
        return sectorType?.idDescripcion || `Sector ${id}`;
      });
    } catch (e) {
      return [];
    }
  }

  getResources(): string[] {
    if (!this.thesis?.resources) return [];
    const raw = this.thesis.resources.trim();
    let parsedAny: any[] = [];
    if (raw.startsWith('[')) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          parsedAny = parsed;
        }
      } catch {
        /* fall through */
      }
    }
    if (!parsedAny.length) {
      const ids = raw
        .split(',')
        .map(s => parseInt(s.trim(), 10))
        .filter(n => !isNaN(n));
      parsedAny = ids.map(id => ({ id }));
    }
    const labels: string[] = [];
    for (const item of parsedAny) {
      const id = Number(item?.id);
      if (isNaN(id)) continue;
      if (id === this.RESOURCE_OTHER_ID) {
        const cat = this.resourceCatalog.find(r => r.id === id);
        const base = cat?.idDescripcion || 'Other';
        const text = item?.text != null ? String(item.text).trim() : '';
        labels.push(text ? `${base}: ${text}` : base);
      } else {
        const resource = this.resourceCatalog.find(r => r.id === id);
        labels.push(resource?.idDescripcion || `Resource ${id}`);
      }
    }
    return labels;
  }

  goBack(): void {
    this.router.navigate(['/thesis-students']);
  }

  editThesis(): void {
    if (this.thesis?.id) {
      this.router.navigate(['/thesis-students', this.thesis.id, 'edit']);
    }
  }

  deleteThesis(): void {
    if (!this.thesis?.id) return;
    
    const title = this.getThesisTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.thesisService.deleteThesis(this.thesis!.id!).subscribe({
            next: () => {
              this.messageService.success('Thesis deleted successfully');
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting thesis:', error);
              this.messageService.error('Error deleting thesis. Please try again.');
            }
          });
        }
      }
    );
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.thesis?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.thesis?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.thesis?.linkPDF);
  }

  getPdfFileName(): string {
    if (!this.thesis?.linkPDF) {
      return '';
    }
    // Extraer el nombre del archivo del linkPDF (formato: "PDF:pdfs/nombre_archivo.pdf")
    if (this.thesis.linkPDF.startsWith('PDF:')) {
      const path = this.thesis.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    // Si es una URL completa, extraer el nombre del archivo
    const parts = this.thesis.linkPDF.split('/');
    return parts[parts.length - 1];
  }
}
