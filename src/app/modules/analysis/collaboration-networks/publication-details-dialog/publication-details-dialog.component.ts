import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-publication-details-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './publication-details-dialog.component.html',
  styleUrls: ['./publication-details-dialog.component.scss']
})
export class PublicationDetailsDialogComponent {
  displayedColumns: string[] = ['id', 'descripcion', 'progressReport', 'journal', 'doi'];

  constructor(
    public dialogRef: MatDialogRef<PublicationDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      publications: any[];
      sourceNode: any;
      targetNode: any;
    }
  ) {}

  openDOI(doi: string): void {
    if (doi) {
      window.open(`https://doi.org/${doi}`, '_blank');
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
