import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../core/models/user.model';
import { FormsModule } from '@angular/forms';
import { UserDetailDrawerComponent } from '../../components/user-detail-drawer/user-detail-drawer.component';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, UserDetailDrawerComponent],
  templateUrl: './user-list-page.component.html',
  styleUrls: ['./user-list-page.component.scss']
})
export class UserListPageComponent implements OnInit, OnDestroy {
  users = signal<User[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');
  totalUsers = signal<number>(0);
  hasMore = signal<boolean>(false);

  // Filtros
  searchTerm = signal<string>('');
  siteId = signal<string | null>(null);
  siteName = signal<string | null>(null);

  // Drawer status
  selectedUser = signal<User | null>(null);
  isDrawerOpen = signal<boolean>(false);

  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  constructor(
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => this.loadUsers());

    // Escuchar cambios en los queryParams (para filtrado por sitio)
    this.route.queryParams.subscribe(params => {
      const siteId = params['siteId'];
      const siteName = params['siteName'];
      if (siteId) {
        this.siteId.set(siteId);
        this.siteName.set(siteName || siteId);
      } else {
        this.siteId.set(null);
        this.siteName.set(null);
      }
      this.loadUsers();
    });
  }

  /**
   * Carga la lista de usuarios desde el backend.
   */
  loadUsers(): void {
    this.loading.set(true);
    this.error.set('');

    this.userService.getUsers(100, 0, this.siteId() || undefined, this.searchTerm()).subscribe({
      next: (response) => {
        this.users.set(response.users);
        this.totalUsers.set(response.totalUsers);
        this.hasMore.set(response.hasMore);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando usuarios:', err);
        this.error.set('Error al cargar la lista de usuarios');
        this.loading.set(false);
      }
    });
  }

  /**
   * Manejador de input en tiempo real: dispara búsqueda a partir del 3er carácter.
   */
  onSearchInput(value: string): void {
    this.searchTerm.set(value);
    if (value.length >= 3 || value.length === 0) {
      this.searchSubject.next(value);
    }
  }

  /**
   * Ejecuta la búsqueda de usuarios.
   */
  onSearch(): void {
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  /**
   * Limpia todos los filtros activos.
   */
  clearFilters(): void {
    this.searchTerm.set('');
    if (this.siteId()) {
      // Si hay siteId, quitarlo de la URL
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { siteId: null, siteName: null },
        queryParamsHandling: 'merge'
      });
    } else {
      this.loadUsers();
    }
  }

  /**
   * Obtiene el nombre completo de un usuario.
   */
  getFullName(user: User): string {
    const firstName = user.firstName || '';
    const lastName = user.lastName || '';
    return `${firstName} ${lastName}`.trim() || user.id;
  }

  /**
   * Obtiene la clase CSS para el badge de estado.
   */
  getStatusClass(enabled: boolean): string {
    return enabled ? 'status-active' : 'status-inactive';
  }

  /**
   * Obtiene el texto del estado.
   */
  getStatusText(enabled: boolean): string {
    return enabled ? 'Activo' : 'Inactivo';
  }

  /**
   * Navega al detalle de un usuario.
   */
  viewUser(user: User): void {
    this.selectedUser.set(user);
    this.isDrawerOpen.set(true);
  }

  closeUserDrawer(): void {
    this.isDrawerOpen.set(false);
    setTimeout(() => this.selectedUser.set(null), 300); // Wait for animation
  }

  /**
   * Recarga la lista de usuarios.
   */
  refresh(): void {
    this.loadUsers();
  }
}
