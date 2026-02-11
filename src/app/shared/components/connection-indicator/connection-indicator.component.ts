import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { Subscription } from 'rxjs';
import { ConnectionService } from '../../../core/services/connection.service';

@Component({
  selector: 'app-connection-indicator',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatTooltipModule,
    MatButtonModule
  ],
  templateUrl: './connection-indicator.component.html',
  styleUrls: ['./connection-indicator.component.scss']
})
export class ConnectionIndicatorComponent implements OnInit, OnDestroy {
  isConnected = true;
  isRetrying = false;
  tooltipText = 'Connected to server';
  
  private subscription?: Subscription;

  constructor(
    private connectionService: ConnectionService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.subscription = this.connectionService.connectionStatus$.subscribe(
      (connected) => {
        this.isConnected = connected;
        this.updateTooltip();
        // Forzar detección de cambios para asegurar que la UI se actualice
        this.cdr.detectChanges();
      }
    );
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  onRetryConnection(): void {
    this.isRetrying = true;
    this.cdr.detectChanges(); // Forzar actualización del estado de retry
    
    this.connectionService.forceConnectionCheck().subscribe({
      next: (connected) => {
        this.isRetrying = false;
        // Obtener el estado actual del servicio para asegurar sincronización
        const currentStatus = this.connectionService.isConnected();
        if (this.isConnected !== currentStatus) {
          this.isConnected = currentStatus;
          this.updateTooltip();
        }
        // Forzar detección de cambios para asegurar que la UI se actualice
        this.cdr.detectChanges();
      },
      error: () => {
        this.isRetrying = false;
        // Obtener el estado actual del servicio para asegurar sincronización
        const currentStatus = this.connectionService.isConnected();
        if (this.isConnected !== currentStatus) {
          this.isConnected = currentStatus;
          this.updateTooltip();
        }
        // Forzar detección de cambios
        this.cdr.detectChanges();
      }
    });
  }

  private updateTooltip(): void {
    if (this.isConnected) {
      this.tooltipText = 'Connected to server';
    } else {
      this.tooltipText = 'Disconnected from server. Click to retry.';
    }
  }
}
