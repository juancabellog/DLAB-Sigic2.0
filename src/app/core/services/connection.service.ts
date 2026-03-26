import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, timer, interval } from 'rxjs';
import { catchError, switchMap, tap, map } from 'rxjs/operators';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ConnectionService {
  private readonly API_URL = environment.apiUrl;
  private readonly HEALTH_CHECK_INTERVAL = 30000; // 30 segundos
  private readonly TOKEN_VALIDATION_INTERVAL = 300000; // 5 minutos
  private readonly CONNECTION_TIMEOUT = 5000; // 5 segundos
  private readonly CLIENT_VERSION = environment.appVersion || 'dev';
  private versionMismatchHandled = false;

  private connectionStatusSubject = new BehaviorSubject<boolean>(true);
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  private healthCheckTimer?: any;
  private tokenValidationTimer?: any;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {
    this.startHealthCheck();
    this.startTokenValidation();
  }

  /**
   * Inicia el monitoreo de conexión
   */
  private startHealthCheck(): void {
    // Verificar conexión inmediatamente solo si hay token
    const token = this.authService.getToken();
    if (token) {
      this.checkConnection().subscribe();
    } else {
      // Si no hay token, marcar como desconectado inicialmente
      this.connectionStatusSubject.next(false);
    }

    // Configurar verificación periódica
    this.healthCheckTimer = setInterval(() => {
      this.checkConnection().subscribe();
    }, this.HEALTH_CHECK_INTERVAL);
  }

  /**
   * Inicia la validación periódica del token
   */
  private startTokenValidation(): void {
    // Validar token inmediatamente
    this.validateToken().subscribe();

    // Configurar validación periódica
    this.tokenValidationTimer = setInterval(() => {
      this.validateToken().subscribe();
    }, this.TOKEN_VALIDATION_INTERVAL);
  }

  /**
   * Verifica la conexión con el backend
   */
  checkConnection(): Observable<boolean> {
    const token = this.authService.getToken();
    
    // Si no hay token, marcar como desconectado
    // Pero si antes estaba conectado y ahora no hay token, podría ser un logout
    if (!token) {
      // Solo marcar como desconectado si realmente no hay token
      // No cambiar el estado si ya estaba desconectado
      if (this.connectionStatusSubject.value) {
        this.connectionStatusSubject.next(false);
      }
      return of(false);
    }

    // Hacer petición de health check
    return this.http.get(`${this.API_URL}/health`).pipe(
      tap(() => {
        // Conexión exitosa
        const wasDisconnected = !this.connectionStatusSubject.value;
        this.connectionStatusSubject.next(true);

        // Validar token SIEMPRE que la conexión esté OK, para:
        // - Detectar expiración de sesión
        // - Comparar versión cliente/servidor en cada health check
        this.validateToken().subscribe({
          next: (isValid) => {
            if (wasDisconnected && isValid) {
              this.snackBar.open(
                '✅ Connection restored with server',
                'Close',
                {
                  duration: 3000,
                  panelClass: ['success-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                }
              );
            } else if (!isValid) {
              // Token inválido, el usuario será redirigido al login
              this.snackBar.open(
                '🔒 Your session has expired. You will be redirected to login.',
                'OK',
                {
                  duration: 4000,
                  panelClass: ['security-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                }
              );
            }
          },
          error: () => {
            // Error al validar token, redirigir al login
            this.snackBar.open(
              '🔒 Error validating session. You will be redirected to login.',
              'OK',
              {
                duration: 4000,
                panelClass: ['security-snackbar'],
                horizontalPosition: 'center',
                verticalPosition: 'top'
              }
            );
          }
        });
      }),
      catchError((error) => {
        // Conexión fallida
        this.connectionStatusSubject.next(false);
        
        // Si es error 401, el token expiró
        if (error.status === 401) {
          this.handleTokenExpired();
        } else {
          // Otros errores de conexión
          this.handleConnectionLost();
        }
        
        return of(false);
      }),
      // Mapear la respuesta a boolean
      map(() => true)
    );
  }

  /**
   * Verifica si el token es válido
   */
  validateToken(): Observable<boolean> {
    const token = this.authService.getToken();
    
    if (!token) {
      return of(false);
    }

    // Primero verificar si el token no ha expirado localmente
    if (!this.authService.isAuthenticated()) {
      this.handleTokenExpired();
      return of(false);
    }

    return this.http.get<{ status?: string; version?: string }>(`${this.API_URL}/auth/validate`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }).pipe(
      tap((resp) => {
        this.connectionStatusSubject.next(true);

        // Chequear versión del backend: si cambia, informar al usuario para que recargue
        if (!this.versionMismatchHandled && resp && resp.version && resp.version !== this.CLIENT_VERSION) {
          this.versionMismatchHandled = true;
          this.snackBar.open(
            'A new version of the platform is available. Please refresh your browser to update.',
            undefined,
            {
              duration: undefined, // mensaje permanente hasta que el usuario recargue
              panelClass: ['info-snackbar'],
              horizontalPosition: 'center',
              verticalPosition: 'top'
            }
          );
        }
      }),
      catchError((error) => {
        if (error.status === 401) {
          this.handleTokenExpired();
        } else if (error.status === 0) {
          // No hacer logout por error de red, solo marcar como desconectado
          this.connectionStatusSubject.next(false);
        } else {
          this.connectionStatusSubject.next(false);
        }
        
        return of(false);
      }),
      // Mapear la respuesta a boolean
      map(() => true)
    );
  }

  /**
   * Maneja cuando el token ha expirado
   */
  private handleTokenExpired(): void {
    // Mostrar notificación de seguridad
    this.snackBar.open(
      '🔒 For security, your session has expired. You will be redirected to login.',
      'OK',
      {
        duration: 5000,
        panelClass: ['security-snackbar'],
        horizontalPosition: 'center',
        verticalPosition: 'top'
      }
    );
    
    this.authService.logout();
    this.connectionStatusSubject.next(false);
  }

  /**
   * Maneja cuando se pierde la conexión
   */
  private handleConnectionLost(): void {
    this.connectionStatusSubject.next(false);
    
    // Mostrar notificación de conexión perdida
    this.snackBar.open(
      '⚠️ Conexión perdida con el servidor. Verificando...',
      'Reintentar',
      {
        duration: 4000,
        panelClass: ['warning-snackbar'],
        horizontalPosition: 'center',
        verticalPosition: 'top'
      }
    );
  }

  /**
   * Obtiene el estado actual de la conexión
   */
  isConnected(): boolean {
    return this.connectionStatusSubject.value;
  }

  /**
   * Fuerza una verificación de conexión
   */
  forceConnectionCheck(): Observable<boolean> {
    return this.checkConnection();
  }

  /**
   * Detiene el monitoreo de conexión
   */
  stopHealthCheck(): void {
    if (this.healthCheckTimer) {
      clearInterval(this.healthCheckTimer);
      this.healthCheckTimer = undefined;
    }
  }

  /**
   * Detiene la validación del token
   */
  stopTokenValidation(): void {
    if (this.tokenValidationTimer) {
      clearInterval(this.tokenValidationTimer);
      this.tokenValidationTimer = undefined;
    }
  }

  /**
   * Reinicia el monitoreo de conexión
   */
  restartHealthCheck(): void {
    this.stopHealthCheck();
    this.startHealthCheck();
  }

  /**
   * Reinicia la validación del token
   */
  restartTokenValidation(): void {
    this.stopTokenValidation();
    this.startTokenValidation();
  }

  /**
   * Detiene todos los monitoreos
   */
  stopAllMonitoring(): void {
    this.stopHealthCheck();
    this.stopTokenValidation();
  }
}
