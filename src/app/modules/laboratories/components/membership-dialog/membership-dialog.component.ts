import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { ResearcherSearchComponent } from '../../../../shared/components/researcher-search/researcher-search.component';
import { RRHHDTO, TipoRRHHDTO } from '../../../../core/models/backend-dtos';
import { ResearcherService } from '../../../../core/services/researcher.service';
import {
  LaboratoryMembership,
  MEMBERSHIP_TYPE,
  MembershipType
} from '../../models/laboratory.models';
import {
  findTipoRrhhByDescription,
  sortTipoRrhhTypes,
  tipoRrhhIdFromValue
} from '../../services/laboratory-tipo-rrhh.util';

export interface MembershipDialogData {
  mode: 'add' | 'edit';
  membershipType: MembershipType;
  membership?: LaboratoryMembership;
}

@Component({
  selector: 'app-membership-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    ResearcherSearchComponent
  ],
  templateUrl: './membership-dialog.component.html',
  styleUrls: ['./membership-dialog.component.scss']
})
export class MembershipDialogComponent implements OnInit {
  personId: number | null = null;
  selectedResearcher: RRHHDTO | null = null;
  resourceType = '';
  researcherTypes: TipoRRHHDTO[] = [];
  startDate = '';
  endDate = '';
  errorMessage = '';

  constructor(
    private dialogRef: MatDialogRef<MembershipDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MembershipDialogData,
    private researcherService: ResearcherService
  ) {}

  get title(): string {
    const label = this.data.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER ? 'Lab Manager' : 'Member';
    return this.data.mode === 'add' ? `Add ${label}` : `Edit ${label}`;
  }

  get isEditMode(): boolean {
    return this.data.mode === 'edit';
  }

  resourceTypeValue(tipo: TipoRRHHDTO): string {
    return tipo.id != null ? String(tipo.id) : '';
  }

  get defaultResourceTypeId(): string {
    if (this.data.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER) {
      return tipoRrhhIdFromValue(
        findTipoRrhhByDescription(this.researcherTypes, 'lab manager', 'manager')?.id
      );
    }
    return '';
  }

  ngOnInit(): void {
    this.researcherService.getResearcherTypes().subscribe(types => {
      this.researcherTypes = sortTipoRrhhTypes(types);
      this.initFormState();
    });
  }

  private initFormState(): void {
    if (this.data.membership) {
      this.personId = this.data.membership.personId;
      this.resourceType = this.data.membership.resourceType || this.defaultResourceTypeId;
      this.startDate = this.data.membership.startDate || '';
      this.endDate = this.data.membership.endDate || '';
      if (this.data.membership.personName) {
        this.selectedResearcher = {
          id: this.data.membership.personId!,
          fullname: this.data.membership.personName
        } as RRHHDTO;
      }
    } else {
      this.resourceType = this.defaultResourceTypeId;
      this.startDate = new Date().toISOString().slice(0, 10);
    }
  }

  onResearcherSelected(researcher: RRHHDTO): void {
    this.personId = researcher.id ?? null;
    this.selectedResearcher = researcher;
    if (researcher.idTipoRRHH != null) {
      this.resourceType = tipoRrhhIdFromValue(researcher.idTipoRRHH);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.errorMessage = '';
    if (!this.isEditMode && !this.personId) {
      this.errorMessage = 'Person is required.';
      return;
    }
    if (!this.resourceType) {
      this.errorMessage = 'Resource type is required.';
      return;
    }
    if (!this.startDate) {
      this.errorMessage = 'Start date is required.';
      return;
    }
    if (this.endDate && this.endDate < this.startDate) {
      this.errorMessage = 'End date must be after start date.';
      return;
    }

    const result: LaboratoryMembership = {
      id: this.data.membership?.id,
      personId: this.personId,
      personName: this.selectedResearcher?.fullname,
      personEmail: this.selectedResearcher?.email,
      membershipType: this.data.membershipType,
      resourceType: this.resourceType,
      startDate: this.startDate,
      endDate: this.endDate || null,
      status: this.data.membership?.status || 'active'
    };
    this.dialogRef.close(result);
  }
}
