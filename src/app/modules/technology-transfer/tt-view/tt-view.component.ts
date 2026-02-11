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
import { of, firstValueFrom } from 'rxjs';

import { MessageService } from '../../../core/services/message.service';
import { TechnologyTransferService } from '../../../core/services/technology-transfer.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { UtilsService } from '../../../core/services/utils.service';
import { TransferenciaTecnologicaDTO, CategoriaTransferenciaDTO, RRHHDTO } from '../../../core/models/backend-dtos';

@Component({
  selector: 'app-tt-view',
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
  templateUrl: './tt-view.component.html',
  styleUrls: ['./tt-view.component.scss']
})
export class TtViewComponent implements OnInit {
  transfer: TransferenciaTecnologicaDTO | null = null;
  loading: boolean = false;
  participants: Array<{ name: string; role: string; corresponding: boolean }> = [];
  transferCategories: CategoriaTransferenciaDTO[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private technologyTransferService: TechnologyTransferService,
    private researcherService: ResearcherService,
    private baseHttp: BaseHttpService,
    private utilsService: UtilsService
  ) {}

  ngOnInit(): void {
    this.loadTransferCategories();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTransfer(parseInt(id));
    }
  }

  loadTransferCategories(): void {
    this.baseHttp.get<CategoriaTransferenciaDTO[]>('/catalogs/transfer-categories').pipe(
      catchError(error => {
        console.error('Error loading transfer categories:', error);
        return of([]);
      })
    ).subscribe(items => {
      this.transferCategories = items;
    });
  }

  loadTransfer(id: number): void {
    this.loading = true;
    this.technologyTransferService.getTechnologyTransfer(id).pipe(
      catchError(error => {
        console.error('Error loading technology transfer:', error);
        this.messageService.error('Error loading technology transfer. Please try again later.');
        this.goBack();
        return of(null);
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe(async transfer => {
      this.transfer = transfer;
      
      if (!transfer) {
        this.messageService.error('Technology transfer not found');
        this.goBack();
        return;
      }

      // Cargar participantes
      if (transfer.participantes && transfer.participantes.length > 0) {
        await this.loadParticipants(transfer.participantes);
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
            this.participants.push({
              name: researcher.fullname || 'Unknown',
              role: 'Participant',
              corresponding: p.corresponding || false
            });
          }
        } catch (error) {
          console.error('Error loading researcher:', error);
        }
      }
    }
  }

  getTransferTitle(): string {
    return this.transfer?.descripcion || 'Untitled Transfer';
  }

  getLocation(): string {
    const parts = [];
    if (this.transfer?.ciudad) parts.push(this.transfer.ciudad);
    if (this.transfer?.region) parts.push(this.transfer.region);
    if (this.transfer?.pais?.idDescripcion) parts.push(this.transfer.pais.idDescripcion);
    return parts.length > 0 ? parts.join(', ') : 'Location not specified';
  }

  getCategories(): string[] {
    if (!this.transfer?.categoriaTransferencia) return [];
    try {
      const categoryIds = this.transfer.categoriaTransferencia.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
      return categoryIds.map(id => {
        const category = this.transferCategories.find(c => c.id === id);
        return category?.idDescripcion || `Category ${id}`;
      });
    } catch (e) {
      return [];
    }
  }

  goBack(): void {
    this.router.navigate(['/technology-transfer']);
  }

  editTransfer(): void {
    if (this.transfer?.id) {
      this.router.navigate(['/technology-transfer', this.transfer.id, 'edit']);
    }
  }

  deleteTransfer(): void {
    if (!this.transfer?.id) return;
    
    const title = this.getTransferTitle();
    this.messageService.confirm(
      `Are you sure you want to delete "${title}"?`,
      (accepted: boolean) => {
        if (accepted) {
          this.technologyTransferService.deleteTechnologyTransfer(this.transfer!.id!).subscribe({
            next: () => {
              this.messageService.success('Technology transfer deleted successfully');
              this.goBack();
            },
            error: (error) => {
              console.error('Error deleting technology transfer:', error);
              this.messageService.error('Error deleting technology transfer. Please try again.');
            }
          });
        }
      },
      'Delete Technology Transfer'
    );
  }

  getBasalStatus(): string {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.transfer?.basal;
    return (basal === 'S' || basal === 's' || basal === '1') ? 'Yes' : 'No';
  }

  isBasal(): boolean {
    // Basal puede ser "S", "s", "1" (todos significan true) o "N", "n", "0" (todos significan false)
    const basal = this.transfer?.basal;
    return basal === 'S' || basal === 's' || basal === '1';
  }

  getPdfUrl(): string | null {
    return this.utilsService.getPdfUrl(this.transfer?.linkPDF);
  }

  getPdfFileName(): string {
    if (!this.transfer?.linkPDF) {
      return '';
    }
    // Extraer el nombre del archivo del linkPDF (formato: "PDF:pdfs/nombre_archivo.pdf")
    if (this.transfer.linkPDF.startsWith('PDF:')) {
      const path = this.transfer.linkPDF.substring(4);
      const parts = path.split('/');
      return parts[parts.length - 1];
    }
    // Si es una URL completa, extraer el nombre del archivo
    const parts = this.transfer.linkPDF.split('/');
    return parts[parts.length - 1];
  }
}
