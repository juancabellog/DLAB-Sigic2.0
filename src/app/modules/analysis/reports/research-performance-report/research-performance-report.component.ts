import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MessageService } from '../../../../core/services/message.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-research-performance-report',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './research-performance-report.component.html',
  styleUrls: ['./research-performance-report.component.scss']
})
export class ResearchPerformanceReportComponent implements OnInit {
  loading = false;
  generating = false;

  constructor(
    private http: HttpClient,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    // Component initialization
  }

  generateReport(): void {
    this.generating = true;
    
    const request = { idReport: 1 }; // Research Performance Report
    const apiUrl = environment.apiUrl || 'http://localhost:8081/sigic2.0/api';
    
    this.http.post(`${apiUrl}/analysis/generate-report`, request, {
      responseType: 'blob',
      observe: 'response',
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    }).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.messageService.error('Empty response from server');
          this.generating = false;
          return;
        }
        
        // Check if the response is an error JSON (when the status is 4xx or 5xx)
        if (response.status >= 400) {
          blob.text().then((text: string) => {
            try {
              const errorObj = JSON.parse(text);
              this.messageService.error(errorObj.error || 'Error generating report');
            } catch {
              this.messageService.error('Error generating report');
            }
            this.generating = false;
          });
          return;
        }
        
        // Create blob URL and download
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'research-performance-report.xlsx';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        this.generating = false;
        this.messageService.success('Report generated successfully');
      },
      error: (error) => {
        console.error('Error generating report:', error);
        let errorMessage = 'Error generating report. Please try again.';
        
        // Try to extract error message from backend
        if (error.error) {
          try {
            // If the error is a blob (because we expected a blob but received JSON)
            if (error.error instanceof Blob) {
              error.error.text().then((text: string) => {
                try {
                  const errorObj = JSON.parse(text);
                  this.messageService.error(errorObj.error || errorMessage);
                } catch {
                  this.messageService.error(errorMessage);
                }
                this.generating = false;
              });
              return;
            } else if (typeof error.error === 'object' && error.error.error) {
              errorMessage = error.error.error;
            }
          } catch (e) {
            console.error('Error parsing error response:', e);
          }
        }
        
        this.messageService.error(errorMessage);
        this.generating = false;
      }
    });
  }
}
