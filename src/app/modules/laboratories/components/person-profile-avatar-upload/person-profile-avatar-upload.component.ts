import { Component, ElementRef, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { getPersonInitials } from '../../models/laboratory.models';
import { LaboratoryService } from '../../services/laboratory.service';

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const ALLOWED_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.webp']);
// TODO: align with backend if a different limit is configured server-side
const MAX_FILE_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-person-profile-avatar-upload',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './person-profile-avatar-upload.component.html',
  styleUrls: ['./person-profile-avatar-upload.component.scss']
})
export class PersonProfileAvatarUploadComponent implements OnInit, OnChanges, OnDestroy {
  @Input() name = '';
  @Input() iniciales = '';
  @Input() profileImageUrl: string | null = null;
  @Input() disabled = false;

  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  displayImageUrl: string | null = null;
  validationError = '';
  pendingFile: File | null = null;

  private blobPreviewUrl: string | null = null;

  constructor(private laboratoryService: LaboratoryService) {}

  get initials(): string {
    return getPersonInitials(this.name, this.iniciales);
  }

  get hasImage(): boolean {
    return !!this.displayImageUrl;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['profileImageUrl'] && !this.pendingFile) {
      this.setServerImage(this.profileImageUrl);
    }
    if (changes['disabled']?.currentValue === true) {
      this.clearPendingSelection();
    }
  }

  ngOnInit(): void {
    if (this.profileImageUrl && !this.pendingFile) {
      this.setServerImage(this.profileImageUrl);
    }
  }

  ngOnDestroy(): void {
    this.revokeBlobPreview();
  }

  resetToServerImage(url: string | null | undefined): void {
    this.clearPendingSelection();
    this.setServerImage(url ?? null);
  }

  onAvatarClick(): void {
    if (this.disabled) return;
    this.fileInput?.nativeElement.click();
  }

  onAvatarKeydown(event: KeyboardEvent): void {
    if (this.disabled) return;
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.onAvatarClick();
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.disabled) return;

    this.validationError = '';
    const validationMessage = this.validateFile(file);
    if (validationMessage) {
      this.validationError = validationMessage;
      return;
    }

    this.revokeBlobPreview();
    this.pendingFile = file;
    this.blobPreviewUrl = URL.createObjectURL(file);
    this.displayImageUrl = this.blobPreviewUrl;
  }

  private validateFile(file: File): string | null {
    const type = (file.type || '').toLowerCase();
    const extension = this.getExtension(file.name);
    const typeAllowed = ALLOWED_TYPES.has(type) || (type === '' && ALLOWED_EXTENSIONS.has(extension));
    if (!typeAllowed) {
      return 'Only JPG, JPEG, PNG and WEBP images are allowed.';
    }
    if (file.size > MAX_FILE_BYTES) {
      return 'Image must be 5 MB or smaller.';
    }
    return null;
  }

  private getExtension(filename: string): string {
    const idx = filename.lastIndexOf('.');
    return idx >= 0 ? filename.slice(idx).toLowerCase() : '';
  }

  private setServerImage(url: string | null): void {
    this.displayImageUrl = this.laboratoryService.resolveMediaUrl(url);
  }

  private clearPendingSelection(): void {
    this.pendingFile = null;
    this.revokeBlobPreview();
    this.validationError = '';
  }

  private revokeBlobPreview(): void {
    if (this.blobPreviewUrl) {
      URL.revokeObjectURL(this.blobPreviewUrl);
      this.blobPreviewUrl = null;
    }
  }
}
