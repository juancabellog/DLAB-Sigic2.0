import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import {
  NewsEnglishContent,
  NewsSpanishContent,
  NewsTranslationDirection,
  NewsTranslationResult
} from '../models/news.models';

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
export class NewsTranslationService {

  constructor(private http: BaseHttpService) {}

  translateNewsContent(
    direction: NewsTranslationDirection,
    content: NewsSpanishContent | NewsEnglishContent
  ): Observable<NewsTranslationResult> {
    if (direction === 'en_to_es') {
      const en = content as NewsEnglishContent;
      return this.http.post<TranslateApiResponse>('/news/translate', {
        direction,
        titleEn: en.titleEn || '',
        summaryEn: en.summaryEn || '',
        bodyEn: en.bodyEn || ''
      }).pipe(
        map(resp => ({
          titleEs: resp.titleEs ?? '',
          summaryEs: resp.summaryEs ?? '',
          bodyEs: resp.bodyEs ?? ''
        }))
      );
    }

    const es = content as NewsSpanishContent;
    return this.http.post<TranslateApiResponse>('/news/translate', {
      direction: 'es_to_en',
      titleEs: es.titleEs || '',
      summaryEs: es.summaryEs || '',
      bodyEs: es.bodyEs || ''
    }).pipe(
      map(resp => ({
        titleEn: resp.titleEn ?? '',
        summaryEn: resp.summaryEn ?? '',
        bodyEn: resp.bodyEn ?? ''
      }))
    );
  }
}
