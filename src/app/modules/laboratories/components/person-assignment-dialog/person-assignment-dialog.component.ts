import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs/operators';

import { ResearcherSearchComponent } from '../../../../shared/components/researcher-search/researcher-search.component';
import { PersonAvatarComponent } from '../person-avatar/person-avatar.component';
import { PersonProfileAvatarUploadComponent } from '../person-profile-avatar-upload/person-profile-avatar-upload.component';
import { RRHHDTO, TipoRRHHDTO } from '../../../../core/models/backend-dtos';
import { ResearcherService } from '../../../../core/services/researcher.service';
import { MessageService } from '../../../../core/services/message.service';
import { LaboratoryService } from '../../services/laboratory.service';
import {
  findTipoRrhhByDescription,
  sortTipoRrhhTypes,
  tipoRrhhIdFromValue
} from '../../services/laboratory-tipo-rrhh.util';
import { LaboratoryMembership } from '../../models/laboratory.models';

export type AssignmentRole = 'director' | 'lab_manager' | 'member';
export type AssignmentMode = 'add' | 'edit' | 'change';

export interface PersonAssignmentDialogData {
  role: AssignmentRole;
  mode: AssignmentMode;
  membership?: LaboratoryMembership;
  directorContact?: {
    personId: number | null;
    personName?: string;
    email?: string;
    orcid?: string;
    mobilePhone?: string;
    personIniciales?: string;
    profileImageUrl?: string | null;
  };
}

export interface PersonAssignmentDialogResult {
  personId: number | null;
  personName?: string;
  personIniciales?: string;
  profileImageUrl?: string | null;
  resourceType: string;
  startDate: string;
  endDate: string | null;
  email: string;
  orcid: string;
  mobilePhone: string;
  membershipId?: string;
}

@Component({
  selector: 'app-person-assignment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ResearcherSearchComponent,
    PersonAvatarComponent,
    PersonProfileAvatarUploadComponent
  ],
  templateUrl: './person-assignment-dialog.component.html',
  styleUrls: ['./person-assignment-dialog.component.scss']
})
export class PersonAssignmentDialogComponent implements OnInit {
  @ViewChild(PersonProfileAvatarUploadComponent) avatarUpload?: PersonProfileAvatarUploadComponent;

  personId: number | null = null;
  selectedResearcher: RRHHDTO | null = null;
  profileImageUrl: string | null = null;
  resourceType = '';
  researcherTypes: TipoRRHHDTO[] = [];
  startDate = '';
  endDate = '';
  email = '';
  orcid = '';
  mobilePhone = '';
  errorMessage = '';
  saving = false;

  readonly contactHelperText = 'Changes to ORCID, email, mobile and resource type update the person profile.';

  constructor(
    private dialogRef: MatDialogRef<PersonAssignmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PersonAssignmentDialogData,
    private researcherService: ResearcherService,
    private messageService: MessageService,
    private laboratoryService: LaboratoryService
  ) {}

  get title(): string {
    const roleLabel = this.data.role === 'director'
      ? 'Lab Director'
      : this.data.role === 'lab_manager'
        ? 'Lab Manager'
        : 'Member';
    if (this.data.mode === 'edit') return `Edit ${roleLabel}`;
    if (this.data.mode === 'change') return `Change ${roleLabel}`;
    return this.data.role === 'director' ? `Assign ${roleLabel}` : `Add ${roleLabel}`;
  }

  get isEditMode(): boolean {
    return this.data.mode === 'edit';
  }

  get showPersonSearch(): boolean {
    return !this.isEditMode;
  }

  get showDates(): boolean {
    return this.data.role !== 'director';
  }

  get showResourceType(): boolean {
    return this.data.role !== 'director';
  }

  get showProfileImageUpload(): boolean {
    return this.personId != null;
  }

  get defaultResourceTypeId(): string {
    if (this.data.role === 'lab_manager') {
      return tipoRrhhIdFromValue(
        findTipoRrhhByDescription(this.researcherTypes, 'lab manager', 'manager')?.id
      );
    }
    return '';
  }

  get personProfileLink(): string | null {
    return this.personId ? `/researchers/${this.personId}/edit` : null;
  }

  get resolvedProfileImageUrl(): string | null {
    return this.laboratoryService.resolveMediaUrl(this.profileImageUrl);
  }

  ngOnInit(): void {
    this.researcherService.getResearcherTypes().subscribe(types => {
      this.researcherTypes = sortTipoRrhhTypes(types);
      this.initFormState();
    });
  }

  private initFormState(): void {
    if (this.data.membership) {
      const m = this.data.membership;
      this.personId = m.personId;
      this.profileImageUrl = m.profileImageUrl ?? null;
      this.resourceType = m.resourceType || this.defaultResourceTypeId;
      this.startDate = m.startDate || '';
      this.endDate = m.endDate || '';
      this.email = m.email || m.personEmail || '';
      this.orcid = m.orcid || '';
      this.mobilePhone = m.mobilePhone || '';
      if (m.personName) {
        this.selectedResearcher = {
          id: m.personId!,
          fullname: m.personName,
          email: this.email,
          orcid: this.orcid,
          numCelular: this.mobilePhone,
          iniciales: m.personIniciales,
          profileImageUrl: m.profileImageUrl ?? undefined
        } as RRHHDTO;
      }
    } else if (this.data.directorContact) {
      const d = this.data.directorContact;
      this.personId = d.personId;
      this.profileImageUrl = d.profileImageUrl ?? null;
      this.email = d.email || '';
      this.orcid = d.orcid || '';
      this.mobilePhone = d.mobilePhone || '';
      if (d.personName) {
        this.selectedResearcher = {
          id: d.personId!,
          fullname: d.personName,
          email: this.email,
          orcid: this.orcid,
          numCelular: this.mobilePhone,
          iniciales: d.personIniciales,
          profileImageUrl: d.profileImageUrl ?? undefined
        } as RRHHDTO;
      }
    } else {
      this.startDate = new Date().toISOString().slice(0, 10);
      this.resourceType = this.defaultResourceTypeId;
    }
  }

  onResearcherSelected(researcher: RRHHDTO): void {
    this.personId = researcher.id ?? null;
    this.selectedResearcher = researcher;
    this.email = researcher.email || '';
    this.orcid = researcher.orcid || '';
    this.mobilePhone = researcher.numCelular || '';
    this.profileImageUrl = researcher.profileImageUrl ?? null;
    this.resourceType = tipoRrhhIdFromValue(researcher.idTipoRRHH) || this.resourceType;
    this.avatarUpload?.resetToServerImage(this.profileImageUrl);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    if (this.saving) return;
    this.errorMessage = '';
    if (!this.isEditMode && !this.personId) {
      this.errorMessage = 'Person is required.';
      return;
    }
    if (this.showResourceType && !this.resourceType) {
      this.errorMessage = 'Resource type is required.';
      return;
    }
    if (this.showDates && !this.startDate) {
      this.errorMessage = 'Start date is required.';
      return;
    }
    if (this.endDate && this.startDate && this.endDate < this.startDate) {
      this.errorMessage = 'End date must be on or after start date.';
      return;
    }
    if (this.avatarUpload?.validationError) {
      this.errorMessage = this.avatarUpload.validationError;
      return;
    }

    const pendingFile = this.avatarUpload?.pendingFile ?? null;
    if (pendingFile && this.personId) {
      this.saving = true;
      this.researcherService.uploadProfileImage(this.personId, pendingFile).pipe(
        finalize(() => { this.saving = false; })
      ).subscribe({
        next: updated => this.closeWithResult(updated.profileImageUrl ?? null),
        error: () => this.messageService.error('Profile image could not be uploaded.')
      });
      return;
    }

    this.closeWithResult(this.profileImageUrl);
  }

  private closeWithResult(profileImageUrl: string | null): void {
    if (this.selectedResearcher) {
      this.selectedResearcher.profileImageUrl = profileImageUrl ?? undefined;
    }
    this.profileImageUrl = profileImageUrl;

    const result: PersonAssignmentDialogResult = {
      personId: this.personId,
      personName: this.selectedResearcher?.fullname,
      personIniciales: this.selectedResearcher?.iniciales,
      profileImageUrl,
      resourceType: this.showResourceType ? this.resourceType : '',
      startDate: this.showDates ? this.startDate : new Date().toISOString().slice(0, 10),
      endDate: this.showDates && this.endDate ? this.endDate : null,
      email: this.email.trim(),
      orcid: this.orcid.trim(),
      mobilePhone: this.mobilePhone.trim(),
      membershipId: this.data.membership?.id
    };
    this.dialogRef.close(result);
  }
}
