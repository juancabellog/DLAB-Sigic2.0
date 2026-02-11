import { Injectable } from '@angular/core';

export interface ListState {
  searchTerm: string;
  basalOnly: boolean;
  pendingOnly: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ListStateService {
  private states: Map<string, ListState> = new Map();

  /**
   * Guarda el estado de los filtros para un tipo de lista específico
   * @param listType Tipo de lista (ej: 'publications', 'thesis-students', etc.)
   * @param state Estado a guardar
   */
  saveState(listType: string, state: Partial<ListState>): void {
    const currentState = this.getState(listType);
    const newState: ListState = {
      searchTerm: state.searchTerm !== undefined ? state.searchTerm : currentState.searchTerm,
      basalOnly: state.basalOnly !== undefined ? state.basalOnly : currentState.basalOnly,
      pendingOnly: state.pendingOnly !== undefined ? state.pendingOnly : currentState.pendingOnly
    };
    this.states.set(listType, newState);
  }

  /**
   * Obtiene el estado guardado para un tipo de lista
   * @param listType Tipo de lista
   * @returns Estado guardado o estado por defecto
   */
  getState(listType: string): ListState {
    return this.states.get(listType) || {
      searchTerm: '',
      basalOnly: false,
      pendingOnly: false
    };
  }

  /**
   * Limpia el estado guardado para un tipo de lista
   * @param listType Tipo de lista
   */
  clearState(listType: string): void {
    this.states.delete(listType);
  }
}


