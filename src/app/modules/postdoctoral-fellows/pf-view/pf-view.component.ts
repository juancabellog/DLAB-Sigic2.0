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
import { PostdoctoralFellowsService } from '../../../core/services/postdoctoral-fellows.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { BecariosPostdoctoralesDTO, RRHHDTO, ResourceDTO, FundingTypeDTO } from '../../../core/models/backend-dtos';
import { TipoParticipacionDTO } from '../../../core/models/catalog-types';

@Component({
  selector: 'app-pf-view',
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
  templateUrl: './pf-view.component.html',
  styleUrls: ['./pf-view.component.scss']
})
export class PfViewComponent implements OnInit {
  fellow: BecariosPostdoctoralesDTO | null = null;
  loading: boolean = false;
  participants: Array<{ name: string; role: string; roleName: string; corresponding: boolean }> = [];
  resources: ResourceDTO[] = [];
  fundingTypes: FundingTypeDTO[] = [];
  participationTypes: TipoParticipacionDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private postdoctoralFellowsService: PostdoctoralFellowsService,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService,
    private catalogService: CatalogService
  ) {}

  ngOnInit(): void {
    this.loadResources();
    this.loadFundingTypes();
    this.loadParticipationTypes();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadFellow(parseInt(id));
    }
  }

  loadResources(): void {
    this.baseHttp.get<ResourceDTO[]>('/catalogs/resources').pipe(
      catchError(error => {
        console.error('Error loading resources:', error);
        return of([]);
      })
    ).subscribe(items => {
      this.resources = items;
    });
  }

  loadFundingTypes(): void {
    this.baseHttp.get<FundingTypeDTO[]>('/catalogs/funding-types').pipe(
      catchError(error => {
        console.error('Error loading funding types:', error);
        return of([]);
      })
    ).subscribe(items => {
      this.fundingTypes = items;
    });
  }

  loadParticipationTypes(): void {
    this.catalogService.getParticipationTypes('postdoctoral-fellows').pipe(
      catchError(error => {
        console.error('Error loading participation types:', error);
        return of([]);
      })
    ).subscribe(types => {
      this.participationTypes = types;
    });
  }

  loadFellow(id: number): void {
    this.loading = true;
    
    // Esperar a que se carguen los tipos de participación y el fellow en paralelo
    forkJoin({
      fellow: this.postdoctoralFellowsService.getPostdoctoralFellow(id).pipe(
        catchError(error => {
          console.error('Error loading postdoctoral fellow:', error);
          this.messageService.error('Error loading postdoctoral fellow. Please try again later.');
          this.goBack();
          return of(null);
        })
      ),
      participationTypes: this.participationTypes.length > 0 
        ? of(this.participationTypes)
        : this.catalogService.getParticipationTypes('postdoctoral-fellows').pipe(
            catchError(error => {
              console.error('Error loading participation types:', error);
              return of([]);
            })
          )
    }).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(async ({ fellow, participationTypes }) => {
      // Actualizar tipos de participación si se cargaron
      if (participationTypes.length > 0) {
        this.participationTypes = participationTypes;
      }
      
      this.fellow = fellow;
      
      if (!fellow) {
        this.messageService.error('Postdoctoral fellow not found');
        this.goBack();
        return;
      }

      // Cargar participantes (ahora los tipos ya están disponibles)
      if (fellow.participantes && fellow.participantes.length > 0) {
        await this.loadParticipants(fellow.participantes);
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

  getFellowTitle(): string {
    return this.fellow?.descripcion || 'Untitled Postdoctoral Fellow';
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.fellow?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.fellow?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getResources(): string[] {
    if (!this.fellow?.resources) return [];
    try {
      const resourceIds = this.fellow.resources.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
      return resourceIds.map(id => {
        const resource = this.resources.find(r => r.id === id);
        return resource?.idDescripcion || `Resource ${id}`;
      });
    } catch (e) {
      return [];
    }
  }

  getFundingSources(): string[] {
    if (!this.fellow?.fundingSource) return [];
    try {
      const fundingIds = this.fellow.fundingSource.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
      return fundingIds.map(id => {
        const fundingType = this.fundingTypes.find(ft => ft.id === id);
        return fundingType?.idDescripcion || `Funding ${id}`;
      });
    } catch (e) {
      return [];
    }
  }

  goBack(): void {
    this.router.navigate(['/postdoctoral-fellows']);
  }

  editFellow(): void {
    if (this.fellow?.id) {
      this.router.navigate(['/postdoctoral-fellows', this.fellow.id, 'edit']);
    }
  }

  deleteFellow(): void {
    if (!this.fellow?.id) return;
    
    const title = this.getFellowTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.postdoctoralFellowsService.deletePostdoctoralFellow(this.fellow!.id!).subscribe({
            next: () => {
              this.messageService.success('Postdoctoral Fellow Deleted', `${title} has been successfully removed.`);
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting postdoctoral fellow:', error);
              this.messageService.error('Error deleting postdoctoral fellow. Please try again.');
            }
          });
        }
      }
    );
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.fellow?.linkPDF);
  }
}


