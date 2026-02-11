import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ViewMode = 'list' | 'card';

@Injectable({
  providedIn: 'root'
})
export class ViewModeService {
  private readonly STORAGE_KEY = 'scientific_products_view_mode';
  private viewModeSubject = new BehaviorSubject<ViewMode>('card');

  constructor() {
    // Cargar preferencia guardada al inicializar el servicio
    this.loadViewMode();
  }

  /**
   * Obtiene el modo de vista actual como Observable
   */
  getViewMode(): Observable<ViewMode> {
    return this.viewModeSubject.asObservable();
  }

  /**
   * Obtiene el modo de vista actual como valor
   */
  getCurrentViewMode(): ViewMode {
    return this.viewModeSubject.value;
  }

  /**
   * Establece el modo de vista y lo persiste
   */
  setViewMode(mode: ViewMode): void {
    this.viewModeSubject.next(mode);
    this.saveViewMode(mode);
  }

  /**
   * Carga la preferencia de vista desde localStorage
   */
  private loadViewMode(): void {
    try {
      const savedMode = localStorage.getItem(this.STORAGE_KEY) as ViewMode;
      if (savedMode && (savedMode === 'list' || savedMode === 'card')) {
        this.viewModeSubject.next(savedMode);
      }
    } catch (error) {
      console.warn('Error loading view mode from localStorage:', error);
    }
  }

  /**
   * Guarda la preferencia de vista en localStorage
   */
  private saveViewMode(mode: ViewMode): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, mode);
    } catch (error) {
      console.warn('Error saving view mode to localStorage:', error);
    }
  }

  /**
   * Resetea la preferencia a 'card' por defecto
   */
  resetViewMode(): void {
    this.setViewMode('card');
  }
}












