import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import {
  AgendaEnglishContent,
  AgendaSpanishContent,
  AgendaTranslationDirection,
  AgendaTranslationResult
} from '../models/agenda.models';

interface TranslateApiResponse {
  titleEn?: string;
  summaryEn?: string;
  bodyEn?: string;
  titleEs?: string;
  summaryEs?: string;
  bodyEs?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AgendaTranslationService {

  constructor(private http: BaseHttpService) {}

  translateAgendaContent(
    direction: AgendaTranslationDirection,
    content: AgendaSpanishContent | AgendaEnglishContent
  ): Observable<AgendaTranslationResult> {
    if (direction === 'en_to_es') {
      const en = content as AgendaEnglishContent;
      return this.http.post<TranslateApiResponse>('/agenda/translate', {
        direction,
        titleEn: en.titleEn || '',
        summaryEn: '',
        bodyEn: en.descriptionEn || ''
      }).pipe(
        map(resp => ({
          titleEs: resp.titleEs ?? '',
          descriptionEs: resp.bodyEs ?? ''
        }))
      );
    }

    const es = content as AgendaSpanishContent;
    return this.http.post<TranslateApiResponse>('/agenda/translate', {
      direction: 'es_to_en',
      titleEs: es.titleEs || '',
      summaryEs: '',
      bodyEs: es.descriptionEs || ''
    }).pipe(
      map(resp => ({
        titleEn: resp.titleEn ?? '',
        descriptionEn: resp.bodyEn ?? ''
      }))
    );
  }
}
