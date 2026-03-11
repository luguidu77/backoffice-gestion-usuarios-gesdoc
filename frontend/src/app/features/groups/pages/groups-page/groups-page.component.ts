import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GroupService } from '../../../../core/services/group.service';
import { GroupItem } from '../../../../core/models/group.model';
import { GroupDetailDrawerComponent } from '../../components/group-detail-drawer/group-detail-drawer.component';

@Component({
    selector: 'app-groups-page',
    standalone: true,
    imports: [CommonModule, FormsModule, GroupDetailDrawerComponent],
    templateUrl: './groups-page.component.html',
    styleUrl: './groups-page.component.scss'
})
export class GroupsPageComponent implements OnInit {
    groups = signal<GroupItem[]>([]);
    loading = signal(false);
    error = signal('');

    searchTerm = signal('');
    onlyRoot = signal(false);

    // Drawer state
    isDrawerOpen = signal(false);
    selectedGroup = signal<GroupItem | null>(null);

    filteredGroups = computed(() => {
        const term = this.searchTerm().toLowerCase();
        if (!term) return this.groups();
        return this.groups().filter(g =>
            g.displayName.toLowerCase().includes(term) || g.id.toLowerCase().includes(term)
        );
    });

    constructor(private groupService: GroupService) { }

    ngOnInit(): void {
        this.loadGroups();
    }

    loadGroups(): void {
        this.loading.set(true);
        this.error.set('');

        this.groupService.getGroups(this.onlyRoot()).subscribe({
            next: (response) => {
                this.groups.set(response.groups || []);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error cargando grupos:', err);
                this.error.set('No se pudieron cargar los grupos de Alfresco.');
                this.loading.set(false);
            }
        });
    }

    onOnlyRootChange(event: Event): void {
        this.onlyRoot.set((event.target as HTMLInputElement).checked);
        this.loadGroups();
    }

    openGroupDrawer(group: GroupItem): void {
        this.selectedGroup.set(group);
        this.isDrawerOpen.set(true);
    }

    closeGroupDrawer(): void {
        this.isDrawerOpen.set(false);
        this.selectedGroup.set(null);
    }
}
