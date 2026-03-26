import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../../core/services/auth.service';
import { MessageService } from '../../../core/services/message.service';
import { AnidExportService } from '../../../core/services/anid-export.service';

type ExportActionState = 'idle' | 'loading' | 'success' | 'error' | 'empty';

@Component({
  selector: 'app-anid-export-center',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './anid-export-center.component.html',
  styleUrls: ['./anid-export-center.component.scss']
})
export class AnidExportCenterComponent {
  // UI states per action
  zipState: ExportActionState = 'idle';
  excelState: ExportActionState = 'idle';
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    public authService: AuthService,
    private messageService: MessageService,
    private anidExportService: AnidExportService
  ) {}

  get isAnidExportCenterVisible(): boolean {
    return this.authService.canAccessAnidExportCenter();
  }

  get isZipLoading(): boolean {
    return this.zipState === 'loading';
  }

  get isExcelLoading(): boolean {
    return this.excelState === 'loading';
  }

  get isAnyLoading(): boolean {
    return this.isZipLoading || this.isExcelLoading;
  }

  private resetMessages(): void {
    this.errorMessage = null;
    this.successMessage = null;
  }

  private downloadBlob(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  onDownloadZip(): void {
    if (!this.isAnidExportCenterVisible || this.isAnyLoading) return;

    this.resetMessages();
    this.zipState = 'loading';
    this.excelState = this.excelState === 'idle' ? 'idle' : this.excelState;

    this.anidExportService.downloadPdfsZip().pipe(
      catchError((err: any) => {
        this.zipState = 'error';
        const msg: string = err?.message || 'Unable to prepare the PDFs ZIP. Please try again.';
        this.errorMessage = msg;
        this.messageService.error(msg);
        return of(null);
      })
    ).subscribe(blob => {
      if (!blob) return;

      if (blob.size === 0) {
        this.zipState = 'empty';
        this.messageService.info('No supporting PDFs were found to prepare this ZIP.');
        return;
      }

      this.downloadBlob(blob, 'anid-supporting-files.zip');
      this.zipState = 'success';
      this.successMessage = 'ZIP prepared. Your download will begin shortly.';
      this.messageService.success('ZIP prepared', 'Download started');
    });
  }

  onGenerateExcel(): void {
    if (!this.isAnidExportCenterVisible || this.isAnyLoading) return;

    this.resetMessages();
    this.excelState = 'loading';

    this.anidExportService.generateExcelWorkbook().pipe(
      catchError((err: any) => {
        this.excelState = 'error';
        const msg: string = err?.message || 'Unable to generate the Excel workbook. Please try again.';
        this.errorMessage = msg;
        this.messageService.error(msg);
        return of(null);
      })
    ).subscribe(blob => {
      if (!blob) return;

      if (blob.size === 0) {
        this.excelState = 'empty';
        this.messageService.info('No data found to generate the Excel workbook.');
        return;
      }

      this.downloadBlob(blob, 'anid-export-workbook.xlsx');
      this.excelState = 'success';
      this.successMessage = 'Workbook generated. Your download will begin shortly.';
      this.messageService.success('Workbook generated', 'Download started');
    });
  }
}

