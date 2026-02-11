import { HttpInterceptorFn, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, map } from 'rxjs/operators';
import { throwError, of } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  // Excluir rutas de monitoreo interno que no deberían mostrar errores al usuario
  const isInternalMonitoringRoute = 
    req.url.includes('/api/auth/validate') || 
    req.url.includes('/api/health');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Si es una ruta de monitoreo interno, manejar de forma especial
      if (isInternalMonitoringRoute) {
        // Si el status es 200, significa que la respuesta fue exitosa pero llegó al catchError
        // Convertirla en una respuesta exitosa para que el servicio la maneje correctamente
        if (error.status === 200) {
          // Crear una respuesta exitosa a partir del error
          const successResponse = new HttpResponse({
            status: 200,
            statusText: 'OK',
            body: error.error || null,
            headers: error.headers,
            url: error.url || undefined
          });
          // Retornar como Observable exitoso
          return of(successResponse);
        }
        
        // Para rutas de monitoreo con errores reales, solo loguear errores reales (no 0)
        // y no mostrar snackbar al usuario
        if (error.status && error.status !== 0) {
          console.warn('Internal monitoring error:', error.status, req.url);
        }
        // Re-lanzar el error para que el servicio lo maneje correctamente
        return throwError(() => error);
      }

      // Ignorar respuestas con status 200 que puedan ser tratadas como errores
      // (esto no debería pasar en catchError, pero por si acaso)
      if (error.status === 200) {
        // Convertir en respuesta exitosa
        const successResponse = new HttpResponse({
          status: 200,
          statusText: 'OK',
          body: error.error || null,
          headers: error.headers,
          url: error.url || undefined
        });
        return of(successResponse);
      }

      let errorMessage = 'Ha ocurrido un error desconocido';

      if (error.error instanceof ErrorEvent) {
        // Error del lado del cliente
        errorMessage = `Error: ${error.error.message}`;
      } else {
        // Error del lado del servidor
        switch (error.status) {
          case 400:
            errorMessage = 'Solicitud incorrecta. Verifique los datos enviados.';
            break;
          case 401:
            errorMessage = 'No autorizado. Por favor, inicie sesión nuevamente.';
            break;
          case 403:
            errorMessage = 'No tiene permisos para realizar esta acción.';
            break;
          case 404:
            errorMessage = 'Recurso no encontrado.';
            break;
          case 409:
            errorMessage = 'Conflicto: El recurso ya existe o está en uso.';
            break;
          case 422:
            errorMessage = 'Datos de entrada inválidos.';
            break;
          case 500:
            errorMessage = 'Error interno del servidor. Intente nuevamente más tarde.';
            break;
          default:
            errorMessage = `Error del servidor: ${error.status} - ${error.message}`;
        }
      }

      // Mostrar mensaje de error al usuario solo para rutas que no son de monitoreo
      snackBar.open(errorMessage, 'Cerrar', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });

      console.error('Error HTTP:', error);
      return throwError(() => error);
    })
  );
};