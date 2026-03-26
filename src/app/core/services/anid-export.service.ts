import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';

@Injectable({
  providedIn: 'root'
})
export class AnidExportService {
  constructor(private baseHttp: BaseHttpService) {}

  /**
   * Downloads supporting PDFs as a ZIP.
   */
  downloadPdfsZip(): Observable<Blob> {
    return this.baseHttp.getFile('/anid-export/pdfs-zip');
  }

  /**
   * Generates a single Excel workbook containing multiple tabs.
   */
  generateExcelWorkbook(): Observable<Blob> {
    return this.baseHttp.getFile('/anid-export/excel-workbook');
  }
}

