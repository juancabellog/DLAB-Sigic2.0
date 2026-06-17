import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { AgendaSpanishContent, AgendaTranslationResult } from '../models/agenda.models';

interface TranslateApiResponse {
  titleEn: string;
  summaryEn: string;
  bodyEn: string;
}

@Injectable({
  providedIn: 'root'
})
export class AgendaTranslationService {

  constructor(private http: BaseHttpService) {}

  translateAgendaContent(content: AgendaSpanishContent): Observable<AgendaTranslationResult> {
    return this.http.post<TranslateApiResponse>('/agenda/translate', {
      titleEs: content.titleEs || '',
      summaryEs: content.summaryEs || '',
      bodyEs: content.descriptionEs || ''
    }).pipe(
      map(resp => ({
        titleEn: resp.titleEn ?? '',
        summaryEn: resp.summaryEn ?? '',
        descriptionEn: resp.bodyEn ?? ''
      }))
    );
  }
}
