import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GroupService } from '../../../../core/services/group.service';
import { GroupItem, GroupMemberItem, GroupMembersResponse } from '../../../../core/models/group.model';

@Component({
    selector: 'app-group-detail-drawer',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './group-detail-drawer.component.html',
    styleUrl: './group-detail-drawer.component.scss'
})
export class GroupDetailDrawerComponent implements OnChanges {
    @Input() group: GroupItem | null = null;
    @Input() isOpen: boolean = false;

    @Output() closeDrawer = new EventEmitter<void>();

    members = signal<GroupMemberItem[]>([]);
    loading = signal(false);
    error = signal('');
    totalItems = signal(0);

    constructor(private groupService: GroupService) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['isOpen'] && this.isOpen && this.group) {
            this.loadMembers();
        }
        if (changes['isOpen'] && !this.isOpen) {
            this.members.set([]);
            this.error.set('');
        }
        if (changes['group'] && this.group && this.isOpen) {
            this.loadMembers();
        }
    }

    loadMembers(): void {
        if (!this.group) return;
        this.loading.set(true);
        this.error.set('');

        this.groupService.getGroupMembers(this.group.id).subscribe({
            next: (response: GroupMembersResponse) => {
                this.members.set(response.members || []);
                this.totalItems.set(response.totalItems || 0);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error cargando miembros del grupo:', err);
                this.error.set('No se pudieron cargar los miembros del grupo.');
                this.loading.set(false);
            }
        });
    }

    get personMembers(): GroupMemberItem[] {
        return this.members().filter(m => m.memberType === 'PERSON');
    }

    get groupMembers(): GroupMemberItem[] {
        return this.members().filter(m => m.memberType === 'GROUP');
    }

    onClose(): void {
        this.closeDrawer.emit();
    }
}
