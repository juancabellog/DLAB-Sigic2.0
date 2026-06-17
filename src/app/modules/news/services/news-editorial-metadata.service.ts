import { Injectable } from '@angular/core';
import { NewsEditorialMetadata, TRANSLATION_STATUS, TranslationStatus } from '../models/news.models';

const STORAGE_KEY = 'sisgic_news_editorial_metadata';

@Injectable({
  providedIn: 'root'
})
export class NewsEditorialMetadataService {

  private loadAll(): Record<string, NewsEditorialMetadata> {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }

  private saveAll(data: Record<string, NewsEditorialMetadata>): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }

  get(id: number): NewsEditorialMetadata {
    const all = this.loadAll();
    return all[String(id)] || this.defaultMetadata();
  }

  save(id: number, metadata: NewsEditorialMetadata): void {
    const all = this.loadAll();
    all[String(id)] = metadata;
    this.saveAll(all);
  }

  remove(id: number): void {
    const all = this.loadAll();
    delete all[String(id)];
    this.saveAll(all);
  }

  defaultMetadata(translationStatus: TranslationStatus = TRANSLATION_STATUS.NO_TRANSLATION): NewsEditorialMetadata {
    return {
      translationStatus,
      mainImageAltEs: '',
      mainImageAltEn: '',
      slug: '',
      metaTitle: '',
      metaDescription: '',
      ogTitle: '',
      ogDescription: '',
      ogImageUrl: '',
      publicUrl: '',
      translationValidatedAt: null,
      validatedSpanishSnapshot: undefined
    };
  }
}
