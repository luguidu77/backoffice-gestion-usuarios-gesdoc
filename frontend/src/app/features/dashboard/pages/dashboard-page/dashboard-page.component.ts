/**
 * Página principal del Dashboard.
 *
 * Responsabilidades:
 *   1. Al cargar, llamar al endpoint /ping del backend y mostrar el resultado
 *   2. Mostrar una tarjeta con el estado del servidor
 *   3. Mostrar una tarjeta con los próximos pasos del proyecto
 *
 * Patrones de Angular 17 utilizados:
 *   - signal()    → estado reactivo local (alternativa moderna a variables simples)
 *   - @if / @else → sintaxis de control de flujo nueva (Angular 17+)
 *   - OnInit      → ciclo de vida: se ejecuta una vez al montar el componente
 */
import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../../../core/services/api.service';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [LoadingSpinnerComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent implements OnInit {

  // --- Estado reactivo con Signals (Angular 17) ---
  // signal() es similar a una variable pero Angular detecta sus cambios
  // automáticamente y re-renderiza solo las partes del template afectadas.

  /** Texto de respuesta del servidor */
  pingStatus = signal<string>('—');

  /** Indica si hay una petición en curso */
  pingLoading = signal<boolean>(false);

  /** Indica si la última petición falló */
  pingError = signal<boolean>(false);

  constructor(private apiService: ApiService) {}

  /**
   * ngOnInit se ejecuta justo después de que Angular crea el componente.
   * Es el lugar correcto para cargar datos iniciales.
   */
  ngOnInit(): void {
    this.checkPing();
  }

  /**
   * Llama a GET /ping del backend y actualiza el estado del componente.
   * El botón "Actualizar" del template también llama a este método.
   */
  checkPing(): void {
    this.pingLoading.set(true);
    this.pingError.set(false);

    this.apiService.ping().subscribe({
      next: (response: string) => {
        // El backend responde con "OK BACKOFFICE"
        this.pingStatus.set(response);
        this.pingLoading.set(false);
      },
      error: () => {
        // El interceptor ya ha logueado el error en consola
        this.pingStatus.set('Sin respuesta del servidor');
        this.pingError.set(true);
        this.pingLoading.set(false);
      }
    });
  }

}
