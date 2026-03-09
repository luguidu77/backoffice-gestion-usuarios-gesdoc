import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../core/models/user.model';

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-list-page.component.html',
  styleUrls: ['./user-list-page.component.scss']
})
export class UserListPageComponent implements OnInit {
  users = signal<User[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');
  totalUsers = signal<number>(0);
  hasMore = signal<boolean>(false);

  constructor(
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  /**
   * Carga la lista de usuarios desde el backend.
   */
  loadUsers(): void {
    this.loading.set(true);
    this.error.set('');

    this.userService.getUsers(100, 0).subscribe({
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
  viewUser(userId: string): void {
    // TODO: Implementar vista de detalle
    console.log('Ver usuario:', userId);
  }

  /**
   * Recarga la lista de usuarios.
   */
  refresh(): void {
    this.loadUsers();
  }
}
