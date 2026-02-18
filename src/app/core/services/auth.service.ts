import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, switchMap } from 'rxjs';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  username: string;
  email: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'current_user';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loadUserFromStorage();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => {
          this.setToken(response.accessToken);
          const user: User = {
            id: 0, // Backend doesn't return ID in login response
            username: response.username,
            email: response.email,
            firstName: '',
            lastName: '',
            roles: response.roles
          };
          this.setCurrentUser(user);
          // Después de un login exitoso, marcar la conexión como activa
          // El ConnectionService verificará la conexión en el próximo health check
          // pero podemos forzar una actualización inmediata del estado
          this.updateConnectionStatusAfterLogin();
        })
      );
  }

  /**
   * Actualiza el estado de conexión después de un login exitoso
   * Esto asegura que el indicador de conexión muestre el estado correcto
   * Nota: El componente de login debería llamar a ConnectionService.forceConnectionCheck()
   * después del login exitoso para actualizar el estado inmediatamente
   */
  private updateConnectionStatusAfterLogin(): void {
    // El health check periódico actualizará el estado automáticamente
    // pero puede tomar hasta 30 segundos. El componente de login debería
    // llamar a ConnectionService.forceConnectionCheck() para una actualización inmediata
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.API_URL}/register`, userData);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    
    // Mostrar notificación de logout
    this.snackBar.open(
      '👋 Session closed successfully',
      'Close',
      {
        duration: 3000,
        panelClass: ['info-snackbar'],
        horizontalPosition: 'center',
        verticalPosition: 'top'
      }
    );
    
    this.router.navigate(['/login']);
  }

  /**
   * Limpia completamente el localStorage (útil para testing)
   */
  clearStorage(): void {
    localStorage.clear();
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    // Verificar si el token no ha expirado (básico)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Date.now() / 1000;
      return payload.exp > currentTime;
    } catch (error) {
      // Si hay error al decodificar, el token es inválido
      return false;
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Verifica si el usuario tiene un rol específico
   * El usuario puede tener múltiples roles, así que verifica si el rol está en el array
   */
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    if (!user || !user.roles || !Array.isArray(user.roles)) {
      return false;
    }
    return user.roles.includes(role);
  }

  /**
   * Verifica si el usuario es administrador
   * Un usuario puede ser admin Y tener otros roles
   */
  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  /**
   * Verifica si el usuario es coordinador
   * Un usuario puede ser coordinador Y tener otros roles (ej: admin + coordinator)
   */
  isCoordinator(): boolean {
    return this.hasRole('ROLE_COORDINATOR');
  }

  /**
   * Verifica si el usuario puede acceder al módulo de análisis
   * Acceso permitido si tiene ROLE_ADMIN O ROLE_COORDINATOR (o ambos)
   */
  canAccessAnalysis(): boolean {
    const canAccess = this.isAdmin() || this.isCoordinator();
    if (canAccess) {
      const user = this.getCurrentUser();
      //console.log('Usuario puede acceder a análisis. Roles:', user?.roles);
    }
    return canAccess;
  }

  /**
   * Valida si el token actual es válido
   */
  validateToken(): Observable<boolean> {
    const token = this.getToken();
    
    if (!token) {
      return of(false);
    }

    // Primero verificar si el token no ha expirado localmente
    if (!this.isAuthenticated()) {
      this.logout();
      return of(false);
    }

    return this.http.get(`${this.API_URL}/validate`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }).pipe(
      tap(() => {
        // Token válido
      }),
      catchError((error) => {
        // Token inválido o expirado
        console.warn('Token validation failed:', error);
        this.logout();
        return of(false);
      }),
      // Mapear la respuesta a boolean
      switchMap(() => of(true))
    );
  }

  /**
   * Verifica la conexión con el backend
   */
  checkConnection(): Observable<boolean> {
    return this.http.get(`${this.API_URL}/health`).pipe(
      tap(() => {
        // Conexión exitosa
      }),
      catchError((error) => {
        console.error('Connection check failed:', error);
        return of(false);
      }),
      // Mapear la respuesta a boolean
      switchMap(() => of(true))
    );
  }

  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  private setCurrentUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private loadUserFromStorage(): void {
    const userStr = localStorage.getItem(this.USER_KEY);
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.currentUserSubject.next(user);
        // Refrescar roles desde el backend después de cargar el usuario
        // Usar setTimeout para asegurar que se ejecute después de la carga inicial
        setTimeout(() => {
          this.refreshRolesFromToken();
        }, 100);
      } catch (error) {
        console.error('Error parsing user from storage:', error);
        this.logout();
      }
    }
  }

  /**
   * Refresca los roles del usuario desde el backend
   * Útil cuando los roles se actualizan en la BD pero el usuario sigue logueado
   */
  refreshRolesFromToken(): void {
    const token = this.getToken();
    if (!token) return;

    // Obtener información actualizada del usuario desde el backend
    this.http.get<User>(`${this.API_URL}/me`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }).pipe(
      catchError((error) => {
        console.error('Error al refrescar roles desde backend:', error);
        return of(null);
      })
    ).subscribe({
      next: (userInfo) => {
        if (userInfo && userInfo.roles) {
          const currentUser = this.getCurrentUser();
          if (currentUser) {
            // Actualizar roles del usuario (preservando todos los roles)
            // userInfo.roles es un array con todos los roles del usuario
            currentUser.roles = Array.isArray(userInfo.roles) ? userInfo.roles : [];
            this.setCurrentUser(currentUser);
            console.log('Roles actualizados desde backend:', currentUser.roles);
            console.log('¿Puede acceder a análisis?', this.canAccessAnalysis());
          }
        }
      }
    });
  }

  /**
   * Obtiene información del token JWT (expiración, tiempo restante, etc.)
   */
  getTokenInfo(): {
    isValid: boolean;
    expirationDate: Date | null;
    remainingMs: number | null;
    remainingHours: number | null;
    remainingMinutes: number | null;
    isExpired: boolean;
  } {
    const token = this.getToken();
    
    if (!token) {
      return {
        isValid: false,
        expirationDate: null,
        remainingMs: null,
        remainingHours: null,
        remainingMinutes: null,
        isExpired: true
      };
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expirationTimestamp = payload.exp * 1000; // Convertir a milisegundos
      const expirationDate = new Date(expirationTimestamp);
      const currentTime = Date.now();
      const remainingMs = expirationTimestamp - currentTime;
      const isExpired = remainingMs <= 0;

      return {
        isValid: true,
        expirationDate,
        remainingMs: isExpired ? 0 : remainingMs,
        remainingHours: isExpired ? 0 : Math.floor(remainingMs / (1000 * 60 * 60)),
        remainingMinutes: isExpired ? 0 : Math.floor((remainingMs % (1000 * 60 * 60)) / (1000 * 60)),
        isExpired
      };
    } catch (error) {
      console.error('Error parsing token:', error);
      return {
        isValid: false,
        expirationDate: null,
        remainingMs: null,
        remainingHours: null,
        remainingMinutes: null,
        isExpired: true
      };
    }
  }

  /**
   * Obtiene un string formateado con el tiempo restante de la sesión
   */
  getRemainingSessionTime(): string {
    const tokenInfo = this.getTokenInfo();
    
    if (!tokenInfo.isValid || tokenInfo.isExpired) {
      return 'Session expired';
    }

    if (tokenInfo.remainingHours !== null && tokenInfo.remainingMinutes !== null) {
      if (tokenInfo.remainingHours > 0) {
        return `${tokenInfo.remainingHours}h ${tokenInfo.remainingMinutes}m`;
      } else {
        return `${tokenInfo.remainingMinutes}m`;
      }
    }

    return 'No disponible';
  }
}