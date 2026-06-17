import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { NewsSpanishContent, NewsTranslationResult } from '../models/news.models';

interface TranslateApiResponse {
  titleEn: string;
  summaryEn: string;
  bodyEn: string;
}

@Injectable({
  providedIn: 'root'
})
export class NewsTranslationService {

  constructor(private http: BaseHttpService) {}

  translateNewsContent(content: NewsSpanishContent): Observable<NewsTranslationResult> {
    return this.http.post<TranslateApiResponse>('/news/translate', {
      titleEs: content.titleEs || '',
      summaryEs: content.summaryEs || '',
      bodyEs: content.bodyEs || ''
    }).pipe(
      map(resp => ({
        titleEn: resp.titleEn ?? '',
        summaryEn: resp.summaryEn ?? '',
        bodyEn: resp.bodyEn ?? ''
      }))
    );
  }
}
