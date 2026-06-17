import { Injectable } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { NewsCategory } from '../../news/models/news.models';
import { isPendingTaxonomyItem } from '../../news/utils/news-taxonomy.util';
import { AgendaEvent } from '../models/agenda.models';
import { AgendaService } from './agenda.service';

@Injectable({
  providedIn: 'root'
})
export class AgendaSaveService {

  constructor(private agendaService: AgendaService) {}

  /**
   * Creates pending categories in the backend, then saves the Agenda event.
   * Aborts the whole operation if any taxonomy creation fails.
   */
  saveAgendaEvent(item: AgendaEvent, agendaId?: number | null): Observable<AgendaEvent> {
    return this.resolvePendingCategories(item.categories || []).pipe(
      switchMap(categories => {
        const resolved = { ...item, categories };
        const save$ = agendaId
          ? this.agendaService.updateAgenda(agendaId, resolved)
          : this.agendaService.createAgenda(resolved);
        return save$.pipe(
          catchError(() => throwError(() => new Error('Could not save agenda event.')))
        );
      })
    );
  }

  private resolvePendingCategories(categories: NewsCategory[]): Observable<NewsCategory[]> {
    const pending = categories.filter(isPendingTaxonomyItem);
    if (!pending.length) {
      return of(categories);
    }

    return forkJoin(
      pending.map(cat =>
        this.agendaService.createCategory(cat.label).pipe(
          map(created => {
            if (!created?.id) {
              throw new Error(`Could not create category "${cat.label}".`);
            }
            return { pending: cat, created: { ...created, isPending: false } };
          }),
          catchError(() =>
            throwError(() => new Error(`Could not create category "${cat.label}".`))
          )
        )
      )
    ).pipe(
      map(replacements => this.replacePendingItems(categories, replacements))
    );
  }

  private replacePendingItems<T extends { id: string; label: string; isPending?: boolean }>(
    items: T[],
    replacements: { pending: T; created: T }[]
  ): T[] {
    const byPendingId = new Map(replacements.map(r => [r.pending.id, r.created]));
    return items.map(item => {
      const replacement = byPendingId.get(item.id);
      if (replacement) {
        return { ...replacement, isPending: false };
      }
      return item;
    });
  }
}
