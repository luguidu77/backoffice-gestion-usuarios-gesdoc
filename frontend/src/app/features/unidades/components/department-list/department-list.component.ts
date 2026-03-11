import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DepartmentService } from '../../../../core/services/department.service';
import { Department } from '../../../../core/models/department.model';

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './department-list.component.html',
  styleUrl: './department-list.component.scss'
})
export class DepartmentListComponent implements OnInit {
  @Input({ required: true }) siteId!: string;
  @Input() isGlobalAdmin: boolean = false;

  departments = signal<Department[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');

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
}
