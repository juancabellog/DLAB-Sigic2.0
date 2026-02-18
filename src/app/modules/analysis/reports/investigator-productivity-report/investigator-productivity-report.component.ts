import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MessageService } from '../../../../core/services/message.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-investigator-productivity-report',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule
  ],
  templateUrl: './investigator-productivity-report.component.html',
  styleUrls: ['./investigator-productivity-report.component.scss']
})
export class InvestigatorProductivityReportComponent implements OnInit {
  loading = false;
  generating = false;
  selectedFile: File | null = null;
  fileUploadDate: Date | null = null;
  isDragOver = false;

  constructor(
    private http: HttpClient,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    // Component initialization
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processFile(input.files[0]);
    }
  }

  onFileDropped(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.processFile(event.dataTransfer.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  private processFile(file: File): void {
    if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls') && !file.name.endsWith('.csv')) {
      this.messageService.error('Por favor seleccione un archivo Excel (.xlsx, .xls) o CSV (.csv)');
      return;
    }
    this.selectedFile = file;
    this.fileUploadDate = new Date();
    this.messageService.success('Archivo cargado: ' + this.selectedFile.name);
  }

  replaceFile(): void {
    this.selectedFile = null;
    this.fileUploadDate = null;
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  generateReport(): void {
    if (!this.selectedFile) {
      this.messageService.error('Por favor seleccione el archivo de proyectos (Project Funding by Investigator)');
      return;
    }

    this.generating = true;
    const formData = new FormData();
    formData.append('projectsFile', this.selectedFile);

    const apiUrl = environment.apiUrl || 'http://localhost:8081/sigic2.0/api';
    const url = `${apiUrl}/analysis/generate-investigator-productivity-report`;

    this.http.post(url, formData, {
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (response) => {
        this.generating = false;
        
        // Obtener el nombre del archivo del header Content-Disposition o usar uno por defecto
        const contentDisposition = response.headers.get('Content-Disposition');
        let filename = 'investigator-productivity-report.xlsx';
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].replace(/['"]/g, '');
          }
        }

        // Crear un blob y descargar el archivo
        const blob = new Blob([response.body!], { 
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
        });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        this.messageService.success('Reporte generado exitosamente');
      },
      error: (error) => {
        this.generating = false;
        console.error('Error generando reporte:', error);
        let errorMessage = 'Error al generar el reporte';
        if (error.error instanceof Blob) {
          // Si el error es un blob, intentar leerlo como texto JSON
          error.error.text().then((text: string) => {
            try {
              const errorJson = JSON.parse(text);
              errorMessage = errorJson.error || errorMessage;
              this.messageService.error(errorMessage);
            } catch {
              this.messageService.error(errorMessage);
            }
          });
        } else if (error.error && error.error.error) {
          errorMessage = error.error.error;
          this.messageService.error(errorMessage);
        } else {
          this.messageService.error(errorMessage);
        }
      }
    });
  }
}

