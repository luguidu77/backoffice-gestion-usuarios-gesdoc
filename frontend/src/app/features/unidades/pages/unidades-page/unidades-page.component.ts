import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SiteService } from '../../../../core/services/site.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Site } from '../../../../core/models/site.model';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-unidades-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './unidades-page.component.html',
  styleUrl: './unidades-page.component.scss'
})
export class UnidadesPageComponent implements OnInit, OnDestroy {
  sites = signal<Site[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');
  totalSites = signal<number>(0);
  hasMore = signal<boolean>(false);
  searchTerm = signal<string>('');
  currentPage = signal<number>(1);
  readonly pageSize = 10;

  /** Rol del usuario en sesión */
  readonly userRole = this.authService.getUserRole();

  /**
   * Lista de sitios visible para el usuario:
   *   - GLOBAL_ADMIN → todos los sitios cargados
   *   - UNIT_ADMIN   → solo los sitios donde es SiteManager
   * Además aplica el filtro de texto del buscador.
   */
  filteredSites = computed(() => {
    const role = this.userRole;
    const managedIds = this.authService.getManagedSiteIds();

    // Limitar al scope del UNIT_ADMIN
    let base = this.sites();
    if (role === 'UNIT_ADMIN') {
      base = base.filter(s => managedIds.includes(s.id));
    }

    // Filtro de texto
    const term = this.searchTerm().toLowerCase();
    if (!term) return base;
    return base.filter(s =>
      s.id?.toLowerCase().includes(term) ||
      s.title?.toLowerCase().includes(term) ||
      s.description?.toLowerCase().includes(term)
    );
  });

  totalPages = computed<number>(() => {
    const total = this.filteredSites().length;
    return Math.max(1, Math.ceil(total / this.pageSize));
  });

  paginatedSites = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredSites().slice(start, start + this.pageSize);
  });

  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  constructor(
    private siteService: SiteService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(200),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm.set(term);
      this.currentPage.set(1);
    });

    this.loadSites();
  }

  /**
   * Carga la lista de sitios desde el backend.
   */
  loadSites(): void {
    this.loading.set(true);
    this.error.set('');

    this.siteService.getSites(100, 0).subscribe({
      next: (response: any) => {
        this.sites.set(response.sites);
        this.totalSites.set(response.totalSites);
        this.hasMore.set(response.hasMore);
        this.currentPage.set(1);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Error cargando sitios:', err);
        this.error.set('Error al cargar la lista de sitios');
        this.loading.set(false);
      }
    });
  }

  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
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
    if (this.canManageUnit()) {
      this.router.navigate(['/users'], { queryParams: { siteId, siteName } });
    }
  }

  /**
   * Determina si el usuario actual puede gestionar unidades.
   * GLOBAL_ADMIN y UNIT_ADMIN tienen acceso; a READ_ONLY no debería llegarle esta página.
   */
  canManageUnit(): boolean {
    return this.userRole === 'GLOBAL_ADMIN' || this.userRole === 'UNIT_ADMIN';
  }

  /**
   * Recarga la lista de sitios.
   */
  refresh(): void {
    this.loadSites();
  }

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }
}
