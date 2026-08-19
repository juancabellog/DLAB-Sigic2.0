import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface DeletePermanentDialogData {
  personName: string;
  context: 'manager' | 'member';
}

@Component({
  selector: 'app-delete-permanent-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Delete historical membership?</h2>
    <mat-dialog-content>
      <p>
        This will permanently delete the historical record for
        <strong>{{ data.personName }}</strong> from the database.
        This action cannot be undone.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="cancel()">Cancel</button>
      <button mat-raised-button color="warn" type="button" (click)="confirm()">Delete permanently</button>
    </mat-dialog-actions>
  `
})
export class DeletePermanentDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<DeletePermanentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DeletePermanentDialogData
  ) {}

  cancel(): void { this.dialogRef.close(false); }
  confirm(): void { this.dialogRef.close(true); }
}
