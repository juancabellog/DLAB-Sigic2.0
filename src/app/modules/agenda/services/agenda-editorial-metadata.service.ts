import { Injectable } from '@angular/core';
import { AgendaEditorialMetadata, TRANSLATION_STATUS, TranslationStatus } from '../models/agenda.models';

const STORAGE_KEY = 'sisgic_agenda_editorial_metadata';

@Injectable({
  providedIn: 'root'
})
export class AgendaEditorialMetadataService {

  private loadAll(): Record<string, AgendaEditorialMetadata> {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }

  private saveAll(data: Record<string, AgendaEditorialMetadata>): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }

  get(id: number): AgendaEditorialMetadata {
    const all = this.loadAll();
    return all[String(id)] || this.defaultMetadata();
  }

  save(id: number, metadata: AgendaEditorialMetadata): void {
    const all = this.loadAll();
    all[String(id)] = metadata;
    this.saveAll(all);
  }

  remove(id: number): void {
    const all = this.loadAll();
    delete all[String(id)];
    this.saveAll(all);
  }

  defaultMetadata(translationStatus: TranslationStatus = TRANSLATION_STATUS.NO_TRANSLATION): AgendaEditorialMetadata {
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
