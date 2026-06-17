import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  LINK_TYPE_OPTIONS,
  LinkEditorDialogData,
  LinkEditorDialogResult,
  LinkType
} from './link-editor.models';
import {
  buildRelAttribute,
  defaultTogglesForLinkType,
  normalizeAndBuildHref,
  supportsNewTab
} from './link-editor.util';

@Component({
  selector: 'app-link-editor-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './link-editor-dialog.component.html',
  styleUrls: ['./link-editor-dialog.component.scss']
})
export class LinkEditorDialogComponent {
  readonly linkTypeOptions = LINK_TYPE_OPTIONS;

  linkType: LinkType;
  value: string;
  openInNewTab: boolean;
  noreferrer: boolean;
  nofollow: boolean;
  sponsored: boolean;
  errorMessage = '';

  constructor(
    private dialogRef: MatDialogRef<LinkEditorDialogComponent, LinkEditorDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: LinkEditorDialogData
  ) {
    this.linkType = data.linkType;
    this.value = data.value;
    this.openInNewTab = data.openInNewTab;
    this.noreferrer = data.noreferrer;
    this.nofollow = data.nofollow;
    this.sponsored = data.sponsored;
  }

  get supportsNewTabOption(): boolean {
    return supportsNewTab(this.linkType);
  }

  get valueLabel(): string {
    switch (this.linkType) {
      case 'email':
        return 'Email address';
      case 'phone':
        return 'Phone number';
      case 'anchor':
        return 'Anchor';
      default:
        return 'URL';
    }
  }

  get valuePlaceholder(): string {
    switch (this.linkType) {
      case 'email':
        return 'example@domain.com';
      case 'phone':
        return '+56 9 1234 5678';
      case 'anchor':
        return '#section-id';
      default:
        return 'Enter or paste a link';
    }
  }

  onLinkTypeChange(): void {
    this.errorMessage = '';
    const defaults = defaultTogglesForLinkType(this.linkType);
    this.openInNewTab = defaults.openInNewTab;
    this.noreferrer = defaults.noreferrer;
  }

  onOpenInNewTabChange(): void {
    if (this.openInNewTab && this.linkType === 'web') {
      this.noreferrer = true;
    }
    if (!this.openInNewTab) {
      this.noreferrer = false;
    }
  }

  cancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }

  removeLink(): void {
    this.dialogRef.close({ action: 'remove' });
  }

  save(): void {
    const built = normalizeAndBuildHref(this.linkType, this.value);
    if (built.error || !built.href) {
      this.errorMessage = built.error || 'Enter a valid link.';
      return;
    }

    const openExternal = supportsNewTab(this.linkType) && this.openInNewTab;
    const rel = buildRelAttribute({
      noreferrer: openExternal && this.noreferrer,
      nofollow: this.nofollow,
      sponsored: this.sponsored
    });

    this.dialogRef.close({
      action: 'save',
      href: built.href,
      target: openExternal ? '_blank' : undefined,
      rel
    });
  }
}
