import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BaseHttpService {
  private readonly baseUrl = environment.apiUrl || 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  /**
   * Realiza una petición GET
   */
  get<T>(endpoint: string, params?: any): Observable<T> {
    const httpParams = this.buildHttpParams(params);
    return this.http.get<T>(`${this.baseUrl}${endpoint}`, { params: httpParams })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Realiza una petición POST
   */
  post<T>(endpoint: string, data: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Realiza una petición PUT
   */
  put<T>(endpoint: string, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Realiza una petición PATCH
   */
  patch<T>(endpoint: string, data: any): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Realiza una petición DELETE
   */
  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${endpoint}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Sube un archivo (multipart/form-data)
   */
  uploadFile<T>(endpoint: string, file: File, fieldName: string = 'file'): Observable<T> {
    const formData = new FormData();
    formData.append(fieldName, file);
    
    return this.http.post<T>(`${this.baseUrl}${endpoint}`, formData).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Realiza una petición GET con paginación
   */
  getPaginated<T>(endpoint: string, params?: any): Observable<any> {
    const httpParams = this.buildHttpParams(params);
    return this.http.get<any>(`${this.baseUrl}${endpoint}`, { params: httpParams })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Realiza una petición GET que devuelve un archivo (Blob)
   */
  getFile(endpoint: string, params?: any): Observable<Blob> {
    const httpParams = this.buildHttpParams(params);
    const token = this.getToken();
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get(`${this.baseUrl}${endpoint}`, {
      params: httpParams,
      headers,
      responseType: 'blob'
    }).pipe(catchError(this.handleError));
  }

  /**
   * Construye los parámetros HTTP
   */
  private buildHttpParams(params: any): HttpParams {
    let httpParams = new HttpParams();
    
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
          if (Array.isArray(params[key])) {
            params[key].forEach((value: any) => {
              httpParams = httpParams.append(key, value.toString());
            });
          } else {
            httpParams = httpParams.append(key, params[key].toString());
          }
        }
      });
    }
    
    return httpParams;
  }

  /**
   * Obtiene los headers por defecto
   */
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  /**
   * Maneja los errores HTTP
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error has occurred';

    if (error.error instanceof ErrorEvent) {
      // Error del lado del cliente
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Error del lado del servidor
      switch (error.status) {
        case 400:
          errorMessage = 'Bad request. Please verify the data sent.';
          break;
        case 401:
          errorMessage = 'Unauthorized. Please log in again.';
          // Redirigir al login si es necesario
          localStorage.removeItem('token');
          window.location.href = '/login';
          break;
        case 403:
          errorMessage = 'You do not have permission to perform this action.';
          break;
        case 404:
          errorMessage = 'Resource not found.';
          break;
        case 409:
          errorMessage = 'Conflict: The resource already exists or is in use.';
          break;
        case 422:
          errorMessage = 'Invalid input data.';
          break;
        case 500:
          errorMessage = 'Internal server error. Please try again later.';
          break;
        default:
          errorMessage = `Server error: ${error.status} - ${error.message}`;
      }
    }

    console.error('Error HTTP:', error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Construye la URL completa para un endpoint
   */
  buildUrl(endpoint: string): string {
    return `${this.baseUrl}${endpoint}`;
  }

  /**
   * Verifica si hay un token válido
   */
  isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    return !!token;
  }

  /**
   * Obtiene el token actual
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Establece el token
   */
  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  /**
   * Elimina el token
   */
  removeToken(): void {
    localStorage.removeItem('token');
  }
}
