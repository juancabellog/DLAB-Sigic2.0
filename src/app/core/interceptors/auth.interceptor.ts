import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // No agregar token a endpoints de autenticación
  if (req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register') || req.url.includes('/api/auth/validate')) {
    return next(req);
  }

  if (token) {
    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    
    return next(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // Si es error 401, el token expiró
        if (error.status === 401) {
          console.warn('Token expired, logging out user');
          authService.logout();
        }
        
        // Si es error de conexión (0, 500, 502, 503, 504)
        if (error.status === 0 || error.status >= 500) {
          console.error('Connection error:', error);
          // Aquí podrías emitir un evento o mostrar una notificación
        }
        
        return throwError(() => error);
      })
    );
  }

  return next(req);
};