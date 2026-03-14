import { Component, Input, OnInit, OnChanges, SimpleChanges, computed, signal } from '@angular/core';
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
export class DepartmentListComponent implements OnInit, OnChanges {
  private readonly pageSize = 100;
  private readonly uiPageSize = 10;

  @Input({ required: true }) siteId!: string;
  @Input() isGlobalAdmin: boolean = false;

  departments = signal<Department[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');
  currentPage = signal<number>(1);

  totalPages = computed<number>(() => {
    const total = this.departments().length;
    return Math.max(1, Math.ceil(total / this.uiPageSize));
  });

  paginatedDepartments = computed<Department[]>(() => {
    const start = (this.currentPage() - 1) * this.uiPageSize;
    return this.departments().slice(start, start + this.uiPageSize);
  });

  // Permissions drawer state
  isPermissionsDrawerOpen = signal(false);
  selectedNodeId = signal<string | null>(null);
  selectedNodeName = signal('');

  // Create dialog state
  isCreateDialogOpen = signal(false);
  newDepartmentName = signal('');

  // Rename dialog state
  isRenameDialogOpen = signal(false);
  renameTarget = signal<Department | null>(null);
  renameDepartmentName = signal('');

  // Delete dialog state
  isDeleteDialogOpen = signal(false);
  deleteTarget = signal<Department | null>(null);
  deleteConfirmationText = signal('');

  operationLoading = signal(false);
  operationError = signal('');

  constructor(private departmentService: DepartmentService) { }

  ngOnInit(): void {
    if (this.siteId) {
      this.loadDepartments();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['siteId'] && !changes['siteId'].firstChange) {
      const currentValue = (changes['siteId'].currentValue || '').toString().trim();
      const previousValue = (changes['siteId'].previousValue || '').toString().trim();
      if (currentValue && currentValue !== previousValue) {
        this.loadDepartments();
      }
    }
  }

  loadDepartments(): void {
    const normalizedSiteId = (this.siteId || '').trim();
    if (!normalizedSiteId) {
      this.departments.set([]);
      this.loading.set(false);
      this.error.set('');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.currentPage.set(1);

    this.loadDepartmentsPage(normalizedSiteId, 0, []);
  }

  private loadDepartmentsPage(siteId: string, skipCount: number, accumulator: Department[]): void {
    this.departmentService.getDepartments(siteId, this.pageSize, skipCount).subscribe({
      next: (response: any) => {
        const pageItems = response?.departments || [];
        const merged = [...accumulator, ...pageItems];
        const hasMore = !!response?.hasMore && pageItems.length > 0;

        if (hasMore) {
          this.loadDepartmentsPage(siteId, skipCount + this.pageSize, merged);
          return;
        }

        const uniqueById = new Map<string, Department>();
        for (const department of merged) {
          if (department?.id && !uniqueById.has(department.id)) {
            uniqueById.set(department.id, department);
          }
        }

        const allDepartments = Array.from(uniqueById.values())
          .sort((left, right) => (left.name || '').localeCompare(right.name || ''));

        this.departments.set(allDepartments);
        this.currentPage.set(1);
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
    if (!this.isGlobalAdmin) {
      return;
    }
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

  openCreateDialog(): void {
    if (!this.isGlobalAdmin) {
      return;
    }
    this.newDepartmentName.set('');
    this.operationError.set('');
    this.isCreateDialogOpen.set(true);
  }

  closeCreateDialog(): void {
    this.isCreateDialogOpen.set(false);
    this.newDepartmentName.set('');
    this.operationError.set('');
  }

  createDepartment(): void {
    const name = this.newDepartmentName().trim();
    if (!name) {
      this.operationError.set('Debes indicar el nombre del nuevo departamento.');
      return;
    }

    this.operationLoading.set(true);
    this.operationError.set('');

    this.departmentService.createDepartment(this.siteId, name).subscribe({
      next: () => {
        this.operationLoading.set(false);
        this.closeCreateDialog();
        this.loadDepartments();
      },
      error: (err) => {
        console.error('Error creando departamento:', err);
        this.operationLoading.set(false);
        this.operationError.set('No se pudo crear el departamento.');
      }
    });
  }

  openRenameDialog(dept: Department): void {
    if (!this.isGlobalAdmin) {
      return;
    }
    this.renameTarget.set(dept);
    this.renameDepartmentName.set((dept.name || '').trim());
    this.operationError.set('');
    this.isRenameDialogOpen.set(true);
  }

  closeRenameDialog(): void {
    this.isRenameDialogOpen.set(false);
    this.renameTarget.set(null);
    this.renameDepartmentName.set('');
    this.operationError.set('');
  }

  renameDepartment(): void {
    const target = this.renameTarget();
    const newName = this.renameDepartmentName().trim();
    if (!target?.id) {
      return;
    }
    if (!newName) {
      this.operationError.set('Debes indicar el nuevo nombre del departamento.');
      return;
    }

    this.operationLoading.set(true);
    this.operationError.set('');

    this.departmentService.renameDepartment(target.id, newName).subscribe({
      next: () => {
        this.operationLoading.set(false);
        this.closeRenameDialog();
        this.loadDepartments();
      },
      error: (err) => {
        console.error('Error renombrando departamento:', err);
        this.operationLoading.set(false);
        this.operationError.set('No se pudo renombrar el departamento.');
      }
    });
  }

  openDeleteDialog(dept: Department): void {
    if (!this.isGlobalAdmin) {
      return;
    }
    this.deleteTarget.set(dept);
    this.deleteConfirmationText.set('');
    this.operationError.set('');
    this.isDeleteDialogOpen.set(true);
  }

  closeDeleteDialog(): void {
    this.isDeleteDialogOpen.set(false);
    this.deleteTarget.set(null);
    this.deleteConfirmationText.set('');
    this.operationError.set('');
  }

  canConfirmDelete(): boolean {
    return this.deleteConfirmationText().trim().toUpperCase() === 'ELIMINAR';
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

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target?.id) {
      return;
    }
    if (!this.canConfirmDelete()) {
      this.operationError.set('Debes escribir ELIMINAR para confirmar el borrado.');
      return;
    }

    this.operationLoading.set(true);
    this.operationError.set('');

    this.departmentService.deleteDepartment(target.id).subscribe({
      next: () => {
        this.operationLoading.set(false);
        this.closeDeleteDialog();
        this.loadDepartments();
      },
      error: (err) => {
        console.error('Error eliminando departamento:', err);
        this.operationLoading.set(false);
        this.operationError.set('No se pudo eliminar el departamento.');
      }
    });
  }
}
