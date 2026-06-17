import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs/operators';
import { NewsService } from '../../services/news.service';
import {
  NEWS_VIDEO_MAX_UPLOAD_BYTES,
  NEWS_VIDEO_UPLOAD_MIME_TYPES,
  normalizeVideoEmbedUrl
} from '../../services/news-video.util';

export type VideoInsertSource = 'embed' | 'upload';

export interface VideoInsertDialogData {
  source?: VideoInsertSource;
  embedUrl?: string;
  isEditing?: boolean;
}

export interface VideoInsertDialogResult {
  action: 'embed' | 'upload' | 'cancel';
  embedUrl?: string;
  uploadedPath?: string;
  mimeType?: string;
}

@Component({
  selector: 'app-video-insert-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './video-insert-dialog.component.html',
  styleUrls: ['./video-insert-dialog.component.scss']
})
export class VideoInsertDialogComponent {
  source: VideoInsertSource;
  videoUrl = '';
  selectedFile: File | null = null;
  errorMessage = '';
  uploading = false;

  readonly maxUploadMb = Math.round(NEWS_VIDEO_MAX_UPLOAD_BYTES / (1024 * 1024));
  readonly uploadEnabled = true;

  constructor(
    private dialogRef: MatDialogRef<VideoInsertDialogComponent, VideoInsertDialogResult>,
    private newsService: NewsService,
    @Inject(MAT_DIALOG_DATA) public data: VideoInsertDialogData
  ) {
    this.source = data.source ?? 'embed';
    this.videoUrl = data.embedUrl ?? '';
  }

  get isEditing(): boolean {
    return !!this.data.isEditing;
  }

  onSourceChange(): void {
    this.errorMessage = '';
    this.selectedFile = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    this.selectedFile = file;
    this.errorMessage = '';
    if (!file) return;

    if (!NEWS_VIDEO_UPLOAD_MIME_TYPES.includes(file.type)) {
      this.selectedFile = null;
      this.errorMessage = 'Supported formats: MP4 and WebM.';
      return;
    }
    if (file.size > NEWS_VIDEO_MAX_UPLOAD_BYTES) {
      this.selectedFile = null;
      this.errorMessage = `Maximum file size is ${this.maxUploadMb} MB.`;
    }
  }

  cancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }

  embedVideo(): void {
    const result = normalizeVideoEmbedUrl(this.videoUrl);
    if (result.error || !result.embedUrl) {
      this.errorMessage = result.error || 'Enter a valid video URL.';
      return;
    }
    this.dialogRef.close({
      action: 'embed',
      embedUrl: result.originalUrl || this.videoUrl.trim()
    });
  }

  uploadVideo(): void {
    if (!this.uploadEnabled) {
      this.errorMessage = 'Video upload is not available yet. Use Embed for now.';
      return;
    }
    if (!this.selectedFile) {
      this.errorMessage = 'Select a video file to upload.';
      return;
    }

    this.uploading = true;
    this.errorMessage = '';
    this.newsService.uploadNewsVideo(this.selectedFile).pipe(
      finalize(() => { this.uploading = false; })
    ).subscribe({
      next: path => {
        if (!path) {
          this.errorMessage = 'Video upload failed. Please try again.';
          return;
        }
        this.dialogRef.close({
          action: 'upload',
          uploadedPath: path,
          mimeType: this.selectedFile!.type
        });
      },
      error: () => {
        this.errorMessage = 'Video upload failed. Please try again.';
      }
    });
  }
}
