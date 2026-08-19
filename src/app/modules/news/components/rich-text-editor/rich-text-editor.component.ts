import {
  Component,
  ElementRef,
  Input,
  ViewChild,
  forwardRef,
  AfterViewInit,
  OnDestroy,
  HostListener,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { NewsService } from '../../services/news.service';
import { LinkEditorDialogComponent } from './link-editor-dialog.component';
import { LinkEditorDialogData, LinkEditorDialogResult } from './link-editor.models';
import {
  VideoInsertDialogComponent,
  VideoInsertDialogData,
  VideoInsertDialogResult
} from './video-insert-dialog.component';
import { getVideoBlockType, isNewsVideoBlock } from '../../services/news-video.util';
import {
  defaultTogglesForLinkType,
  isSafeHref,
  parseHrefToLinkForm,
  parseRelAttribute
} from './link-editor.util';
import { MessageService } from '../../../../core/services/message.service';

export type NewsImageSize = 'small' | 'medium' | 'large' | 'full';
export type NewsImageAlign = 'left' | 'center' | 'right';
export type NewsImageTextWrap = 'text-right' | 'text-left' | 'text-below';
export type BlockFormat = 'p' | 'h1' | 'h2' | 'h3' | 'blockquote';

const SIZE_CLASSES: NewsImageSize[] = ['small', 'medium', 'large', 'full'];
const ALIGN_CLASSES: NewsImageAlign[] = ['left', 'center', 'right'];
const WRAP_CLASSES = ['news-image-float-left', 'news-image-float-right', 'news-image-block'] as const;
const BLOCK_TAGS = new Set(['P', 'H1', 'H2', 'H3', 'H4', 'BLOCKQUOTE', 'LI', 'DIV', 'PRE']);

@Component({
  selector: 'app-rich-text-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDividerModule
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RichTextEditorComponent),
      multi: true
    }
  ],
  templateUrl: './rich-text-editor.component.html',
  styleUrls: ['./rich-text-editor.component.scss']
})
export class RichTextEditorComponent implements ControlValueAccessor, AfterViewInit, OnDestroy {
  @Input() placeholder = 'Write content...';
  @Input() minHeight = '220px';
  /** Scroll happens inside the body area; toolbar stays above it. */
  @Input() maxEditorHeight = 'clamp(280px, calc(100vh - 300px), 720px)';

  get editorBodyStyle(): Record<string, string> {
    return {
      minHeight: this.minHeight,
      maxHeight: this.maxEditorHeight
    };
  }

  @ViewChild('editable') editableRef!: ElementRef<HTMLElement>;
  @ViewChild('imageInput') imageInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('foreColorInput') foreColorInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('highlightColorInput') highlightColorInputRef!: ElementRef<HTMLInputElement>;

  readonly imageSizeOptions: { id: NewsImageSize; label: string }[] = [
    { id: 'small', label: 'Small' },
    { id: 'medium', label: 'Medium' },
    { id: 'large', label: 'Large' },
    { id: 'full', label: 'Full width' }
  ];

  readonly imageTextWrapOptions: { id: NewsImageTextWrap; label: string; tooltip: string }[] = [
    { id: 'text-left', label: 'Text left', tooltip: 'Image on the right, text flows on the left' },
    { id: 'text-right', label: 'Text right', tooltip: 'Image on the left, text flows on the right' },
    { id: 'text-below', label: 'Text below', tooltip: 'Block image, text continues below' }
  ];

  readonly blockFormatOptions: { value: BlockFormat; label: string }[] = [
    { value: 'p', label: 'Paragraph' },
    { value: 'h1', label: 'Heading 1' },
    { value: 'h2', label: 'Heading 2' },
    { value: 'h3', label: 'Heading 3' },
    { value: 'blockquote', label: 'Quote' }
  ];

  readonly fontSizeOptions = ['12', '14', '16', '18', '20', '24', '28', '32'];

  readonly lineHeightOptions: { value: string; label: string }[] = [
    { value: '1', label: '1.0' },
    { value: '1.15', label: '1.15' },
    { value: '1.5', label: '1.5' },
    { value: '1.75', label: '1.75' },
    { value: '2', label: '2.0' }
  ];

  value = '';
  disabled = false;
  uploadingImage = false;
  sourceMode = false;
  sourceHtml = '';

  blockFormat: BlockFormat = 'p';
  fontSize = '18';
  lineHeight = '1.5';
  foreColor = '#212121';
  highlightColor = '#fff59d';

  selectedImage: HTMLImageElement | null = null;
  imageToolbarVisible = false;
  imageToolbarTop = 0;
  imageToolbarLeft = 0;

  selectedAnchor: HTMLAnchorElement | null = null;
  linkToolbarVisible = false;
  linkToolbarTop = 0;
  linkToolbarLeft = 0;
  selectedLinkHref = '';

  selectedVideoBlock: HTMLElement | null = null;
  videoToolbarVisible = false;
  videoToolbarTop = 0;
  videoToolbarLeft = 0;
  selectedVideoLabel = '';

  selectedImageSize: NewsImageSize = 'medium';
  selectedImageAlign: NewsImageAlign = 'left';
  selectedImageTextWrap: NewsImageTextWrap = 'text-below';
  imageAltText = '';

  private onChange: (v: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(
    private newsService: NewsService,
    private dialog: MatDialog,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    this.applyValueToEditor();
  }

  ngOnDestroy(): void {
    this.flushToModel();
    this.clearImageSelection(false);
    this.clearLinkSelection(false);
    this.clearVideoSelection(false);
  }

  writeValue(value: string | null): void {
    this.value = value || '';
    if (this.sourceMode) {
      this.sourceHtml = this.value;
    } else {
      this.applyValueToEditor();
    }
  }

  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(): void {
    this.syncValue();
  }

  onBlur(): void {
    setTimeout(() => {
      const active = document.activeElement;
      if (
        active?.closest('.rte-link-toolbar-portal') ||
        active?.closest('.rte-image-toolbar-portal') ||
        active?.closest('.rte-video-toolbar-portal')
      ) {
        return;
      }
      this.clearLinkSelection(false);
      this.clearVideoSelection(false);
      this.flushToModel();
      this.onTouched();
    }, 0);
  }

  /** Persists the current editor HTML into the parent ngModel binding. */
  flushToModel(): void {
    if (this.sourceMode) {
      this.value = this.newsService.normalizeBodyHtmlForStorage(this.sourceHtml);
      this.onChange(this.value);
      return;
    }
    const el = this.editableRef?.nativeElement;
    if (!el) {
      return;
    }
    const domHtml = this.newsService.normalizeBodyHtmlForStorage(el.innerHTML);
    if (!this.htmlHasText(domHtml) && this.htmlHasText(this.value)) {
      // Hidden tab or programmatic parent update: keep model value, do not wipe with empty DOM.
      return;
    }
    this.syncValue();
  }

  private htmlHasText(html: string | null | undefined): boolean {
    if (!html?.trim()) {
      return false;
    }
    return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/gi, ' ').trim().length > 0;
  }

  onSourceHtmlChange(html: string): void {
    this.sourceHtml = html;
    this.value = this.newsService.normalizeBodyHtmlForStorage(html);
    this.onChange(this.value);
  }

  onEditableScroll(): void {
    if (this.selectedImage) {
      this.updateImageToolbarPosition();
    }
    if (this.selectedAnchor) {
      this.updateLinkToolbarPosition();
    }
    if (this.selectedVideoBlock) {
      this.updateVideoToolbarPosition();
    }
  }

  onEditableClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const videoBlock = target.closest('.news-video-embed, .news-video-file');
    if (videoBlock && this.editableRef?.nativeElement.contains(videoBlock)) {
      event.preventDefault();
      this.selectVideoBlock(videoBlock as HTMLElement);
      return;
    }
    const anchor = target.closest('a');
    if (anchor && this.editableRef?.nativeElement.contains(anchor)) {
      event.preventDefault();
      this.selectLink(anchor as HTMLAnchorElement);
      return;
    }
    if (target.tagName === 'IMG') {
      event.preventDefault();
      this.selectImage(target as HTMLImageElement);
      return;
    }
    if (
      !target.closest('.rte-image-toolbar-portal') &&
      !target.closest('.rte-link-toolbar-portal') &&
      !target.closest('.rte-video-toolbar-portal')
    ) {
      this.clearImageSelection();
      this.clearLinkSelection();
      this.clearVideoSelection();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    if (this.linkToolbarVisible) {
      if (target.closest('.rte-link-toolbar-portal')) return;
      const clickedAnchor = target.closest('a');
      if (clickedAnchor && this.editableRef?.nativeElement.contains(clickedAnchor)) return;
      this.clearLinkSelection(false);
    }

    if (this.videoToolbarVisible) {
      if (target.closest('.rte-video-toolbar-portal')) return;
      const clickedVideo = target.closest('.news-video-embed, .news-video-file');
      if (clickedVideo && this.editableRef?.nativeElement.contains(clickedVideo)) return;
      this.clearVideoSelection(false);
    }

    if (!this.imageToolbarVisible) return;
    if (target.closest('.rte-image-toolbar-portal')) return;
    if (target.closest('.rte-editable') && target.tagName === 'IMG') return;
    if (this.editableRef?.nativeElement.contains(target)) {
      if (target.tagName !== 'IMG') {
        this.clearImageSelection();
      }
      return;
    }
    if (!this.editableRef?.nativeElement.contains(target)) {
      this.clearImageSelection();
    }
  }

  @HostListener('document:selectionchange')
  onSelectionChange(): void {
    if (!this.linkToolbarVisible || !this.selectedAnchor) return;
    requestAnimationFrame(() => {
      if (!this.linkToolbarVisible || !this.selectedAnchor) return;
      if (document.activeElement?.closest('.rte-link-toolbar-portal')) return;
      const anchor = this.findAnchorAtSelection();
      if (anchor === this.selectedAnchor) return;
      this.clearLinkSelection(false);
    });
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Escape') return;
    if (this.linkToolbarVisible) {
      this.clearLinkSelection();
      event.preventDefault();
    }
    if (this.imageToolbarVisible) {
      this.clearImageSelection();
    }
    if (this.videoToolbarVisible) {
      this.clearVideoSelection();
    }
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (this.selectedImage) {
      this.updateImageToolbarPosition();
    }
    if (this.selectedAnchor) {
      this.updateLinkToolbarPosition();
    }
    if (this.selectedVideoBlock) {
      this.updateVideoToolbarPosition();
    }
  }

  openSelectedLink(): void {
    const href = this.selectedAnchor?.getAttribute('href')?.trim() || '';
    if (!href) return;
    if (!isSafeHref(href)) {
      this.messageService.error('This link cannot be opened because it uses an unsafe protocol.');
      return;
    }
    window.open(href, '_blank', 'noopener,noreferrer');
  }

  editSelectedLink(): void {
    if (!this.selectedAnchor) return;
    this.openLinkEditorDialog(this.selectedAnchor);
  }

  removeSelectedLink(): void {
    const anchor = this.selectedAnchor;
    this.clearLinkSelection(false);
    this.removeAnchor(anchor);
  }

  exec(command: string, value?: string): void {
    if (this.disabled || this.sourceMode) return;
    this.editableRef?.nativeElement.focus();
    document.execCommand(command, false, value);
    this.syncValue();
  }

  applyBlockFormat(format: BlockFormat): void {
    this.blockFormat = format;
    this.exec('formatBlock', format);
  }

  applyFontSize(size: string): void {
    if (this.disabled || this.sourceMode) return;
    this.fontSize = size;
    this.editableRef?.nativeElement.focus();
    this.enableStyleWithCss();
    const applied = document.execCommand('fontSize', false, '7');
    if (applied) {
      this.replaceFontTagsWithSpan(size);
    } else {
      this.wrapSelectionStyle('font-size', `${size}px`);
    }
    this.syncValue();
  }

  applyForeColor(color: string): void {
    if (this.disabled || this.sourceMode) return;
    this.foreColor = color;
    this.editableRef?.nativeElement.focus();
    this.enableStyleWithCss();
    document.execCommand('foreColor', false, color);
    this.syncValue();
  }

  applyHighlightColor(color: string): void {
    if (this.disabled || this.sourceMode) return;
    this.highlightColor = color;
    this.editableRef?.nativeElement.focus();
    this.enableStyleWithCss();
    if (document.queryCommandSupported('hiliteColor')) {
      document.execCommand('hiliteColor', false, color);
    } else {
      document.execCommand('backColor', false, color);
    }
    this.syncValue();
  }

  openForeColorPicker(): void {
    if (this.disabled || this.sourceMode) return;
    this.foreColorInputRef?.nativeElement.click();
  }

  openHighlightColorPicker(): void {
    if (this.disabled || this.sourceMode) return;
    this.highlightColorInputRef?.nativeElement.click();
  }

  insertLink(): void {
    if (this.disabled || this.sourceMode) return;
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    el.focus();
    this.openLinkEditorDialog(this.findAnchorAtSelection());
  }

  insertQuote(): void {
    this.applyBlockFormat('blockquote');
  }

  toggleSourceMode(): void {
    if (this.disabled) return;
    if (!this.sourceMode) {
      this.sourceHtml = this.value;
      this.clearImageSelection(false);
      this.clearLinkSelection(false);
      this.clearVideoSelection(false);
      this.sourceMode = true;
      return;
    }
    this.value = this.newsService.normalizeBodyHtmlForStorage(this.sourceHtml);
    this.sourceMode = false;
    // *ngIf recreates #editable on the next render; apply HTML after the view exists.
    this.cdr.detectChanges();
    this.applyValueToEditor();
    this.onChange(this.value);
  }

  applyLineHeight(value: string): void {
    if (this.disabled || this.sourceMode) return;
    this.lineHeight = value;
    this.applyBlockStyle('lineHeight', value);
  }

  triggerImageUpload(): void {
    if (this.disabled || this.uploadingImage || this.sourceMode) return;
    this.imageInputRef?.nativeElement.click();
  }

  openVideoInsertDialog(): void {
    if (this.disabled || this.sourceMode) return;
    this.editableRef?.nativeElement.focus();
    this.openVideoDialog();
  }

  editSelectedVideo(): void {
    if (!this.selectedVideoBlock) return;
    const blockType = getVideoBlockType(this.selectedVideoBlock);
    const data: VideoInsertDialogData = {
      source: blockType === 'file' ? 'upload' : 'embed',
      embedUrl: this.selectedVideoBlock.getAttribute('data-video-url') || '',
      isEditing: true
    };
    this.openVideoDialog(data, this.selectedVideoBlock);
  }

  removeSelectedVideo(): void {
    if (!this.selectedVideoBlock) return;
    this.selectedVideoBlock.remove();
    this.clearVideoSelection();
    this.syncValue();
  }

  setVideoFullWidth(): void {
    if (!this.selectedVideoBlock) return;
    this.selectedVideoBlock.classList.toggle('news-video-full-width');
    this.syncValue();
    this.updateVideoToolbarPosition();
  }

  setVideoAlignCenter(): void {
    if (!this.selectedVideoBlock) return;
    this.selectedVideoBlock.classList.toggle('news-video-align-center');
    this.syncValue();
    this.updateVideoToolbarPosition();
  }

  isVideoFullWidth(): boolean {
    return this.selectedVideoBlock?.classList.contains('news-video-full-width') ?? false;
  }

  isVideoAlignCenter(): boolean {
    return this.selectedVideoBlock?.classList.contains('news-video-align-center') ?? false;
  }

  onImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.messageService.warn('Use the Video button to insert video files.');
      return;
    }

    this.uploadingImage = true;
    this.newsService.uploadBodyMedia(file).pipe(
      finalize(() => { this.uploadingImage = false; })
    ).subscribe({
      next: path => {
        if (path) {
          this.insertImageAtCursor(path, '');
        }
      },
      error: () => { /* parent may show toast via service later */ }
    });
  }

  setImageSize(size: NewsImageSize): void {
    if (!this.selectedImage) return;
    this.stripImageSizeStyles(this.selectedImage);
    SIZE_CLASSES.forEach(s => this.selectedImage!.classList.remove(`news-image-${s}`));
    this.selectedImage.classList.add(`news-image-${size}`);
    this.selectedImageSize = size;
    if (size === 'full') {
      this.applyTextWrapToImage(this.selectedImage, 'text-below', false);
    }
    this.syncValue();
    this.updateImageToolbarPosition();
  }

  setImageTextWrap(wrap: NewsImageTextWrap): void {
    if (!this.selectedImage) return;
    if ((wrap === 'text-right' || wrap === 'text-left') && this.selectedImageSize === 'full') {
      SIZE_CLASSES.forEach(s => this.selectedImage!.classList.remove(`news-image-${s}`));
      this.selectedImage.classList.add('news-image-large');
      this.selectedImageSize = 'large';
    }
    this.applyTextWrapToImage(this.selectedImage, wrap, true);
  }

  alignSelectedImage(align: NewsImageAlign): void {
    if (!this.selectedImage || this.selectedImageTextWrap !== 'text-below') return;
    ALIGN_CLASSES.forEach(a => this.selectedImage!.classList.remove(`news-image-align-${a}`));
    this.selectedImage.classList.add(`news-image-align-${align}`);
    this.stripImageLayoutInlineStyles(this.selectedImage);
    this.selectedImageAlign = align;
    this.syncValue();
    this.updateImageToolbarPosition();
  }

  applyImageAlt(): void {
    if (!this.selectedImage) return;
    this.selectedImage.alt = this.imageAltText;
    this.syncValue();
  }

  removeSelectedImage(): void {
    if (!this.selectedImage) return;
    this.selectedImage.remove();
    this.clearImageSelection();
    this.syncValue();
  }

  isSizeActive(size: NewsImageSize): boolean {
    return this.selectedImageSize === size;
  }

  isAlignActive(align: NewsImageAlign): boolean {
    return this.selectedImageAlign === align;
  }

  isTextWrapActive(wrap: NewsImageTextWrap): boolean {
    return this.selectedImageTextWrap === wrap;
  }

  isAlignDisabled(): boolean {
    return this.selectedImageTextWrap !== 'text-below';
  }

  private enableStyleWithCss(): void {
    try {
      document.execCommand('styleWithCSS', false, 'true');
    } catch {
      // ignored
    }
  }

  private replaceFontTagsWithSpan(size: string): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    el.querySelectorAll('font').forEach(font => {
      const span = document.createElement('span');
      span.style.fontSize = `${size}px`;
      span.innerHTML = font.innerHTML;
      font.replaceWith(span);
    });
  }

  private wrapSelectionStyle(cssProperty: string, cssValue: string): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return;

    const range = sel.getRangeAt(0);
    const span = document.createElement('span');
    span.style.setProperty(cssProperty, cssValue);
    try {
      range.surroundContents(span);
    } catch {
      const text = range.toString();
      if (!text) return;
      document.execCommand(
        'insertHTML',
        false,
        `<span style="${cssProperty}: ${cssValue}">${this.escapeHtml(text)}</span>`
      );
    }
  }

  private applyBlockStyle(property: 'lineHeight', value: string): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    el.focus();

    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return;

    let node: Node | null = selection.anchorNode;
    if (!node) return;
    if (node.nodeType === Node.TEXT_NODE) {
      node = node.parentElement;
    }

    const block = this.findBlockElement(node, el);
    if (block) {
      block.style[property] = value;
      this.syncValue();
      return;
    }

    document.execCommand('formatBlock', false, 'p');
    const afterFormat = window.getSelection()?.anchorNode;
    if (!afterFormat) return;
    const wrapped = this.findBlockElement(
      afterFormat.nodeType === Node.TEXT_NODE ? afterFormat.parentElement : afterFormat,
      el
    );
    if (wrapped) {
      wrapped.style[property] = value;
      this.syncValue();
    }
  }

  private findBlockElement(node: Node | null, root: HTMLElement): HTMLElement | null {
    let block = node as HTMLElement | null;
    while (block && block !== root) {
      if (BLOCK_TAGS.has(block.tagName)) {
        return block;
      }
      block = block.parentElement;
    }
    return null;
  }

  private insertImageAtCursor(relativePath: string, alt: string): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    el.focus();

    const escapedPath = relativePath.replace(/"/g, '&quot;');
    const html = `<img src="${escapedPath}" alt="${alt.replace(/"/g, '&quot;')}" class="news-image-medium news-image-block news-image-align-left" />`;

    if (!document.execCommand('insertHTML', false, html)) {
      el.innerHTML += html;
    }

    this.syncValue();
    this.applyValueToEditor();

    const inserted = el.querySelector(`img[src="${relativePath}"]`);
    if (inserted instanceof HTMLImageElement) {
      this.selectImage(inserted);
    }
  }

  private openVideoDialog(
    data: VideoInsertDialogData = {},
    existingBlock: HTMLElement | null = null
  ): void {
    this.dialog
      .open(VideoInsertDialogComponent, {
        width: '440px',
        maxWidth: '95vw',
        autoFocus: 'first-togglable',
        panelClass: 'rte-video-dialog-panel',
        data
      })
      .afterClosed()
      .subscribe((result: VideoInsertDialogResult | undefined) => {
        if (!result || result.action === 'cancel') return;
        const html = this.buildVideoHtmlFromResult(result);
        if (!html) {
          this.messageService.error('Could not insert video. Check the URL or file and try again.');
          return;
        }
        if (existingBlock) {
          this.replaceVideoBlock(existingBlock, html);
        } else {
          this.insertVideoHtmlAtCursor(html);
        }
      });
  }

  private buildVideoHtmlFromResult(result: VideoInsertDialogResult): string | null {
    if (result.action === 'embed' && result.embedUrl) {
      return this.newsService.insertVideoEmbed(result.embedUrl);
    }
    if (result.action === 'upload' && result.uploadedPath) {
      return this.newsService.buildUploadedVideoHtml(
        result.uploadedPath,
        result.mimeType || 'video/mp4'
      );
    }
    return null;
  }

  private insertVideoHtmlAtCursor(html: string): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    el.focus();

    if (!document.execCommand('insertHTML', false, html)) {
      el.innerHTML += html;
    }

    this.syncValue();
    this.applyValueToEditor();

    const blocks = el.querySelectorAll('.news-video-embed, .news-video-file');
    const inserted = blocks[blocks.length - 1];
    if (inserted instanceof HTMLElement) {
      this.selectVideoBlock(inserted);
    }
  }

  private replaceVideoBlock(existingBlock: HTMLElement, html: string): void {
    const temp = document.createElement('div');
    temp.innerHTML = html;
    const newBlock = temp.firstElementChild;
    if (!(newBlock instanceof HTMLElement)) return;
    existingBlock.replaceWith(newBlock);
    this.selectVideoBlock(newBlock);
    this.syncValue();
  }

  private selectVideoBlock(block: HTMLElement): void {
    if (!isNewsVideoBlock(block)) return;
    this.clearVideoSelection(false);
    this.clearImageSelection(false);
    this.clearLinkSelection(false);
    this.selectedVideoBlock = block;
    block.classList.add('rte-video-selected');
    this.selectedVideoLabel = this.readVideoBlockLabel(block);
    this.videoToolbarVisible = true;
    requestAnimationFrame(() => this.updateVideoToolbarPosition());
  }

  private clearVideoSelection(sync = true): void {
    if (this.selectedVideoBlock) {
      this.selectedVideoBlock.classList.remove('rte-video-selected');
    }
    this.selectedVideoBlock = null;
    this.videoToolbarVisible = false;
    this.selectedVideoLabel = '';
    if (sync) {
      this.syncValue();
    }
  }

  private readVideoBlockLabel(block: HTMLElement): string {
    const blockType = getVideoBlockType(block);
    if (blockType === 'embed') {
      return block.getAttribute('data-video-url') || 'Embedded video';
    }
    const src = block.getAttribute('data-video-src') ||
      block.querySelector('source')?.getAttribute('src') ||
      block.querySelector('video')?.getAttribute('src') ||
      'Uploaded video';
    const segments = src.split('/');
    return segments[segments.length - 1] || src;
  }

  private updateVideoToolbarPosition(): void {
    if (!this.selectedVideoBlock) return;

    const rect = this.selectedVideoBlock.getBoundingClientRect();
    const margin = 8;
    const viewportW = window.innerWidth;
    const viewportH = window.innerHeight;
    const toolbarWidth = Math.min(360, viewportW - 32);
    const toolbarHeight = 44;

    let top = rect.bottom + margin;
    if (top + toolbarHeight > viewportH - margin) {
      top = Math.max(margin, rect.top - toolbarHeight - margin);
    }

    let left = rect.left;
    if (left + toolbarWidth > viewportW - margin) {
      left = Math.max(margin, viewportW - toolbarWidth - margin);
    }
    if (left < margin) {
      left = margin;
    }

    this.videoToolbarTop = top;
    this.videoToolbarLeft = left;
  }

  private selectLink(anchor: HTMLAnchorElement): void {
    this.clearVideoSelection(false);
    this.clearLinkSelection(false);
    this.clearImageSelection(false);
    this.selectedAnchor = anchor;
    anchor.classList.add('rte-link-selected');
    this.selectedLinkHref = anchor.getAttribute('href') || '';
    this.linkToolbarVisible = true;
    requestAnimationFrame(() => this.updateLinkToolbarPosition());
  }

  private clearLinkSelection(sync = true): void {
    if (this.selectedAnchor) {
      this.selectedAnchor.classList.remove('rte-link-selected');
    }
    this.selectedAnchor = null;
    this.linkToolbarVisible = false;
    this.selectedLinkHref = '';
    if (sync) {
      this.syncValue();
    }
  }

  private updateLinkToolbarPosition(): void {
    if (!this.selectedAnchor) return;

    const linkRect = this.selectedAnchor.getBoundingClientRect();
    const margin = 8;
    const viewportW = window.innerWidth;
    const viewportH = window.innerHeight;
    const toolbarWidth = Math.min(420, viewportW - 32);
    const toolbarHeight = 48;

    let top = linkRect.bottom + margin;
    if (top + toolbarHeight > viewportH - margin) {
      top = Math.max(margin, linkRect.top - toolbarHeight - margin);
    }

    let left = linkRect.left;
    if (left + toolbarWidth > viewportW - margin) {
      left = Math.max(margin, viewportW - toolbarWidth - margin);
    }
    if (left < margin) {
      left = margin;
    }

    this.linkToolbarTop = top;
    this.linkToolbarLeft = left;
  }

  private openLinkEditorDialog(existingAnchor: HTMLAnchorElement | null): void {
    const selectedText = this.getSelectedText();
    const href = existingAnchor?.getAttribute('href') || '';
    const parsed = parseHrefToLinkForm(href);
    const relState = parseRelAttribute(existingAnchor?.getAttribute('rel'));
    const defaults = defaultTogglesForLinkType(parsed.linkType);

    const dialogData: LinkEditorDialogData = {
      linkType: parsed.linkType,
      value: parsed.value,
      openInNewTab: existingAnchor
        ? existingAnchor.getAttribute('target') === '_blank'
        : defaults.openInNewTab,
      noreferrer: existingAnchor ? relState.noreferrer : defaults.noreferrer,
      nofollow: relState.nofollow,
      sponsored: relState.sponsored,
      isEditing: !!existingAnchor,
      selectedText: existingAnchor?.textContent?.trim() || selectedText
    };

    this.dialog
      .open(LinkEditorDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        autoFocus: 'first-togglable',
        panelClass: 'rte-link-dialog-panel',
        data: dialogData
      })
      .afterClosed()
      .subscribe((result: LinkEditorDialogResult | undefined) => {
        if (!result || result.action === 'cancel') return;
        if (result.action === 'remove') {
          if (existingAnchor === this.selectedAnchor) {
            this.clearLinkSelection(false);
          }
          this.removeAnchor(existingAnchor);
          return;
        }
        if (result.action === 'save' && result.href) {
          this.applyLinkResult(result, existingAnchor, selectedText);
          if (existingAnchor && existingAnchor === this.selectedAnchor) {
            this.selectedLinkHref = existingAnchor.getAttribute('href') || result.href;
            this.updateLinkToolbarPosition();
          }
        }
      });
  }

  private selectImage(img: HTMLImageElement): void {
    this.clearVideoSelection(false);
    this.clearLinkSelection(false);
    this.clearImageSelection(false);
    this.selectedImage = img;
    img.classList.add('rte-img-selected');
    this.imageAltText = img.alt || '';
    this.selectedImageSize = this.readImageSize(img);
    this.selectedImageTextWrap = this.readImageTextWrap(img);
    this.selectedImageAlign = this.readImageAlign(img);
    this.imageToolbarVisible = true;
    requestAnimationFrame(() => this.updateImageToolbarPosition());
  }

  private clearImageSelection(sync = true): void {
    if (this.selectedImage) {
      this.selectedImage.classList.remove('rte-img-selected');
    }
    this.selectedImage = null;
    this.imageToolbarVisible = false;
    this.imageAltText = '';
    if (sync) {
      this.syncValue();
    }
  }

  private readImageSize(img: HTMLImageElement): NewsImageSize {
    for (const size of SIZE_CLASSES) {
      if (img.classList.contains(`news-image-${size}`)) {
        return size;
      }
    }
    return 'medium';
  }

  private readImageAlign(img: HTMLImageElement): NewsImageAlign {
    for (const align of ALIGN_CLASSES) {
      if (img.classList.contains(`news-image-align-${align}`)) {
        return align;
      }
    }
    return 'left';
  }

  private readImageTextWrap(img: HTMLImageElement): NewsImageTextWrap {
    if (img.classList.contains('news-image-float-left')) return 'text-right';
    if (img.classList.contains('news-image-float-right')) return 'text-left';
    return 'text-below';
  }

  private applyTextWrapToImage(img: HTMLImageElement, wrap: NewsImageTextWrap, sync: boolean): void {
    WRAP_CLASSES.forEach(c => img.classList.remove(c));
    const wrapClass =
      wrap === 'text-right' ? 'news-image-float-left'
        : wrap === 'text-left' ? 'news-image-float-right'
          : 'news-image-block';
    img.classList.add(wrapClass);
    this.stripImageLayoutInlineStyles(img);

    if (wrap !== 'text-below') {
      ALIGN_CLASSES.forEach(a => img.classList.remove(`news-image-align-${a}`));
      this.selectedImageAlign = 'left';
    } else if (!ALIGN_CLASSES.some(a => img.classList.contains(`news-image-align-${a}`))) {
      img.classList.add('news-image-align-left');
      this.selectedImageAlign = 'left';
    }

    this.selectedImageTextWrap = wrap;
    if (sync) {
      this.syncValue();
    }
    this.updateImageToolbarPosition();
  }

  private stripImageLayoutInlineStyles(img: HTMLImageElement): void {
    img.style.float = '';
    img.style.display = '';
    img.style.margin = '';
    img.style.marginLeft = '';
    img.style.marginRight = '';
    img.style.marginTop = '';
    img.style.marginBottom = '';
  }

  private stripImageSizeStyles(img: HTMLImageElement): void {
    img.style.width = '';
    img.style.height = '';
    img.removeAttribute('width');
    img.removeAttribute('height');
  }

  private updateImageToolbarPosition(): void {
    if (!this.selectedImage) return;

    const imgRect = this.selectedImage.getBoundingClientRect();
    const margin = 8;
    const viewportW = window.innerWidth;
    const viewportH = window.innerHeight;
    const toolbarWidth = Math.min(480, Math.max(420, viewportW - 32));
    const toolbarHeight = 210;

    let top = imgRect.bottom + margin;
    if (top + toolbarHeight > viewportH - margin) {
      top = Math.max(margin, imgRect.top - toolbarHeight - margin);
    }

    let left = imgRect.left;
    if (left + toolbarWidth > viewportW - margin) {
      left = Math.max(margin, viewportW - toolbarWidth - margin);
    }
    if (left < margin) {
      left = margin;
    }

    this.imageToolbarTop = top;
    this.imageToolbarLeft = left;
  }

  private applyValueToEditor(): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    const displayHtml = this.newsService.resolveBodyHtmlForDisplay(this.value);
    if (el.innerHTML !== displayHtml) {
      el.innerHTML = displayHtml;
    }
  }

  private syncValue(): void {
    const el = this.editableRef?.nativeElement;
    if (!el) return;
    this.selectedImage?.classList.remove('rte-img-selected');
    this.selectedAnchor?.classList.remove('rte-link-selected');
    this.selectedVideoBlock?.classList.remove('rte-video-selected');
    this.value = this.newsService.normalizeBodyHtmlForStorage(el.innerHTML);
    this.selectedImage?.classList.add('rte-img-selected');
    this.selectedAnchor?.classList.add('rte-link-selected');
    this.selectedVideoBlock?.classList.add('rte-video-selected');
    this.onChange(this.value);
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  private getSelectedText(): string {
    const sel = window.getSelection();
    if (!sel || sel.isCollapsed) return '';
    return sel.toString();
  }

  private findAnchorAtSelection(): HTMLAnchorElement | null {
    const root = this.editableRef?.nativeElement;
    if (!root) return null;

    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) return null;

    const nodes = [sel.anchorNode, sel.focusNode];
    for (const start of nodes) {
      let node: Node | null = start;
      while (node && node !== root) {
        if (node instanceof HTMLAnchorElement) {
          return node;
        }
        if (node instanceof HTMLElement) {
          const closest = node.closest('a');
          if (closest instanceof HTMLAnchorElement && root.contains(closest)) {
            return closest;
          }
        }
        node = node.parentNode;
      }
    }
    return null;
  }

  private applyLinkResult(
    result: LinkEditorDialogResult,
    existingAnchor: HTMLAnchorElement | null,
    selectedText: string
  ): void {
    const el = this.editableRef?.nativeElement;
    if (!el || !result.href) return;
    el.focus();

    if (existingAnchor) {
      this.updateAnchorAttributes(existingAnchor, result);
      if (existingAnchor === this.selectedAnchor) {
        this.selectedLinkHref = existingAnchor.getAttribute('href') || result.href;
        this.updateLinkToolbarPosition();
      }
      this.syncValue();
      return;
    }

    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) return;
    const range = sel.getRangeAt(0);
    const anchor = document.createElement('a');
    this.updateAnchorAttributes(anchor, result);

    if (!range.collapsed) {
      try {
        const fragment = range.extractContents();
        anchor.appendChild(fragment);
        range.insertNode(anchor);
      } catch {
        anchor.textContent = selectedText || result.href;
        document.execCommand('insertHTML', false, anchor.outerHTML);
      }
    } else {
      anchor.textContent = selectedText?.trim() || result.href;
      range.insertNode(anchor);
    }

    sel.removeAllRanges();
    const after = document.createRange();
    after.setStartAfter(anchor);
    after.collapse(true);
    sel.addRange(after);
    this.syncValue();
  }

  private updateAnchorAttributes(anchor: HTMLAnchorElement, result: LinkEditorDialogResult): void {
    anchor.setAttribute('href', result.href!);
    if (result.target) {
      anchor.target = result.target;
    } else {
      anchor.removeAttribute('target');
    }
    if (result.rel) {
      anchor.setAttribute('rel', result.rel);
    } else {
      anchor.removeAttribute('rel');
    }
  }

  private removeAnchor(anchor: HTMLAnchorElement | null): void {
    if (!anchor) return;
    const parent = anchor.parentNode;
    if (!parent) return;
    while (anchor.firstChild) {
      parent.insertBefore(anchor.firstChild, anchor);
    }
    parent.removeChild(anchor);
    this.syncValue();
  }
}
