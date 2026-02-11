import { Injectable } from '@angular/core';
import { MessageService as PrimeMessageService } from 'primeng/api';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/components/confirm-dialog/confirm-dialog.component';

export type MessageSeverity = 'success' | 'info' | 'warn' | 'error';

@Injectable({
  providedIn: 'root'
})
export class MessageService {

  constructor(
    private primeMessageService: PrimeMessageService,
    private dialog: MatDialog
  ) { }

  /**
   * Show a success message
   */
  success(message: string, action?: string): void {
    this.show(message, action, 'success');
  }

  /**
   * Show an info message
   */
  info(message: string, action?: string): void {
    this.show(message, action, 'info');
  }

  /**
   * Show a warning message
   */
  warn(message: string, action?: string): void {
    this.show(message, action, 'warn');
  }

  /**
   * Show an error message
   */
  error(message: string, action?: string): void {
    this.show(message, action, 'error');
  }

  /**
   * Show a custom message
   */
  show(message: string, action?: string, severity: MessageSeverity = 'info', duration: number = 3000): void {
    this.primeMessageService.add({
      severity: severity,
      summary: action || this.getDefaultSummary(severity),
      detail: message,
      life: duration
    });
  }

  /**
   * Get default summary based on severity
   */
  private getDefaultSummary(severity: MessageSeverity): string {
    switch (severity) {
      case 'success': return 'Success';
      case 'info': return 'Information';
      case 'warn': return 'Warning';
      case 'error': return 'Error';
      default: return 'Message';
    }
  }

  /**
   * Show a confirmation dialog
   */
  confirm(message: string, callback: (accepted: boolean) => void, title: string = 'Confirm Action'): void {
    const dialogData: ConfirmDialogData = {
      title: title,
      message: message,
      confirmText: 'Yes',
      cancelText: 'No'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData,
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      callback(result === true);
    });
  }
}
