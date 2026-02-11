import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export type ViewMode = 'list' | 'card';

@Component({
  selector: 'app-view-mode-selector',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatTooltipModule
  ],
  template: `
    <mat-button-toggle-group 
      [value]="currentMode" 
      (change)="onModeChange($event.value)"
      class="view-mode-selector">
      <mat-button-toggle value="list" matTooltip="List View">
        <mat-icon>view_list</mat-icon>
      </mat-button-toggle>
      <mat-button-toggle value="card" matTooltip="Card View">
        <mat-icon>view_module</mat-icon>
      </mat-button-toggle>
    </mat-button-toggle-group>
  `,
  styles: [`
    .view-mode-selector {
      margin-left: auto;
    }
    
    .view-mode-selector .mat-button-toggle {
      border-radius: 4px;
      border: 1px solid #e0e0e0;
    }
    
    .view-mode-selector .mat-button-toggle:first-child {
      border-top-right-radius: 0;
      border-bottom-right-radius: 0;
    }
    
    .view-mode-selector .mat-button-toggle:last-child {
      border-top-left-radius: 0;
      border-bottom-left-radius: 0;
      border-left: none;
    }
    
    .view-mode-selector .mat-button-toggle.mat-button-toggle-checked {
      background-color: #1976d2;
      color: white;
    }
    
    .view-mode-selector .mat-button-toggle mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
  `]
})
export class ViewModeSelectorComponent {
  @Input() currentMode: ViewMode = 'list';
  @Output() modeChange = new EventEmitter<ViewMode>();

  onModeChange(mode: ViewMode): void {
    this.currentMode = mode;
    this.modeChange.emit(mode);
  }
}
