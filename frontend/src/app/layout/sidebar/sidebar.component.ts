import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  isCollapsed = false;

  constructor(public authService: AuthService) { }

  /** Nombre a mostrar en el sidebar: nombre completo, username o fallback. */
  get displayName(): string {
    const user = this.authService.getUserData();
    if (!user) return 'Usuario';
    const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim();
    return fullName || user.username || 'Usuario';
  }

  /** Iniciales del usuario para el avatar colapsado. */
  get initials(): string {
    const user = this.authService.getUserData();
    if (!user) return '?';
    const first = (user.firstName?.[0] || user.username?.[0] || '?').toUpperCase();
    const last = (user.lastName?.[0] || '').toUpperCase();
    return first + last;
  }

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  logout() {
    this.authService.logout().subscribe();
  }
}
