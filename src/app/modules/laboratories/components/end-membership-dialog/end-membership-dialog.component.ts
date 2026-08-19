import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface EndMembershipDialogData {
  personName: string;
  context?: 'member' | 'manager';
}

@Component({
  selector: 'app-end-membership-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './end-membership-dialog.component.html'
})
export class EndMembershipDialogComponent {
  endDate = new Date().toISOString().slice(0, 10);
  errorMessage = '';

  constructor(
    private dialogRef: MatDialogRef<EndMembershipDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EndMembershipDialogData
  ) {}

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    if (!this.endDate) {
      this.errorMessage = 'End date is required.';
      return;
    }
    this.dialogRef.close(this.endDate);
  }
}
