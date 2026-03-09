/**
 * Componente spinner de carga reutilizable.
 *
 * Componente shared: pequeño, sin dependencias externas, usado desde
 * cualquier feature cuando se esperan datos asincrónicos.
 *
 * Uso en un template:
 *   @if (loading()) {
 *     <app-loading-spinner />
 *   }
 *
 * El template y estilos están inline (no ficheros separados)
 * porque el componente es muy pequeño. Esta es una práctica
 * aceptada en Angular para componentes de utilidad simples.
 */
import { Component } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [],
  template: `
    <div class="spinner-wrapper" role="status" aria-label="Cargando...">
      <div class="spinner"></div>
      <span class="spinner-text">Cargando...</span>
    </div>
  `,
  styles: [`
    .spinner-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 32px;
      gap: 12px;
    }

    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid #bee3f8;
      border-top-color: var(--color-primary-light, #4299e1);
      border-radius: 50%;
      animation: giro 0.75s linear infinite;
    }

    .spinner-text {
      font-size: 0.85rem;
      color: var(--color-text-muted, #718096);
    }

    @keyframes giro {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoadingSpinnerComponent {}
