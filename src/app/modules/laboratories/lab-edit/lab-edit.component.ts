import { ChangeDetectorRef, Component, OnInit, QueryList, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize, switchMap, map } from 'rxjs/operators';
import { forkJoin, of, Observable } from 'rxjs';

import { TipoRRHHDTO } from '../../../core/models/backend-dtos';

import { RichTextEditorComponent } from '../../news/components/rich-text-editor/rich-text-editor.component';
import { MessageService } from '../../../core/services/message.service';
import { ResearcherService } from '../../../core/services/researcher.service';
import { LaboratoryService } from '../services/laboratory.service';
import { PersonAvatarComponent } from '../components/person-avatar/person-avatar.component';
import {
  PersonAssignmentDialogComponent,
  PersonAssignmentDialogResult
} from '../components/person-assignment-dialog/person-assignment-dialog.component';
import { EndMembershipDialogComponent } from '../components/end-membership-dialog/end-membership-dialog.component';
import { DeletePermanentDialogComponent } from '../components/delete-permanent-dialog/delete-permanent-dialog.component';
import {
  buildSpanishSnapshot,
  CLUSTER_OPTIONS,
  createEmptyLaboratory,
  hasRichTextContent,
  isMembershipActive,
  LAB_STATUS,
  Laboratory,
  LaboratoryMembership,
  matchesPersonSearch,
  MEMBERSHIP_TYPE,
  TRANSLATION_STATUS,
} from '../models/laboratory.models';
import { formatLocalDate } from '../../../core/utils/date.util';
import {
  getTipoRrhhLabel,
  sortTipoRrhhTypes
} from '../services/laboratory-tipo-rrhh.util';

@Component({
  selector: 'app-lab-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
    MatDialogModule,
    MatTableModule,
    MatExpansionModule,
    MatSlideToggleModule,
    RichTextEditorComponent,
    PersonAvatarComponent
  ],
  templateUrl: './lab-edit.component.html',
  styleUrls: ['./lab-edit.component.scss']
})
export class LabEditComponent implements OnInit {
  @ViewChildren(RichTextEditorComponent) richTextEditors!: QueryList<RichTextEditorComponent>;

  isEditMode = false;
  itemId: number | null = null;
  loading = false;
  saving = false;
  translating = false;
  uploadingImage = false;
  selectedTabIndex = 0;

  item: Laboratory = createEmptyLaboratory();
  memberships: LaboratoryMembership[] = [];
  researcherTypes: TipoRRHHDTO[] = [];

  currentMemberSearch = '';
  formerMemberSearch = '';
  formerManagersExpanded = false;
  formerMembersExpanded = false;

  translationReviewWarning = false;
  private validatedSpanishSnapshot = '';

  readonly clusterOptions = CLUSTER_OPTIONS;

  readonly currentMemberColumns = ['person', 'resourceType', 'email', 'orcid', 'startDate', 'actions'];
  readonly formerMemberColumns = ['person', 'resourceType', 'email', 'orcid', 'mobile', 'period', 'actions'];
  readonly formerManagerColumns = ['person', 'resourceType', 'email', 'orcid', 'mobile', 'period', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private laboratoryService: LaboratoryService,
    private researcherService: ResearcherService,
    private messageService: MessageService,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.researcherService.getResearcherTypes().subscribe(types => {
      this.researcherTypes = sortTipoRrhhTypes(types);
    });
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id && id !== 'new') {
        this.isEditMode = true;
        this.itemId = parseInt(id, 10);
        const clusterParam = this.route.snapshot.queryParamMap.get('clusterId');
        const clusterId = clusterParam != null ? parseInt(clusterParam, 10) : undefined;
        this.loadItem(this.itemId, clusterId);
      }
    });
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Edit Laboratory' : 'New Laboratory';
  }

  get isLaboratoryActive(): boolean {
    return this.item.status === LAB_STATUS.ACTIVE;
  }

  set isLaboratoryActive(active: boolean) {
    this.item.status = active ? LAB_STATUS.ACTIVE : LAB_STATUS.INACTIVE;
  }

  get laboratoryActiveHelperText(): string {
    return this.isLaboratoryActive
      ? 'Visible and available in the platform and public website.'
      : 'Inactive laboratories are kept in the system for historical purposes but hidden from public listings.';
  }

  get canSave(): boolean {
    return !!this.item.nameEs?.trim()
      && hasRichTextContent(this.item.descriptionEs)
      && this.item.clusterId != null;
  }

  get hasDirector(): boolean {
    return this.item.directorId != null && !!this.item.directorName;
  }

  get currentLabManager(): LaboratoryMembership | null {
    return this.memberships.find(m =>
      m.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER && isMembershipActive(m)
    ) || null;
  }

  get formerLabManagers(): LaboratoryMembership[] {
    return this.memberships
      .filter(m => m.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER && !isMembershipActive(m))
      .sort((a, b) => (b.endDate || '').localeCompare(a.endDate || ''));
  }

  get currentMembers(): LaboratoryMembership[] {
    return this.memberships
      .filter(m => m.membershipType === MEMBERSHIP_TYPE.MEMBER && isMembershipActive(m))
      .filter(m => matchesPersonSearch(m, this.currentMemberSearch))
      .sort((a, b) => (a.personName || '').localeCompare(b.personName || ''));
  }

  get formerMembers(): LaboratoryMembership[] {
    return this.memberships
      .filter(m => m.membershipType === MEMBERSHIP_TYPE.MEMBER && !isMembershipActive(m))
      .filter(m => matchesPersonSearch(m, this.formerMemberSearch))
      .sort((a, b) => (b.endDate || '').localeCompare(a.endDate || ''));
  }

  loadItem(id: number, clusterId?: number): void {
    this.loading = true;
    this.laboratoryService.getLaboratoryById(id, clusterId).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: lab => {
        this.item = { ...lab };
        this.memberships = lab.memberships || [];
        if (lab.translationStatus === TRANSLATION_STATUS.VALIDATED) {
          this.validatedSpanishSnapshot = buildSpanishSnapshot(lab);
        }
        this.formerManagersExpanded = this.formerLabManagers.length > 0;
        this.checkTranslationReviewWarning();
      },
      error: () => {
        this.messageService.error('Could not load laboratory.');
        this.router.navigate(['/laboratories']);
      }
    });
  }

  reloadLab(): void {
    if (!this.itemId) return;
    this.laboratoryService.getLaboratoryById(this.itemId, this.item.clusterId).subscribe({
      next: lab => {
        this.item = { ...lab };
        this.memberships = lab.memberships || [];
      }
    });
  }

  reloadMemberships(): void {
    if (!this.itemId) return;
    this.laboratoryService.getLaboratoryMembers(this.itemId, this.item.clusterId).subscribe({
      next: list => { this.memberships = list; }
    });
  }

  get directorResourceLabel(): string {
    return this.item.directorResourceTypeLabel
      || getTipoRrhhLabel(this.researcherTypes, this.item.directorResourceType);
  }

  getResourceTypeLabel(value: string | LaboratoryMembership | null | undefined): string {
    if (value && typeof value === 'object') {
      return getTipoRrhhLabel(this.researcherTypes, value.resourceType, value.resourceTypeLabel);
    }
    return getTipoRrhhLabel(this.researcherTypes, value);
  }

  formatDate(date: string | null | undefined): string {
    return formatLocalDate(date, 'dd MMM yyyy', 'en-GB');
  }

  formatPeriod(m: LaboratoryMembership): string {
    const end = m.endDate ? this.formatDate(m.endDate) : '—';
    return `${this.formatDate(m.startDate)} – ${end}`;
  }

  viewResearcherProfile(personId: number | null | undefined): void {
    if (personId) {
      this.router.navigate(['/researchers', personId]);
    }
  }

  private isPendingMembership(m: LaboratoryMembership): boolean {
    return m.id == null;
  }

  private applyDirectorLocal(result: PersonAssignmentDialogResult): void {
    this.item.directorId = result.personId;
    this.item.directorName = result.personName;
    this.item.directorEmail = result.email;
    this.item.directorOrcid = result.orcid;
    this.item.directorMobilePhone = result.mobilePhone;
    this.item.directorIniciales = result.personIniciales;
    this.item.directorProfileImageUrl = result.profileImageUrl ?? undefined;
    this.item.directorResourceType = result.resourceType || undefined;
    this.item.directorResourceTypeLabel = getTipoRrhhLabel(this.researcherTypes, result.resourceType);
  }

  private refreshPersonProfileImage(personId: number | null | undefined, profileImageUrl?: string | null): void {
    if (!personId) return;
    if (this.item.directorId === personId) {
      this.item.directorProfileImageUrl = profileImageUrl ?? undefined;
    }
    this.memberships = this.memberships.map(m =>
      m.personId === personId ? { ...m, profileImageUrl: profileImageUrl ?? undefined } : m
    );
  }

  private applyAssignmentResult(result: PersonAssignmentDialogResult): void {
    this.refreshPersonProfileImage(result.personId, result.profileImageUrl);
  }

  private syncPersonContact(personId: number, result: PersonAssignmentDialogResult): Observable<void> {
    return this.researcherService.getResearcher(personId).pipe(
      switchMap(researcher =>
        this.researcherService.updateResearcher(personId, {
          ...researcher,
          email: result.email || researcher.email,
          orcid: result.orcid || researcher.orcid,
          numCelular: result.mobilePhone || researcher.numCelular,
          idTipoRRHH: result.resourceType
            ? Number(result.resourceType)
            : researcher.idTipoRRHH
        })
      ),
      map(() => undefined)
    );
  }

  private addMembershipLocal(payload: LaboratoryMembership): boolean {
    if (payload.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER && isMembershipActive(payload)) {
      const hasActive = this.memberships.some(m =>
        m.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER && isMembershipActive(m)
      );
      if (hasActive) {
        this.messageService.error('Laboratory already has a current lab manager.');
        return false;
      }
    }
    if (payload.membershipType === MEMBERSHIP_TYPE.MEMBER && isMembershipActive(payload)) {
      const duplicate = this.memberships.some(m =>
        m.membershipType === MEMBERSHIP_TYPE.MEMBER
        && m.personId === payload.personId
        && isMembershipActive(m)
      );
      if (duplicate) {
        this.messageService.error('Person already has an active membership in this laboratory.');
        return false;
      }
    }
    this.memberships = [payload, ...this.memberships];
    return true;
  }

  private updateMembershipLocal(membershipKey: string | undefined, payload: LaboratoryMembership, indexByRef?: LaboratoryMembership): void {
    const idx = membershipKey != null
      ? this.memberships.findIndex(m => m.id === membershipKey)
      : indexByRef != null
        ? this.memberships.indexOf(indexByRef)
        : -1;
    if (idx >= 0) {
      this.memberships[idx] = { ...this.memberships[idx], ...payload };
      this.memberships = [...this.memberships];
    }
  }

  private flushPendingMemberships(laboratoryId: number): Observable<void> {
    const pending = this.memberships.filter(m => this.isPendingMembership(m));
    if (pending.length === 0) {
      return of(undefined);
    }
    return forkJoin(
      pending.map(m => this.laboratoryService.addLaboratoryMember(laboratoryId, this.item.clusterId, m))
    ).pipe(
      switchMap(created => {
        const stillPending = this.memberships.filter(x => this.isPendingMembership(x));
        stillPending.forEach((local, i) => {
          const saved = created[i];
          if (saved?.id) {
            Object.assign(local, saved);
          }
        });
        this.memberships = [...this.memberships];
        return of(undefined);
      })
    );
  }

  // —— Director ——

  openAssignDirector(): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: { role: 'director', mode: 'add' }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result?.personId) return;
      this.applyAssignmentResult(result);
      if (!this.itemId) {
        this.applyDirectorLocal(result);
        this.syncPersonContact(result.personId, result).subscribe({
          next: () => this.messageService.success('Lab director assigned. Save the laboratory to persist.'),
          error: () => this.messageService.success('Lab director assigned locally. Save the laboratory to persist.')
        });
        return;
      }
      this.laboratoryService.assignDirector(this.itemId, this.item.clusterId, {
        personId: result.personId,
        email: result.email,
        orcid: result.orcid,
        mobilePhone: result.mobilePhone
      }).subscribe({
        next: lab => {
          this.item = { ...this.item, ...lab };
          this.messageService.success('Lab director assigned.');
        },
        error: () => this.messageService.error('Could not assign lab director.')
      });
    });
  }

  openEditDirector(): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: {
        role: 'director',
        mode: 'edit',
        directorContact: {
          personId: this.item.directorId,
          personName: this.item.directorName,
          email: this.item.directorEmail,
          orcid: this.item.directorOrcid,
          mobilePhone: this.item.directorMobilePhone,
          personIniciales: this.item.directorIniciales,
          profileImageUrl: this.item.directorProfileImageUrl
        }
      }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result) return;
      this.applyAssignmentResult(result);
      if (!this.itemId) {
        this.item.directorEmail = result.email;
        this.item.directorOrcid = result.orcid;
        this.item.directorMobilePhone = result.mobilePhone;
        if (this.item.directorId) {
          this.syncPersonContact(this.item.directorId, result).subscribe({
            next: () => this.messageService.success('Lab director updated.'),
            error: () => this.messageService.error('Could not update lab director contact.')
          });
        }
        return;
      }
      this.laboratoryService.updateDirectorContact(this.itemId, this.item.clusterId, {
        email: result.email,
        orcid: result.orcid,
        mobilePhone: result.mobilePhone
      }).subscribe({
        next: lab => {
          this.item = { ...this.item, ...lab };
          this.messageService.success('Lab director updated.');
        },
        error: () => this.messageService.error('Could not update lab director.')
      });
    });
  }

  openChangeDirector(): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: { role: 'director', mode: 'change' }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result?.personId) return;
      this.applyAssignmentResult(result);
      if (!this.itemId) {
        this.applyDirectorLocal(result);
        this.syncPersonContact(result.personId, result).subscribe({
          next: () => this.messageService.success('Lab director changed. Save the laboratory to persist.'),
          error: () => this.messageService.success('Lab director changed locally. Save the laboratory to persist.')
        });
        return;
      }
      this.laboratoryService.assignDirector(this.itemId, this.item.clusterId, {
        personId: result.personId,
        email: result.email,
        orcid: result.orcid,
        mobilePhone: result.mobilePhone
      }).subscribe({
        next: lab => {
          this.item = { ...this.item, ...lab };
          this.messageService.success('Lab director changed.');
        },
        error: () => this.messageService.error('Could not change lab director.')
      });
    });
  }

  // —— Lab Manager ——

  openAssignLabManager(): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: { role: 'lab_manager', mode: 'add' }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result?.personId) return;
      this.applyAssignmentResult(result);
      const payload = this.laboratoryService.buildMembershipPayload(result, MEMBERSHIP_TYPE.LAB_MANAGER);
      payload.personName = result.personName;
      payload.personIniciales = result.personIniciales;
      payload.email = result.email;
      payload.orcid = result.orcid;
      payload.mobilePhone = result.mobilePhone;
      if (!this.itemId) {
        if (this.addMembershipLocal(payload)) {
          this.syncPersonContact(result.personId, result).subscribe();
          this.messageService.success('Lab manager assigned. Save the laboratory to persist.');
        }
        return;
      }
      this.laboratoryService.addLaboratoryMember(this.itemId, this.item.clusterId, payload).subscribe({
        next: () => {
          this.messageService.success('Lab manager assigned.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not assign lab manager. Only one current manager is allowed.')
      });
    });
  }

  openEditLabManager(manager: LaboratoryMembership): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: { role: 'lab_manager', mode: 'edit', membership: manager }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result) return;
      this.applyAssignmentResult(result);
      const payload = this.laboratoryService.buildMembershipPayload(result, MEMBERSHIP_TYPE.LAB_MANAGER, manager);
      payload.personName = result.personName || manager.personName;
      payload.personIniciales = result.personIniciales || manager.personIniciales;
      if (!this.itemId || this.isPendingMembership(manager)) {
        this.updateMembershipLocal(manager.id, payload, manager);
        if (result.personId) {
          this.syncPersonContact(result.personId, result).subscribe();
        }
        this.messageService.success('Lab manager updated.');
        return;
      }
      if (!manager.id) return;
      this.laboratoryService.updateLaboratoryMember(this.itemId, this.item.clusterId, manager.id!, payload).subscribe({
        next: () => {
          this.messageService.success('Lab manager updated.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not update lab manager.')
      });
    });
  }

  openChangeLabManager(): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: { role: 'lab_manager', mode: 'change' }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result?.personId) return;
      this.applyAssignmentResult(result);
      if (!this.itemId) {
        const current = this.currentLabManager;
        if (current) {
          current.endDate = new Date().toISOString().slice(0, 10);
          current.status = 'ended';
        }
        const payload = this.laboratoryService.buildMembershipPayload(result, MEMBERSHIP_TYPE.LAB_MANAGER);
        payload.personName = result.personName;
        payload.personIniciales = result.personIniciales;
        if (this.addMembershipLocal(payload)) {
          this.syncPersonContact(result.personId, result).subscribe();
          this.messageService.success('Lab manager changed. Save the laboratory to persist.');
        }
        return;
      }
      this.laboratoryService.changeLabManager(this.itemId, this.item.clusterId, {
        personId: result.personId,
        resourceType: result.resourceType,
        startDate: result.startDate,
        email: result.email,
        orcid: result.orcid,
        mobilePhone: result.mobilePhone
      }).subscribe({
        next: () => {
          this.messageService.success('Lab manager changed.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not change lab manager.')
      });
    });
  }

  endLabManager(manager: LaboratoryMembership): void {
    this.endMembership(manager, 'manager');
  }

  // —— Members ——

  openAddMember(): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px',
      data: { role: 'member', mode: 'add' }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result?.personId) return;
      this.applyAssignmentResult(result);
      const payload = this.laboratoryService.buildMembershipPayload(result, MEMBERSHIP_TYPE.MEMBER);
      payload.personName = result.personName;
      payload.personIniciales = result.personIniciales;
      if (!this.itemId) {
        if (this.addMembershipLocal(payload)) {
          this.syncPersonContact(result.personId, result).subscribe();
          this.messageService.success('Member added. Save the laboratory to persist.');
        }
        return;
      }
      this.laboratoryService.addLaboratoryMember(this.itemId, this.item.clusterId, payload).subscribe({
        next: () => {
          this.messageService.success('Member added.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not add member. Check for duplicate or overlapping periods.')
      });
    });
  }

  openEditMember(member: LaboratoryMembership): void {
    const ref = this.dialog.open(PersonAssignmentDialogComponent, {
      width: '520px', 
      data: { role: 'member', mode: 'edit', membership: member }
    });
    ref.afterClosed().subscribe((result: PersonAssignmentDialogResult | undefined) => {
      if (!result) return;
      this.applyAssignmentResult(result);
      const payload = this.laboratoryService.buildMembershipPayload(result, MEMBERSHIP_TYPE.MEMBER, member);
      payload.personName = result.personName || member.personName;
      if (!this.itemId || this.isPendingMembership(member)) {
        this.updateMembershipLocal(member.id, payload, member);
        if (member.personId) {
          this.syncPersonContact(member.personId, result).subscribe();
        }
        this.messageService.success('Member updated.');
        return;
      }
      if (!member.id) return;
      this.laboratoryService.updateLaboratoryMember(this.itemId, this.item.clusterId, member.id!, payload).subscribe({
        next: () => {
          this.messageService.success('Member updated.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not update member.')
      });
    });
  }

  endMembership(membership: LaboratoryMembership, context: 'member' | 'manager' = 'member'): void {
    const ref = this.dialog.open(EndMembershipDialogComponent, {
      width: '440px',
      data: { personName: membership.personName || 'this person', context }
    });
    ref.afterClosed().subscribe(endDate => {
      if (!endDate) return;
      if (!this.itemId || this.isPendingMembership(membership)) {
        membership.endDate = endDate;
        membership.status = 'ended';
        this.memberships = [...this.memberships];
        this.messageService.success(context === 'manager' ? 'Lab manager assignment ended.' : 'Membership ended.');
        return;
      }
      if (!membership.id) return;
      this.laboratoryService.endLaboratoryMember(this.itemId!, this.item.clusterId, membership.id!, endDate).subscribe({
        next: () => {
          this.messageService.success(context === 'manager' ? 'Lab manager assignment ended.' : 'Membership ended.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not end membership.')
      });
    });
  }

  restoreMembership(membership: LaboratoryMembership): void {
    if (!this.itemId || this.isPendingMembership(membership)) {
      if (membership.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER) {
        const hasActive = this.memberships.some(m =>
          m !== membership
          && m.membershipType === MEMBERSHIP_TYPE.LAB_MANAGER
          && isMembershipActive(m)
        );
        if (hasActive) {
          this.messageService.error('End the current lab manager before restoring another.');
          return;
        }
      }
      membership.endDate = null;
      membership.status = 'active';
      this.memberships = [...this.memberships];
      this.messageService.success('Membership restored.');
      return;
    }
    if (!membership.id) return;
    this.laboratoryService.restoreMembership(this.itemId, this.item.clusterId, membership.id!).subscribe({
      next: () => {
        this.messageService.success('Membership restored.');
        this.reloadMemberships();
      },
      error: () => this.messageService.error('Could not restore membership.')
    });
  }

  deletePermanent(membership: LaboratoryMembership, context: 'manager' | 'member'): void {
    const ref = this.dialog.open(DeletePermanentDialogComponent, {
      width: '440px',
      data: { personName: membership.personName || 'this person', context }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      if (!this.itemId || this.isPendingMembership(membership)) {
        this.memberships = this.memberships.filter(m => m !== membership);
        this.messageService.success('Historical record removed.');
        return;
      }
      if (!membership.id) return;
      this.laboratoryService.deleteMembershipPermanently(this.itemId!, this.item.clusterId, membership.id!).subscribe({
        next: () => {
          this.messageService.success('Historical record deleted.');
          this.reloadMemberships();
        },
        error: () => this.messageService.error('Could not delete record.')
      });
    });
  }

  // —— Translation & save (unchanged logic) ——

  onSpanishFieldChange(): void {
    if (
      this.item.translationStatus === TRANSLATION_STATUS.VALIDATED &&
      this.validatedSpanishSnapshot &&
      buildSpanishSnapshot(this.item) !== this.validatedSpanishSnapshot
    ) {
      this.item.translationStatus = TRANSLATION_STATUS.REQUIRES_REVIEW;
      this.translationReviewWarning = true;
    }
    this.checkTranslationReviewWarning();
  }

  onEnglishFieldChange(): void {
    if (this.item.translationStatus === TRANSLATION_STATUS.NO_TRANSLATION) {
      this.item.translationStatus = TRANSLATION_STATUS.MANUALLY_EDITED;
    } else if (this.item.translationStatus === TRANSLATION_STATUS.AUTO_GENERATED) {
      this.item.translationStatus = TRANSLATION_STATUS.MANUALLY_EDITED;
    }
  }

  checkTranslationReviewWarning(): void {
    this.translationReviewWarning = this.item.translationStatus === TRANSLATION_STATUS.REQUIRES_REVIEW;
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
  }

  flushRichTextEditors(): void {
    this.richTextEditors?.forEach(editor => editor.flushToModel());
  }

  translateFromSpanish(): void {
    this.flushRichTextEditors();
    if (!this.item.nameEs?.trim()) {
      this.messageService.warn('Spanish name is required for translation.');
      return;
    }
    this.confirmOverwriteIfNeeded(
      this.item.nameEn,
      this.item.descriptionEn,
      'Existing English name and/or description will be overwritten. Do you want to continue?',
      () => this.runTranslation('es_to_en', this.item.nameEs, this.item.descriptionEs)
    );
  }

  translateFromEnglish(): void {
    this.flushRichTextEditors();
    if (!this.item.nameEn?.trim()) {
      this.messageService.warn('English name is required for translation.');
      return;
    }
    this.confirmOverwriteIfNeeded(
      this.item.nameEs,
      this.item.descriptionEs,
      'Existing Spanish name and/or description will be overwritten. Do you want to continue?',
      () => this.runTranslation('en_to_es', this.item.nameEn, this.item.descriptionEn)
    );
  }

  private confirmOverwriteIfNeeded(
    name: string | null | undefined,
    description: string | null | undefined,
    message: string,
    onConfirm: () => void
  ): void {
    const hasContent = !!name?.trim() || hasRichTextContent(description);
    if (!hasContent) {
      onConfirm();
      return;
    }
    this.messageService.confirm(message, (accepted: boolean) => {
      if (accepted) {
        onConfirm();
      }
    }, 'Overwrite content?');
  }

  private runTranslation(
    direction: 'es_to_en' | 'en_to_es',
    name: string | null | undefined,
    description: string | null | undefined
  ): void {
    this.translating = true;
    this.laboratoryService.generateLaboratoryTranslation(direction, {
      name: name ?? '',
      description: description ?? ''
    }).pipe(
      finalize(() => { this.translating = false; })
    ).subscribe({
      next: result => {
        this.item = this.laboratoryService.applyTranslationResult(this.item, result, direction);
        this.messageService.success(
          direction === 'es_to_en'
            ? 'English translation generated.'
            : 'Spanish translation generated.'
        );
        this.cdr.detectChanges();
      },
      error: () => this.messageService.error('Translation failed.')
    });
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploadingImage = true;
    this.laboratoryService.uploadImage(file).pipe(
      finalize(() => { this.uploadingImage = false; })
    ).subscribe({
      next: url => {
        this.item.imageUrl = url;
        this.messageService.success('Image uploaded.');
      },
      error: () => this.messageService.error('Image upload failed.')
    });
  }

  getImagePreviewUrl(): string | null {
    return this.laboratoryService.resolveMediaUrl(this.item.imageUrl);
  }

  getPersonImageUrl(url?: string | null): string | null {
    return this.laboratoryService.resolveMediaUrl(url);
  }

  save(): void {
    this.flushRichTextEditors();
    if (!this.canSave) {
      this.messageService.warn('Complete required fields: Spanish name, description and cluster.');
      return;
    }
    this.saving = true;
    const request$ = this.isEditMode && this.itemId
      ? this.laboratoryService.updateLaboratory(this.itemId, this.item)
      : this.laboratoryService.createLaboratory(this.item);

    request$.pipe(
      switchMap(saved => {
        this.itemId = saved.id ?? this.itemId;
        this.isEditMode = true;
        return this.flushPendingMemberships(saved.id!).pipe(map(() => saved));
      }),
      finalize(() => { this.saving = false; })
    ).subscribe({
      next: () => {
        this.messageService.success('Laboratory saved.');
        this.router.navigate(['/laboratories']);
      },
      error: () => this.messageService.error('Could not save laboratory.')
    });
  }

  saveAndView(): void {
    this.flushRichTextEditors();
    if (!this.canSave) return;
    this.saving = true;
    const request$ = this.isEditMode && this.itemId
      ? this.laboratoryService.updateLaboratory(this.itemId, this.item)
      : this.laboratoryService.createLaboratory(this.item);

    request$.pipe(
      switchMap(saved => {
        this.itemId = saved.id ?? this.itemId;
        this.isEditMode = true;
        return this.flushPendingMemberships(saved.id!).pipe(map(() => saved));
      }),
      finalize(() => { this.saving = false; })
    ).subscribe({
      next: saved => {
        this.messageService.success('Laboratory saved.');
        this.router.navigate(['/laboratories', saved.id, 'view'], {
          queryParams: { clusterId: saved.clusterId }
        });
      },
      error: () => this.messageService.error('Could not save laboratory.')
    });
  }

  backToList(): void {
    this.router.navigate(['/laboratories']);
  }
}
