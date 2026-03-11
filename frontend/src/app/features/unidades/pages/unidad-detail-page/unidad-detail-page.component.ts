import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SiteService } from '../../../../core/services/site.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../../../core/services/user.service';
import { User } from '../../../../core/models/user.model';
import { DepartmentListComponent } from '../../components/department-list/department-list.component';

@Component({
  selector: 'app-unidad-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DepartmentListComponent],
  templateUrl: './unidad-detail-page.component.html',
  styleUrl: './unidad-detail-page.component.scss'
})
export class UnidadDetailPageComponent implements OnInit {
  siteId = signal<string>('');
  activeTab = signal<'resumen' | 'departamentos' | 'miembros' | 'ajustes'>('resumen');

  isGlobalAdmin = signal<boolean>(false);

  members = signal<User[]>([]);
  loadingMembers = signal<boolean>(false);
  membersError = signal<string>('');

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private userService: UserService
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.siteId.set(id);
    }

    const user = this.authService.getUserData();
    this.isGlobalAdmin.set(user?.isGlobalAdmin === true);
  }

  setTab(tab: 'resumen' | 'departamentos' | 'miembros' | 'ajustes') {
    this.activeTab.set(tab);

    if (tab === 'miembros' && this.members().length === 0) {
      this.loadMembers();
    }
  }

  loadMembers(): void {
    if (!this.siteId()) return;

    this.loadingMembers.set(true);
    this.membersError.set('');

    this.userService.getUsers(100, 0, this.siteId()).subscribe({
      next: (response) => {
        this.members.set(response.users);
        this.loadingMembers.set(false);
      },
      error: (err) => {
        console.error('Error cargando miembros:', err);
        this.membersError.set('Error al cargar los miembros de la unidad');
        this.loadingMembers.set(false);
      }
    });
  }
}
