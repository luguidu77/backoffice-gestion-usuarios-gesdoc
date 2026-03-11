import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DepartmentService } from '../../../../core/services/department.service';
import { Department } from '../../../../core/models/department.model';
import { DepartmentPermissionsDrawerComponent } from '../department-permissions-drawer/department-permissions-drawer.component';

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [CommonModule, DepartmentPermissionsDrawerComponent],
  templateUrl: './department-list.component.html',
  styleUrl: './department-list.component.scss'
})
export class DepartmentListComponent implements OnInit {
  @Input({ required: true }) siteId!: string;
  @Input() isGlobalAdmin: boolean = false;

  departments = signal<Department[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');

  // Permissions drawer state
  isPermissionsDrawerOpen = signal(false);
  selectedNodeId = signal<string | null>(null);
  selectedNodeName = signal('');

  constructor(private departmentService: DepartmentService) { }

  ngOnInit(): void {
    if (this.siteId) {
      this.loadDepartments();
    }
  }

  loadDepartments(): void {
    this.loading.set(true);
    this.error.set('');

    this.departmentService.getDepartments(this.siteId, 100, 0).subscribe({
      next: (response: any) => {
        this.departments.set(response.departments);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Error cargando departamentos:', err);
        this.error.set('No se pudieron cargar los departamentos de la unidad');
        this.loading.set(false);
      }
    });
  }

  refresh(): void {
    this.loadDepartments();
  }

  openPermissionsDrawer(dept: Department): void {
    this.selectedNodeId.set(dept.id);
    this.selectedNodeName.set(dept.name);
    this.isPermissionsDrawerOpen.set(true);
  }

  closePermissionsDrawer(): void {
    this.isPermissionsDrawerOpen.set(false);
    this.selectedNodeId.set(null);
  }

  onPermissionsUpdated(): void {
    this.loadDepartments();
  }
}
