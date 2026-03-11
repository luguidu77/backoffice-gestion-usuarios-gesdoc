import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User, Group } from '../../../../core/models/user.model';
import { UserService } from '../../../../core/services/user.service';

@Component({
  selector: 'app-user-detail-drawer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-detail-drawer.component.html',
  styleUrl: './user-detail-drawer.component.scss'
})
export class UserDetailDrawerComponent implements OnChanges {
  @Input() user: User | null = null;
  @Input() isOpen: boolean = false;

  @Output() closeDrawer = new EventEmitter<void>();

  userGroups = signal<Group[]>([]);
  loadingGroups = signal<boolean>(false);

  constructor(private userService: UserService) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['user'] && this.user) {
      this.loadUserGroups(this.user.id);
    }
  }

  loadUserGroups(userId: string): void {
    this.loadingGroups.set(true);
    this.userService.getUserGroups(userId).subscribe({
      next: (response) => {
        this.userGroups.set(response.groups || []);
        this.loadingGroups.set(false);
      },
      error: (err) => {
        console.error('Error loading user groups', err);
        this.userGroups.set([]);
        this.loadingGroups.set(false);
      }
    });
  }

  onClose() {
    this.closeDrawer.emit();
  }

  getFullName(): string {
    if (!this.user) return '';
    return `${this.user.firstName || ''} ${this.user.lastName || ''}`.trim() || this.user.id;
  }
}
