import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { SiteService } from '../../../../core/services/site.service';
import { Site } from '../../../../core/models/site.model';

@Component({
  selector: 'app-unidades-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './unidades-page.component.html',
  styleUrl: './unidades-page.component.scss'
})
export class UnidadesPageComponent implements OnInit {
  sites = signal<Site[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');
  totalSites = signal<number>(0);
  hasMore = signal<boolean>(false);

  constructor(
    private siteService: SiteService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadSites();
  }

  /**
   * Carga la lista de sitios desde el backend.
   */
  loadSites(): void {
    this.loading.set(true);
    this.error.set('');

    this.siteService.getSites(100, 0).subscribe({
      next: (response) => {
        this.sites.set(response.sites);
        this.totalSites.set(response.totalSites);
        this.hasMore.set(response.hasMore);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando sitios:', err);
        this.error.set('Error al cargar la lista de sitios');
        this.loading.set(false);
      }
    });
  }

  /**
   * Devuelve el texto de visibilidad en español.
   */
  getVisibilityLabel(visibility: string): string {
    switch (visibility?.toUpperCase()) {
      case 'PUBLIC': return 'Público';
      case 'PRIVATE': return 'Privado';
      case 'MODERATED': return 'Moderado';
      default: return visibility || '-';
    }
  }

  /**
   * Devuelve la clase CSS según la visibilidad del sitio.
   */
  getVisibilityClass(visibility: string): string {
    switch (visibility?.toUpperCase()) {
      case 'PUBLIC': return 'visibility-public';
      case 'PRIVATE': return 'visibility-private';
      case 'MODERATED': return 'visibility-moderated';
      default: return '';
    }
  }

  /**
   * Navega a la gestión de usuarios filtrando por este sitio.
   */
  viewMembers(siteId: string, siteName?: string): void {
    this.router.navigate(['/users'], { queryParams: { siteId, siteName } });
  }

  /**
   * Recarga la lista de sitios.
   */
  refresh(): void {
    this.loadSites();
  }
}
