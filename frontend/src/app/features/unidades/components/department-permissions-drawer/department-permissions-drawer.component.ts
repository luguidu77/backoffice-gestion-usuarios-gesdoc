import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepartmentService } from '../../../../core/services/department.service';
import { GroupService } from '../../../../core/services/group.service';
import {
    NodePermissionsResponse,
    UpdateNodePermissionsRequest,
    AdvancedPermission,
    CONTENT_ROLES
} from '../../../../core/models/department.model';
import { GroupItem } from '../../../../core/models/group.model';

@Component({
    selector: 'app-department-permissions-drawer',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './department-permissions-drawer.component.html',
    styleUrl: './department-permissions-drawer.component.scss'
})
export class DepartmentPermissionsDrawerComponent implements OnChanges {
    @Input() nodeId: string | null = null;
    @Input() nodeName: string = '';
    @Input() isOpen: boolean = false;
    @Input() isGlobalAdmin: boolean = false;

    @Output() closeDrawer = new EventEmitter<void>();
    @Output() permissionsUpdated = new EventEmitter<void>();

    readonly CONTENT_ROLES = CONTENT_ROLES;

    // State
    permissions = signal<NodePermissionsResponse | null>(null);
    loading = signal(false);
    saving = signal(false);
    error = signal('');
    successMessage = signal('');
    isHelpOpen = signal(false);

    // Search groups state
    searchingGroups = signal(false);
    availableGroups = signal<GroupItem[]>([]);
    groupSearchTerm = signal('');

    // New permission form
    newPermAuthority = signal('');
    newPermRole = signal('Contributor');
    newPermAccess = signal<'ALLOWED' | 'DENIED'>('ALLOWED');

    // Computed: edited local permissions
    localPermissions = signal<AdvancedPermission[]>([]);
    inheritanceEnabled = signal(true);

    isModified = computed(() => {
        const original = this.permissions();
        if (!original) return false;
        const localChanged = JSON.stringify(this.localPermissions()) !== JSON.stringify(original.locallySet);
        const inheritChanged = this.inheritanceEnabled() !== original.isInheritanceEnabled;
        return localChanged || inheritChanged;
    });

    constructor(
        private departmentService: DepartmentService,
        private groupService: GroupService
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['isOpen'] && this.isOpen && this.nodeId) {
            if (!this.isGlobalAdmin) {
                this.error.set('Solo el administrador global puede ver y editar permisos de departamentos.');
                return;
            }
            this.loadPermissions();
            this.loadGroups();
        }
        if (changes['isOpen'] && !this.isOpen) {
            this.resetState();
        }
    }

    loadPermissions(): void {
        if (!this.nodeId) return;
        this.loading.set(true);
        this.error.set('');

        this.departmentService.getNodePermissions(this.nodeId).subscribe({
            next: (response) => {
                this.permissions.set(response);
                this.localPermissions.set([...response.locallySet]);
                this.inheritanceEnabled.set(response.isInheritanceEnabled);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error cargando permisos:', err);
                this.error.set('No se pudieron cargar los permisos del departamento.');
                this.loading.set(false);
            }
        });
    }

    loadGroups(searchTerm?: string): void {
        this.searchingGroups.set(true);
        this.groupService.getGroups(false, searchTerm, 100).subscribe({
            next: (response) => {
                this.availableGroups.set(response.groups || []);
                this.searchingGroups.set(false);
            },
            error: () => {
                this.availableGroups.set([]);
                this.searchingGroups.set(false);
            }
        });
    }

    onGroupSearch(term: string): void {
        this.groupSearchTerm.set(term);
        this.loadGroups(term || undefined);
    }

    addPermission(): void {
        if (!this.isGlobalAdmin) {
            return;
        }
        const authorityId = this.newPermAuthority();
        if (!authorityId) return;

        const exists = this.localPermissions().some(p => p.authorityId === authorityId);
        if (exists) {
            this.error.set(`Ya existe un permiso para "${authorityId}".`);
            return;
        }

        const group = this.availableGroups().find(g => g.id === authorityId);
        const newPerm: AdvancedPermission = {
            authorityId,
            authorityDisplayName: group?.displayName ?? authorityId,
            authorityType: authorityId.startsWith('GROUP_') ? 'GROUP' : 'USER',
            name: this.newPermRole(),
            accessStatus: this.newPermAccess()
        };

        this.localPermissions.update(list => [...list, newPerm]);
        this.newPermAuthority.set('');
        this.error.set('');
    }

    removePermission(authorityId: string): void {
        if (!this.isGlobalAdmin) {
            return;
        }
        this.localPermissions.update(list => list.filter(p => p.authorityId !== authorityId));
    }

    updatePermissionRole(authorityId: string, newRole: string): void {
        if (!this.isGlobalAdmin) {
            return;
        }
        this.localPermissions.update(list =>
            list.map(p => p.authorityId === authorityId ? { ...p, name: newRole } : p)
        );
    }

    toggleInheritance(): void {
        if (!this.isGlobalAdmin) {
            return;
        }
        this.inheritanceEnabled.update(v => !v);
    }

    save(): void {
        if (!this.isGlobalAdmin) {
            return;
        }
        if (!this.nodeId || !this.isModified()) return;
        this.saving.set(true);
        this.error.set('');
        this.successMessage.set('');

        const request: UpdateNodePermissionsRequest = {
            isInheritanceEnabled: this.inheritanceEnabled(),
            locallySet: this.localPermissions().map(p => ({
                authorityId: p.authorityId,
                name: p.name,
                accessStatus: p.accessStatus
            }))
        };

        this.departmentService.updateNodePermissions(this.nodeId, request).subscribe({
            next: (updated) => {
                this.permissions.set(updated);
                this.localPermissions.set([...updated.locallySet]);
                this.inheritanceEnabled.set(updated.isInheritanceEnabled);
                this.successMessage.set('Permisos actualizados correctamente.');
                this.saving.set(false);
                this.permissionsUpdated.emit();
                setTimeout(() => this.successMessage.set(''), 4000);
            },
            error: (err) => {
                console.error('Error guardando permisos:', err);
                this.error.set('No se pudieron guardar los permisos. Verifique los datos.');
                this.saving.set(false);
            }
        });
    }

    discardChanges(): void {
        if (!this.isGlobalAdmin) {
            return;
        }
        const original = this.permissions();
        if (!original) return;
        this.localPermissions.set([...original.locallySet]);
        this.inheritanceEnabled.set(original.isInheritanceEnabled);
        this.error.set('');
    }

    onClose(): void {
        if (this.isModified()) {
            if (!confirm('Hay cambios sin guardar. ¿Desea descartarlos?')) return;
        }
        this.closeDrawer.emit();
    }

    getRoleLabel(roleValue: string): string {
        return CONTENT_ROLES.find(r => r.value === roleValue)?.label ?? roleValue;
    }

    private resetState(): void {
        this.permissions.set(null);
        this.localPermissions.set([]);
        this.error.set('');
        this.successMessage.set('');
        this.isHelpOpen.set(false);
        this.newPermAuthority.set('');
        this.newPermRole.set('Contributor');
        this.newPermAccess.set('ALLOWED');
        this.groupSearchTerm.set('');
    }
}
