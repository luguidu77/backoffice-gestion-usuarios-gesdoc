import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { catchError, finalize, map, switchMap } from 'rxjs/operators';
import { forkJoin, of } from 'rxjs';

import { GroupService } from '../../../../core/services/group.service';
import { UserService } from '../../../../core/services/user.service';
import { SiteService } from '../../../../core/services/site.service';
import { AuthService } from '../../../../core/services/auth.service';
import { GroupItem, GroupMemberItem } from '../../../../core/models/group.model';
import {
  User,
  Group,
  UserListResponse,
  GroupListResponse as UserGroupListResponse,
  UserSiteMembership,
  UserSiteMembershipListResponse,
  UnitReassignmentMode,
  UnitReassignmentProofResponse
} from '../../../../core/models/user.model';

interface DepartmentView {
  id: string;
  technicalName: string;
  displayName: string;
  unitId: string | null;
  unitName: string;
}

interface UnitMenuView {
  unitId: string;
  unitName: string;
  departments: DepartmentView[];
}

interface GlobalUserRow {
  member: GroupMemberItem;
  email: string;
  enabled: boolean;
}

interface UnitOption {
  id: string;
  name: string;
}

interface UnitDiff {
  original: string[];
  target: string[];
  final: string[];
  toAdd: string[];
  toRemove: string[];
  transferFrom: string | null;
}

interface DepartmentDiff {
  desired: string[];
  toAdd: string[];
  toRemove: string[];
}

interface UnitDepartmentSection {
  unitId: string;
  unitName: string;
  departments: DepartmentView[];
}

interface UnitReassignmentResult {
  userId: string;
  mode: UnitReassignmentMode;
  previousUnits: UnitOption[];
  targetUnits: UnitOption[];
  finalUnits: UnitOption[];
  addedUnits: UnitOption[];
  removedUnits: UnitOption[];
  finalDepartments: string[];
  addedDepartments: string[];
  removedDepartments: string[];
  proofFileName: string;
  proofStoredFileName: string;
}

interface UserUnitMembershipCard {
  siteId: string;
  siteName: string;
  role: string;
  isAdmin: boolean;
  departments: string[];
  technicalGroups: string[];
}

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list-page.component.html',
  styleUrls: ['./user-list-page.component.scss']
})
export class UserListPageComponent implements OnInit, OnDestroy {
  private readonly role = this.authService.getUserRole();
  private readonly managedSiteIdSet = new Set(
    this.authService.getManagedSiteIds().map(id => id.toUpperCase())
  );

  readonly isGlobalAdmin = this.role === 'GLOBAL_ADMIN';
  readonly isUnitAdmin = this.role === 'UNIT_ADMIN';

  viewMode = signal<'structure' | 'global'>('global');

  rawGroups = signal<GroupItem[]>([]);
  unitNameById = signal<Record<string, string>>({});
  groupsLoading = signal<boolean>(false);
  groupsError = signal<string>('');
  groupSearch = signal<string>('');
  expandedUnitId = signal<string | null>(null);
  selectedGroupId = signal<string | null>(null);
  selectedUnitId = signal<string | null>(null);
  selectedUnitName = signal<string>('');
  isUnitContextLocked = signal<boolean>(false);

  members = signal<GroupMemberItem[]>([]);
  membersLoading = signal<boolean>(false);
  membersError = signal<string>('');
  memberSearch = signal<string>('');
  memberNameById = signal<Record<string, string>>({});
  memberManagedSiteIdsByUser = signal<Record<string, string[]>>({});

  unitListPage = signal<number>(1);
  readonly unitListPageSize = 8;
  memberListPage = signal<number>(1);
  readonly memberListPageSize = 12;

  globalSearchTerm = signal<string>('');
  globalResults = signal<GlobalUserRow[]>([]);
  globalSearching = signal<boolean>(false);
  globalSearchError = signal<string>('');
  globalPage = signal<number>(1);
  globalTotalUsers = signal<number>(0);
  globalHasMore = signal<boolean>(false);
  readonly globalPageSize = 50;
  pendingFocusUserId = signal<string>('');

  private globalSearchDebounceTimer: ReturnType<typeof setTimeout> | null = null;

  actionMessage = signal<string>('');
  actionError = signal<string>('');
  busyPromoteUserId = signal<string | null>(null);
  isUserActionsOpen = signal<boolean>(false);
  actionMember = signal<GroupMemberItem | null>(null);
  actionContextUnitId = signal<string | null>(null);
  actionContextUnitName = signal<string>('');
  actionUserFullName = signal<string>('');
  actionUserEmail = signal<string>('');
  actionUserRole = signal<string>('');
  actionUserUnits = signal<UserUnitMembershipCard[]>([]);
  actionDetailsLoading = signal<boolean>(false);
  actionDetailsError = signal<string>('');
  promoteConfirmOpen = signal<boolean>(false);
  promoteSuccessOpen = signal<boolean>(false);
  promoteSuccessMessage = signal<string>('');

  isReassignOpen = signal<boolean>(false);
  selectedMember = signal<GroupMemberItem | null>(null);
  reassignSearch = signal<string>('');
  reassignLoading = signal<boolean>(false);
  reassignSaving = signal<boolean>(false);
  reassignError = signal<string>('');
  originalMembershipIds = signal<string[]>([]);
  selectedMembershipIds = signal<string[]>([]);

  isUnitReassignOpen = signal<boolean>(false);
  unitReassignSearch = signal<string>('');
  unitReassignLoading = signal<boolean>(false);
  unitReassignSaving = signal<boolean>(false);
  unitReassignError = signal<string>('');
  originalUnitIds = signal<string[]>([]);
  originalUnitDepartmentIds = signal<string[]>([]);
  unitWizardStep = signal<1 | 2 | 3 | 4>(1);
  unitReassignMode = signal<UnitReassignmentMode>('TRANSFER');
  unitTransferFromId = signal<string>('');
  unitTargetIds = signal<string[]>([]);
  selectedUnitDepartmentIds = signal<string[]>([]);
  unitDepartmentError = signal<string>('');
  unitShowOnlySelected = signal<boolean>(false);
  unitProofFile = signal<File | null>(null);
  unitProofError = signal<string>('');
  unitResultDialogOpen = signal<boolean>(false);
  unitResult = signal<UnitReassignmentResult | null>(null);

  departments = computed(() => {
    const views = this.rawGroups()
      .map(group => this.toDepartmentView(group))
      .filter(group => group.unitId !== null);

    const scoped = views.filter(group => this.isDepartmentVisibleByScope(group));
    return scoped.sort((a, b) => a.displayName.localeCompare(b.displayName));
  });

  unitMenus = computed<UnitMenuView[]>(() => {
    const groupedByUnit = new Map<string, UnitMenuView>();
    for (const department of this.departments()) {
      if (!department.unitId) {
        continue;
      }

      if (!groupedByUnit.has(department.unitId)) {
        groupedByUnit.set(department.unitId, {
          unitId: department.unitId,
          unitName: department.unitName,
          departments: []
        });
      }
      groupedByUnit.get(department.unitId)!.departments.push(department);
    }

    let menus = Array.from(groupedByUnit.values()).map(menu => ({
      ...menu,
      departments: [...menu.departments].sort((a, b) => a.displayName.localeCompare(b.displayName))
    }));

    menus = menus.sort((a, b) => a.unitName.localeCompare(b.unitName));

    const term = this.groupSearch().trim().toLowerCase();
    if (!term) {
      return menus;
    }

    return menus
      .map(menu => {
        const filteredDepartments = menu.departments.filter(department =>
          department.displayName.toLowerCase().includes(term) ||
          department.technicalName.toLowerCase().includes(term)
        );

        return {
          ...menu,
          departments: filteredDepartments
        };
      })
      .filter(menu =>
        menu.unitName.toLowerCase().includes(term) ||
        menu.unitId.toLowerCase().includes(term) ||
        menu.departments.length > 0
      );
  });

  unitListTotalPages = computed<number>(() => {
    const total = this.unitMenus().length;
    return Math.max(1, Math.ceil(total / this.unitListPageSize));
  });

  pagedUnitMenus = computed<UnitMenuView[]>(() => {
    const allUnits = this.unitMenus();
    const start = (this.unitListPage() - 1) * this.unitListPageSize;
    return allUnits.slice(start, start + this.unitListPageSize);
  });

  selectedDepartment = computed(() => {
    const selectedId = this.selectedGroupId();
    if (!selectedId) {
      return null;
    }
    return this.departments().find(group => group.id === selectedId) || null;
  });

  visibleMembers = computed(() => {
    const term = this.memberSearch().trim().toLowerCase();

    const people = this.members().filter(member => member.memberType === 'PERSON');
    if (!term) {
      return people;
    }

    return people.filter(member =>
      member.id.toLowerCase().includes(term) ||
      (member.displayName || '').toLowerCase().includes(term)
    );
  });

  memberListTotalPages = computed<number>(() => {
    const total = this.visibleMembers().length;
    return Math.max(1, Math.ceil(total / this.memberListPageSize));
  });

  pagedVisibleMembers = computed<GroupMemberItem[]>(() => {
    const allMembers = this.visibleMembers();
    const start = (this.memberListPage() - 1) * this.memberListPageSize;
    return allMembers.slice(start, start + this.memberListPageSize);
  });

  selectedUnitMenu = computed<UnitMenuView | null>(() => {
    const selectedUnitId = this.selectedUnitId();
    if (!selectedUnitId) {
      return null;
    }

    return this.unitMenus().find(unit => unit.unitId === selectedUnitId) || null;
  });

  assignableDepartments = computed(() => {
    const selectedDepartment = this.selectedDepartment();
    let assignable = this.departments();

    if (this.viewMode() === 'structure' && selectedDepartment && selectedDepartment.unitId) {
      assignable = assignable.filter(group => group.unitId === selectedDepartment.unitId);
    }

    const term = this.reassignSearch().trim().toLowerCase();
    if (!term) {
      return assignable;
    }

    return assignable.filter(group =>
      group.displayName.toLowerCase().includes(term) ||
      group.unitName.toLowerCase().includes(term)
    );
  });

  availableUnits = computed<UnitOption[]>(() => {
    const units: UnitOption[] = Object.entries(this.unitNameById())
      .map(([id, name]) => ({ id, name: name || id }))
      .sort((a, b) => a.name.localeCompare(b.name));

    if (!this.isUnitAdmin) {
      return units;
    }

    return units.filter(unit => this.managedSiteIdSet.has(unit.id.toUpperCase()));
  });

  selectableUnits = computed<UnitOption[]>(() => {
    const units = this.availableUnits();
    if (this.unitReassignMode() === 'DEPARTMENTS') {
      const originalSet = new Set(this.originalUnitIds());
      return units.filter(unit => originalSet.has(unit.id));
    }
    if (this.unitReassignMode() === 'TRANSFER') {
      const originalSet = new Set(this.originalUnitIds());
      return units.filter(unit => !originalSet.has(unit.id));
    }

    const originalSet = new Set(this.originalUnitIds());
    return units.filter(unit => !originalSet.has(unit.id));
  });

  filteredAvailableUnits = computed(() => {
    let units = this.selectableUnits();

    const originalUnits = new Set(this.originalUnitIds());
    units = [...units].sort((a, b) => {
      const aCurrent = originalUnits.has(a.id) ? 0 : 1;
      const bCurrent = originalUnits.has(b.id) ? 0 : 1;
      if (aCurrent !== bCurrent) {
        return aCurrent - bCurrent;
      }
      return a.name.localeCompare(b.name);
    });

    const term = this.unitReassignSearch().trim().toLowerCase();
    if (!term) {
      return units;
    }

    return units.filter(unit =>
      unit.name.toLowerCase().includes(term) ||
      unit.id.toLowerCase().includes(term)
    );
  });

  currentUnitOptions = computed<UnitOption[]>(() => this.mapUnitIdsToOptions(this.originalUnitIds()));
  selectedTargetUnitOptions = computed<UnitOption[]>(() => this.mapUnitIdsToOptions(this.unitTargetIds()));
  unitDiff = computed<UnitDiff>(() => this.computeUnitDiff());
  departmentAssignmentUnitIds = computed<string[]>(() => {
    const diff = this.unitDiff();
    if (this.unitReassignMode() === 'TRANSFER') {
      return diff.target;
    }
    return diff.target;
  });
  unitDepartmentSections = computed<UnitDepartmentSection[]>(() => {
    const unitIds = this.departmentAssignmentUnitIds();
    if (unitIds.length === 0) {
      return [];
    }

    return unitIds
      .map(unitId => ({
        unitId,
        unitName: this.resolveUnitName(unitId),
        departments: this.departments()
          .filter(department => department.unitId === unitId)
          .sort((a, b) => a.displayName.localeCompare(b.displayName))
      }))
      .sort((a, b) => a.unitName.localeCompare(b.unitName))
      .filter(section => section.departments.length > 0);
  });
  unitDepartmentDiff = computed<DepartmentDiff>(() => this.computeDepartmentDiff());
  unitDepartmentSelectionMessage = computed<string>(() => this.getUnitDepartmentSelectionMessage());
  resultUnitOptions = computed<UnitOption[]>(() => this.mapUnitIdsToOptions(this.unitDiff().final));
  addedUnitOptions = computed<UnitOption[]>(() => this.mapUnitIdsToOptions(this.unitDiff().toAdd));
  removedUnitOptions = computed<UnitOption[]>(() => this.mapUnitIdsToOptions(this.unitDiff().toRemove));

  canProceedFromUnitStep2 = computed<boolean>(() => {
    if (this.unitReassignLoading()) {
      return false;
    }
    if (this.unitReassignMode() === 'DEPARTMENTS') {
      return this.unitTargetIds().length > 0;
    }
    if (this.unitReassignMode() === 'ADD') {
      return this.unitTargetIds().length > 0;
    }
    const originalCount = this.originalUnitIds().length;
    if (originalCount > 1 && !this.unitTransferFromId()) {
      return false;
    }
    return this.unitTargetIds().length === 1;
  });

  canProceedFromUnitStep3 = computed<boolean>(() => {
    if (this.unitReassignLoading()) {
      return false;
    }
    return this.canProceedFromUnitStep2() && !this.unitDepartmentSelectionMessage();
  });

  canSaveUnitWizard = computed<boolean>(() => {
    if (this.unitReassignLoading() || this.unitReassignSaving()) {
      return false;
    }
    if (!this.unitProofFile() || !!this.unitProofError()) {
      return false;
    }
    const diff = this.unitDiff();
    const departmentDiff = this.unitDepartmentDiff();
    return diff.toAdd.length > 0 ||
      diff.toRemove.length > 0 ||
      departmentDiff.toAdd.length > 0 ||
      departmentDiff.toRemove.length > 0;
  });

  globalTotalPages = computed<number>(() => {
    const total = this.globalTotalUsers();
    return Math.max(1, Math.ceil(total / this.globalPageSize));
  });

  constructor(
    private groupService: GroupService,
    private userService: UserService,
    private siteService: SiteService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    const preselectedUnitId = this.route.snapshot.queryParamMap.get('siteId');
    const preselectedUnitName = this.route.snapshot.queryParamMap.get('siteName');
    const preselectedUserId = this.route.snapshot.queryParamMap.get('userId');

    if (preselectedUnitId || preselectedUnitName) {
      this.isUnitContextLocked.set(true);
    }

    if (preselectedUnitId) {
      this.selectedUnitId.set(preselectedUnitId);
    }

    if (preselectedUnitName) {
      this.selectedUnitName.set(preselectedUnitName);
    }

    this.loadUnits();
    this.loadDepartments();
    this.switchMode('global');

    if (this.globalSearchTerm().trim().length === 0) {
      this.globalPage.set(1);
      this.searchGlobalUsersByScope('', 1);
    }

    if (preselectedUserId && preselectedUserId.trim().length > 0) {
      const normalizedUserId = preselectedUserId.trim();
      this.pendingFocusUserId.set(normalizedUserId);
      this.globalSearchTerm.set(normalizedUserId);
      this.searchGlobalUsersByScope(normalizedUserId);
    }
  }

  ngOnDestroy(): void {
    if (this.globalSearchDebounceTimer) {
      clearTimeout(this.globalSearchDebounceTimer);
      this.globalSearchDebounceTimer = null;
    }
  }

  switchMode(mode: 'structure' | 'global'): void {
    if (mode === 'structure') {
      return;
    }

    this.viewMode.set(mode);
    this.actionError.set('');
    this.actionMessage.set('');

    if (mode === 'global') {
      const term = this.globalSearchTerm().trim();
      this.globalPage.set(1);
      this.searchGlobalUsersByScope(term, 1);
    }
  }

  loadDepartments(): void {
    this.groupsLoading.set(true);
    this.groupsError.set('');

    this.groupService.getGroups(false, undefined, 1000, 0).subscribe({
      next: (response) => {
        this.rawGroups.set(response.groups || []);
        this.groupsLoading.set(false);
        this.ensureSelectedGroup();
      },
      error: (err) => {
        console.error('Error cargando departamentos:', err);
        this.groupsError.set('No se pudieron cargar los departamentos.');
        this.groupsLoading.set(false);
      }
    });
  }

  loadUnits(): void {
    this.siteService.getSites(500, 0).subscribe({
      next: (response) => {
        const map: Record<string, string> = {};
        for (const site of response.sites || []) {
          const unitName = this.getSiteDisplayName(site.id, site.title, site.description);
          map[site.id] = unitName;
        }
        this.unitNameById.set(map);
      },
      error: (err) => {
        console.warn('No se pudo cargar el mapa de unidades:', err);
      }
    });
  }

  refreshAll(): void {
    this.loadUnits();
    this.loadDepartments();

    if (this.selectedDepartment()) {
      this.loadMembersForSelectedGroup();
    }

    if (this.viewMode() === 'global') {
      const term = this.globalSearchTerm().trim();
      this.searchGlobalUsersByScope(term, this.globalPage());
    }
  }

  onGroupSearchChange(value: string): void {
    this.groupSearch.set(value);
    this.unitListPage.set(1);
    this.ensureSelectedGroup();
  }

  onMemberSearchChange(value: string): void {
    this.memberSearch.set(value);
    this.memberListPage.set(1);
  }

  goToPreviousUnitPage(): void {
    if (this.unitListPage() > 1) {
      this.unitListPage.set(this.unitListPage() - 1);
    }
  }

  goToNextUnitPage(): void {
    if (this.unitListPage() < this.unitListTotalPages()) {
      this.unitListPage.set(this.unitListPage() + 1);
    }
  }

  goToPreviousMemberPage(): void {
    if (this.memberListPage() > 1) {
      this.memberListPage.set(this.memberListPage() - 1);
    }
  }

  goToNextMemberPage(): void {
    if (this.memberListPage() < this.memberListTotalPages()) {
      this.memberListPage.set(this.memberListPage() + 1);
    }
  }

  onUnitClick(unit: UnitMenuView): void {
    if (this.expandedUnitId() === unit.unitId) {
      this.expandedUnitId.set(null);
    } else {
      this.expandedUnitId.set(unit.unitId);
    }

    this.selectedUnitId.set(unit.unitId);
    this.selectedUnitName.set(unit.unitName);
    this.selectedGroupId.set(null);
    this.memberSearch.set('');
    this.memberListPage.set(1);
    this.loadMembersForSelectedUnit();
  }

  isUnitExpanded(unitId: string): boolean {
    return this.expandedUnitId() === unitId;
  }

  selectDepartment(departmentId: string): void {
    const selectedDepartment = this.departments().find(department => department.id === departmentId);
    if (selectedDepartment && selectedDepartment.unitId) {
      this.expandedUnitId.set(selectedDepartment.unitId);
      this.selectedUnitId.set(selectedDepartment.unitId);
      this.selectedUnitName.set(selectedDepartment.unitName);
    }

    if (this.selectedGroupId() === departmentId) {
      return;
    }

    this.selectedGroupId.set(departmentId);
    this.memberSearch.set('');
    this.memberListPage.set(1);
    this.actionError.set('');
    this.actionMessage.set('');
    this.loadMembersForSelectedGroup();
  }

  loadMembersForSelectedUnit(): void {
    const selectedUnit = this.selectedUnitMenu();
    if (!selectedUnit || selectedUnit.departments.length === 0) {
      this.members.set([]);
      this.membersError.set('No hay departamentos disponibles en esta unidad.');
      return;
    }

    const memberRequests = selectedUnit.departments.map(department =>
      this.groupService.getGroupMembers(department.id, 1000, 0).pipe(
        map(response => response.members || []),
        catchError(() => of([] as GroupMemberItem[]))
      )
    );

    this.membersLoading.set(true);
    this.membersError.set('');

    forkJoin(memberRequests).subscribe({
      next: (allMembers) => {
        const mergedById = new Map<string, GroupMemberItem>();

        for (const memberList of allMembers) {
          for (const member of memberList) {
            if (member.memberType !== 'PERSON') {
              continue;
            }

            if (!mergedById.has(member.id)) {
              mergedById.set(member.id, member);
            }
          }
        }

        const mergedMembers = Array.from(mergedById.values())
          .sort((a, b) => a.id.localeCompare(b.id));

        this.members.set(mergedMembers);
        this.memberNameById.set({});
        this.memberManagedSiteIdsByUser.set({});
        this.enrichMemberNames(mergedMembers);
        this.enrichMemberManagedSites(mergedMembers);
        this.memberListPage.set(1);
        this.membersLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando usuarios de la unidad:', err);
        this.membersError.set('No se pudieron cargar los usuarios de la unidad seleccionada.');
        this.membersLoading.set(false);
      }
    });
  }

  loadMembersForSelectedGroup(): void {
    const department = this.selectedDepartment();
    if (!department) {
      this.members.set([]);
      return;
    }

    this.membersLoading.set(true);
    this.membersError.set('');

    this.groupService.getGroupMembers(department.id, 1000, 0).subscribe({
      next: (response) => {
        this.members.set(response.members || []);
        this.memberNameById.set({});
        this.memberManagedSiteIdsByUser.set({});
        this.enrichMemberNames(response.members || []);
        this.enrichMemberManagedSites(response.members || []);
        this.membersLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando usuarios del departamento:', err);
        this.membersError.set('No se pudieron cargar los usuarios del departamento.');
        this.membersLoading.set(false);
      }
    });
  }

  onGlobalSearchChange(value: string): void {
    this.globalSearchTerm.set(value);

    const term = value.trim();
    if (term.length === 0) {
      this.globalSearchError.set('');
      this.globalPage.set(1);
      this.scheduleGlobalSearch('', 1);
      return;
    }

    this.globalSearchError.set('');
    this.globalPage.set(1);
    this.scheduleGlobalSearch(term, 1);
  }

  goToPreviousGlobalPage(): void {
    if (this.globalPage() <= 1 || this.globalSearching()) {
      return;
    }
    const previousPage = this.globalPage() - 1;
    this.globalPage.set(previousPage);
    this.searchGlobalUsersByScope(this.globalSearchTerm().trim(), previousPage);
  }

  goToNextGlobalPage(): void {
    if (this.globalSearching()) {
      return;
    }
    const nextPage = this.globalPage() + 1;
    if (nextPage > this.globalTotalPages()) {
      return;
    }
    this.globalPage.set(nextPage);
    this.searchGlobalUsersByScope(this.globalSearchTerm().trim(), nextPage);
  }

  openUserActionsDrawer(member: GroupMemberItem, unitId: string | null, unitName: string): void {
    this.closeReassignDrawer();
    this.closeUnitReassignDrawer();
    this.closeUnitResultDialog();
    this.closePromoteConfirmDialog();
    this.closePromoteSuccessDialog();
    this.actionError.set('');
    this.actionMessage.set('');
    this.actionMember.set(member);
    this.actionContextUnitId.set(unitId);
    this.actionContextUnitName.set(unitName || '');
    this.loadActionUserDetails(member.id);
    this.isUserActionsOpen.set(true);
  }

  closeUserActionsDrawer(): void {
    this.isUserActionsOpen.set(false);
    this.actionMember.set(null);
    this.actionContextUnitId.set(null);
    this.actionContextUnitName.set('');
    this.actionUserFullName.set('');
    this.actionUserEmail.set('');
    this.actionUserRole.set('');
    this.actionUserUnits.set([]);
    this.actionDetailsLoading.set(false);
    this.actionDetailsError.set('');
  }

  openUnitReassignmentFromActions(): void {
    const member = this.actionMember();
    if (!member) {
      return;
    }
    this.closeUserActionsDrawer();
    this.openUnitReassignDrawer(member);
  }


  goToUnitDetailFromAction(siteId: string, siteName: string): void {
    this.closeUserActionsDrawer();
    this.router.navigate(['/unidades', siteId], { queryParams: { siteName, tab: 'departamentos' } });
  }

  closePromoteConfirmDialog(): void {
    this.promoteConfirmOpen.set(false);
  }

  closePromoteSuccessDialog(): void {
    this.promoteSuccessOpen.set(false);
    this.promoteSuccessMessage.set('');
  }

  confirmPromoteFromUserActions(): void {
    const member = this.actionMember();
    const unitId = this.actionContextUnitId();
    const unitName = this.actionContextUnitName();
    if (!member || !unitId || !this.isGlobalAdmin) {
      this.closePromoteConfirmDialog();
      return;
    }

    this.busyPromoteUserId.set(member.id);
    this.actionError.set('');
    this.actionMessage.set('');

    this.siteService.assignUnitAdmin(unitId, member.id)
      .pipe(finalize(() => this.busyPromoteUserId.set(null)))
      .subscribe({
        next: () => {
          const successMessage = 'Usuario ' + member.id + ' asignado como Administrador de la Unidad ' + unitName + '.';
          this.actionMessage.set(successMessage);
          this.promoteSuccessMessage.set(successMessage);
          this.promoteConfirmOpen.set(false);
          this.promoteSuccessOpen.set(true);
          this.closeUserActionsDrawer();
          this.refreshMemberManagedSitesForUser(member.id);
          if (this.viewMode() === 'global') {
            const term = this.globalSearchTerm().trim();
            this.searchGlobalUsersByScope(term, this.globalPage());
          }
        },
        error: (err) => {
          console.error('Error asignando administrador de unidad:', err);
          this.actionError.set('No se pudo asignar el rol de Administrador de Unidad.');
          this.promoteConfirmOpen.set(false);
        }
      });
  }

  openReassignDrawer(member: GroupMemberItem): void {
    this.closeUserActionsDrawer();
    this.closePromoteConfirmDialog();
    this.closePromoteSuccessDialog();
    this.closeUnitReassignDrawer();
    this.closeUnitResultDialog();
    this.selectedMember.set(member);
    this.isReassignOpen.set(true);
    this.reassignSearch.set('');
    this.reassignError.set('');
    this.originalMembershipIds.set([]);
    this.selectedMembershipIds.set([]);
    this.loadUserMemberships(member.id);
  }

  closeReassignDrawer(): void {
    this.isReassignOpen.set(false);
    this.selectedMember.set(null);
    this.reassignSearch.set('');
    this.reassignError.set('');
    this.originalMembershipIds.set([]);
    this.selectedMembershipIds.set([]);
  }

  openUnitReassignDrawer(member: GroupMemberItem): void {
    this.closeUserActionsDrawer();
    this.closePromoteConfirmDialog();
    this.closePromoteSuccessDialog();
    this.closeReassignDrawer();
    this.closeUnitResultDialog();
    this.selectedMember.set(member);
    this.isUnitReassignOpen.set(true);
    this.unitWizardStep.set(1);
    this.unitReassignMode.set('TRANSFER');
    this.unitTransferFromId.set('');
    this.unitReassignSearch.set('');
    this.unitReassignError.set('');
    this.unitDepartmentError.set('');
    this.unitShowOnlySelected.set(false);
    this.unitProofFile.set(null);
    this.unitProofError.set('');
    this.originalUnitIds.set([]);
    this.originalUnitDepartmentIds.set([]);
    this.unitTargetIds.set([]);
    this.selectedUnitDepartmentIds.set([]);
    this.loadUserUnitsMemberships(member.id);
    this.loadUserDepartmentMemberships(member.id);
  }

  closeUnitReassignDrawer(): void {
    this.isUnitReassignOpen.set(false);
    this.selectedMember.set(null);
    this.unitWizardStep.set(1);
    this.unitReassignMode.set('TRANSFER');
    this.unitTransferFromId.set('');
    this.unitReassignSearch.set('');
    this.unitReassignError.set('');
    this.unitDepartmentError.set('');
    this.unitShowOnlySelected.set(false);
    this.unitProofFile.set(null);
    this.unitProofError.set('');
    this.originalUnitIds.set([]);
    this.originalUnitDepartmentIds.set([]);
    this.unitTargetIds.set([]);
    this.selectedUnitDepartmentIds.set([]);
  }

  loadUserMemberships(userId: string): void {
    this.reassignLoading.set(true);
    this.reassignError.set('');

    this.userService.getUserGroups(userId).subscribe({
      next: (response) => {
        const assignableSet = new Set(this.getAssignableDepartmentIds());
        const scopedMemberships = (response.groups || [])
          .map(group => group.id)
          .filter(groupId => assignableSet.has(groupId));

        this.originalMembershipIds.set(scopedMemberships);
        this.selectedMembershipIds.set([...scopedMemberships]);
        this.reassignLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando membresias del usuario:', err);
        this.reassignError.set('No se pudieron cargar los grupos del usuario.');
        this.reassignLoading.set(false);
      }
    });
  }

  loadUserUnitsMemberships(userId: string): void {
    this.unitReassignLoading.set(true);
    this.unitReassignError.set('');

    this.userService.getUserSites(userId).subscribe({
      next: (response) => {
        const availableUnits = this.availableUnits();
        const allowedUnits = availableUnits.length > 0 ? new Set(availableUnits.map(unit => unit.id)) : null;
        const scopedMemberships = this.normalizeUnitIds((response.sites || [])
          .map(site => site.siteId)
          .filter(siteId => !allowedUnits || allowedUnits.has(siteId)));

        this.originalUnitIds.set(scopedMemberships);
        if (this.unitReassignMode() === 'TRANSFER') {
          this.unitTransferFromId.set(scopedMemberships.length > 0 ? scopedMemberships[0] : '');
          this.unitTargetIds.set([]);
        } else {
          this.unitTransferFromId.set(scopedMemberships.length > 0 ? scopedMemberships[0] : '');
          this.unitTargetIds.set([...scopedMemberships]);
        }
        this.syncSelectedDepartmentsForCurrentTargets();
        this.unitReassignLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando unidades del usuario:', err);
        this.unitReassignError.set('No se pudieron cargar las unidades del usuario.');
        this.unitReassignLoading.set(false);
      }
    });
  }

  loadUserDepartmentMemberships(userId: string): void {
    this.userService.getUserGroups(userId).subscribe({
      next: (response) => {
        const knownDepartments = new Set(this.departments().map(department => department.id));
        const scopedMemberships = this.normalizeUnitIds((response.groups || [])
          .map(group => group.id)
          .filter(groupId => knownDepartments.has(groupId)));

        this.originalUnitDepartmentIds.set(scopedMemberships);
        this.syncSelectedDepartmentsForCurrentTargets();
      },
      error: (err) => {
        console.error('Error cargando departamentos del usuario para reasignacion de unidad:', err);
      }
    });
  }

  isGroupSelected(groupId: string): boolean {
    return this.selectedMembershipIds().includes(groupId);
  }

  toggleGroupSelection(groupId: string, checked: boolean): void {
    const current = new Set(this.selectedMembershipIds());
    if (checked) {
      current.add(groupId);
    } else {
      current.delete(groupId);
    }
    this.selectedMembershipIds.set(Array.from(current));
  }

  isUnitSelected(unitId: string): boolean {
    return this.unitTargetIds().includes(unitId);
  }

  toggleUnitSelection(unitId: string, checked: boolean): void {
    this.unitDepartmentError.set('');

    if (this.unitReassignMode() === 'TRANSFER') {
      this.unitTargetIds.set(checked ? [unitId] : []);
      this.syncSelectedDepartmentsForCurrentTargets();
      return;
    }

    const current = new Set(this.unitTargetIds());
    if (checked) {
      current.add(unitId);
    } else {
      current.delete(unitId);
    }
    this.unitTargetIds.set(Array.from(current));
    this.syncSelectedDepartmentsForCurrentTargets();
  }

  setTransferFromUnit(unitId: string): void {
    this.unitTransferFromId.set(unitId);
    if (this.unitReassignMode() === 'TRANSFER' && this.unitTargetIds().includes(unitId)) {
      this.unitTargetIds.set([]);
    }
    this.syncSelectedDepartmentsForCurrentTargets();
    this.unitDepartmentError.set('');
  }

  isUnitDepartmentSelected(departmentId: string): boolean {
    return this.selectedUnitDepartmentIds().includes(departmentId);
  }

  toggleUnitDepartmentSelection(departmentId: string, checked: boolean): void {
    const current = new Set(this.selectedUnitDepartmentIds());
    if (checked) {
      current.add(departmentId);
    } else {
      current.delete(departmentId);
    }
    this.selectedUnitDepartmentIds.set(Array.from(current));
    this.unitDepartmentError.set('');
  }

  selectUnitReassignMode(mode: UnitReassignmentMode): void {
    if (this.unitReassignMode() === mode) {
      return;
    }

    const previousFinal = [...this.unitDiff().final];
    this.unitReassignMode.set(mode);
    this.unitReassignError.set('');
    this.unitDepartmentError.set('');

    if (mode === 'TRANSFER') {
      const original = this.normalizeUnitIds(this.originalUnitIds());
      this.unitTransferFromId.set(original.length > 0 ? original[0] : '');
      this.unitTargetIds.set([]);
      this.syncSelectedDepartmentsForCurrentTargets();
      return;
    }

    if (mode === 'DEPARTMENTS') {
      const original = this.normalizeUnitIds(this.originalUnitIds());
      this.unitTransferFromId.set(original.length > 0 ? original[0] : '');
      this.unitTargetIds.set([...original]);
      this.syncSelectedDepartmentsForCurrentTargets();
      return;
    }

    const originalSet = new Set(this.originalUnitIds());
    const original = this.normalizeUnitIds(this.originalUnitIds());
    this.unitTransferFromId.set(original.length > 0 ? original[0] : '');
    this.unitTargetIds.set(previousFinal.filter(unitId => !originalSet.has(unitId)));
    this.syncSelectedDepartmentsForCurrentTargets();
  }

  goToUnitWizardStep(step: 1 | 2 | 3 | 4): void {
    if (step === this.unitWizardStep()) {
      return;
    }

    if (step === 2 && this.unitReassignLoading()) {
      return;
    }

    if (step === 3 && !this.canProceedFromUnitStep2()) {
      return;
    }

    if (step === 4 && !this.canProceedFromUnitStep3()) {
      this.unitDepartmentError.set(this.unitDepartmentSelectionMessage());
      return;
    }

    this.unitWizardStep.set(step);
  }

  nextUnitWizardStep(): void {
    const currentStep = this.unitWizardStep();
    if (currentStep === 1) {
      this.unitWizardStep.set(2);
      return;
    }

    if (currentStep === 2 && this.canProceedFromUnitStep2()) {
      this.unitWizardStep.set(3);
      return;
    }

    if (currentStep === 3 && this.canProceedFromUnitStep3()) {
      this.unitWizardStep.set(4);
      this.unitDepartmentError.set('');
      return;
    }

    if (currentStep === 3) {
      this.unitDepartmentError.set(this.unitDepartmentSelectionMessage());
    }
  }

  previousUnitWizardStep(): void {
    const currentStep = this.unitWizardStep();
    if (currentStep === 2) {
      this.unitWizardStep.set(1);
      return;
    }
    if (currentStep === 3) {
      this.unitWizardStep.set(2);
      return;
    }
    if (currentStep === 4) {
      this.unitWizardStep.set(3);
    }
  }

  onUnitProofFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files && input.files.length > 0 ? input.files[0] : null;

    if (!file) {
      this.unitProofFile.set(null);
      this.unitProofError.set('Debes adjuntar un PDF justificante.');
      return;
    }

    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
      this.unitProofFile.set(null);
      this.unitProofError.set('El justificante debe ser un archivo PDF.');
      return;
    }

    this.unitProofFile.set(file);
    this.unitProofError.set('');
  }

  clearUnitProofFile(): void {
    this.unitProofFile.set(null);
    this.unitProofError.set('');
  }

  closeUnitResultDialog(): void {
    this.unitResultDialogOpen.set(false);
    this.unitResult.set(null);
  }

  getUnitModeLabel(mode: UnitReassignmentMode): string {
    if (mode === 'TRANSFER') {
      return 'Traslado de unidad';
    }
    if (mode === 'DEPARTMENTS') {
      return 'Reasignacion de departamentos';
    }
    return 'Alta en unidad adicional';
  }

  saveReassignment(): void {
    const member = this.selectedMember();
    if (!member) {
      return;
    }

    const allowedIds = new Set(this.getAssignableDepartmentIds());
    const original = this.originalMembershipIds().filter(groupId => allowedIds.has(groupId));
    const selected = this.selectedMembershipIds().filter(groupId => allowedIds.has(groupId));

    const toAdd = selected.filter(groupId => !original.includes(groupId));
    const toRemove = original.filter(groupId => !selected.includes(groupId));

    if (toAdd.length === 0 && toRemove.length === 0) {
      this.closeReassignDrawer();
      return;
    }

    const requests = [
      ...toAdd.map(groupId => this.groupService.addUserToGroup(groupId, member.id)),
      ...toRemove.map(groupId => this.groupService.removeUserFromGroup(groupId, member.id))
    ];

    this.reassignSaving.set(true);
    this.reassignError.set('');

    forkJoin(requests)
      .pipe(finalize(() => this.reassignSaving.set(false)))
      .subscribe({
        next: () => {
          this.actionError.set('');
          this.actionMessage.set('Reasignacion de departamentos actualizada para ' + member.id + '.');
          this.closeReassignDrawer();
          if (this.viewMode() === 'structure') {
            this.loadMembersForSelectedGroup();
          } else {
            const term = this.globalSearchTerm().trim();
            this.searchGlobalUsersByScope(term, this.globalPage());
          }
        },
        error: (err) => {
          console.error('Error guardando reasignacion de grupos:', err);
          this.reassignError.set('No se pudieron guardar los cambios de departamentos.');
        }
      });
  }

  saveUnitReassignment(): void {
    const member = this.selectedMember();
    if (!member) {
      return;
    }

    const proofFile = this.unitProofFile();
    if (!proofFile) {
      this.unitProofError.set('Debes adjuntar un PDF justificante.');
      return;
    }

    const departmentSelectionMessage = this.unitDepartmentSelectionMessage();
    if (departmentSelectionMessage) {
      this.unitDepartmentError.set(departmentSelectionMessage);
      return;
    }

    const unitDiff = this.unitDiff();
    const departmentDiff = this.unitDepartmentDiff();
    const hasUnitChanges = unitDiff.toAdd.length > 0 || unitDiff.toRemove.length > 0;
    const hasDepartmentChanges = departmentDiff.toAdd.length > 0 || departmentDiff.toRemove.length > 0;
    if (!hasUnitChanges && !hasDepartmentChanges) {
      this.unitReassignError.set('No hay cambios de unidad/departamentos para guardar.');
      return;
    }

    const siteRequests = [
      ...unitDiff.toAdd.map(siteId => this.siteService.assignSiteUser(siteId, member.id)),
      ...unitDiff.toRemove.map(siteId => this.siteService.removeSiteUser(siteId, member.id))
    ];
    const departmentRequests = [
      ...departmentDiff.toAdd.map(groupId => this.groupService.addUserToGroup(groupId, member.id)),
      ...departmentDiff.toRemove.map(groupId => this.groupService.removeUserFromGroup(groupId, member.id))
    ];
    const updateMemberships$ = siteRequests.length > 0 ? forkJoin(siteRequests) : of([]);
    const updateDepartments$ = departmentRequests.length > 0 ? forkJoin(departmentRequests) : of([]);

    this.unitReassignSaving.set(true);
    this.unitReassignError.set('');
    this.unitDepartmentError.set('');
    this.unitProofError.set('');

    let siteMembershipsApplied = false;
    let departmentMembershipsApplied = false;

    updateMemberships$
      .pipe(
        switchMap(() => {
          siteMembershipsApplied = true;
          return updateDepartments$;
        }),
        switchMap(() => {
          departmentMembershipsApplied = true;
          return this.userService.uploadUnitReassignmentProof(member.id, {
            file: proofFile,
            operationMode: this.unitReassignMode(),
            fromUnitIds: unitDiff.original,
            targetUnitIds: unitDiff.target,
            finalUnitIds: unitDiff.final,
            transferFromUnitId: unitDiff.transferFrom || undefined
          });
        }),
        finalize(() => this.unitReassignSaving.set(false))
      )
      .subscribe({
        next: (proofResponse: UnitReassignmentProofResponse) => {
          const result: UnitReassignmentResult = {
            userId: member.id,
            mode: this.unitReassignMode(),
            previousUnits: this.mapUnitIdsToOptions(unitDiff.original),
            targetUnits: this.mapUnitIdsToOptions(unitDiff.target),
            finalUnits: this.mapUnitIdsToOptions(unitDiff.final),
            addedUnits: this.mapUnitIdsToOptions(unitDiff.toAdd),
            removedUnits: this.mapUnitIdsToOptions(unitDiff.toRemove),
            finalDepartments: this.mapDepartmentIdsToLabels(departmentDiff.desired),
            addedDepartments: this.mapDepartmentIdsToLabels(departmentDiff.toAdd),
            removedDepartments: this.mapDepartmentIdsToLabels(departmentDiff.toRemove),
            proofFileName: proofResponse.originalFileName || proofFile.name,
            proofStoredFileName: proofResponse.storedFileName || ''
          };

          this.actionError.set('');
          this.actionMessage.set('Reasignacion de unidad y departamentos actualizada para ' + member.id + '.');
          this.closeUnitReassignDrawer();
          this.unitResult.set(result);
          this.unitResultDialogOpen.set(true);

          if (this.viewMode() === 'structure') {
            this.loadMembersForSelectedGroup();
          } else {
            const term = this.globalSearchTerm().trim();
            this.searchGlobalUsersByScope(term, this.globalPage());
          }
        },
        error: (err) => {
          console.error('Error guardando reasignacion de unidad:', err);
          if (siteMembershipsApplied && departmentMembershipsApplied) {
            this.unitReassignError.set(
              'Los cambios de unidad/departamentos se aplicaron, pero no se pudo guardar el justificante PDF.'
            );
          } else if (siteMembershipsApplied) {
            this.unitReassignError.set(
              'La unidad se actualizo, pero fallo la asignacion de departamentos.'
            );
          } else {
            this.unitReassignError.set('No se pudieron guardar los cambios de unidad/departamentos.');
          }
        }
      });
  }

  unitModeDescription(mode: UnitReassignmentMode): string {
    if (mode === 'TRANSFER') {
      return 'Traslada al usuario desde una unidad origen hacia una unidad destino.';
    }
    if (mode === 'DEPARTMENTS') {
      return 'Mantiene sus unidades y solo se actualizan departamentos en las unidades elegidas.';
    }
    return 'El usuario mantiene sus unidades actuales y se agrega a nuevas.';
  }

  hasUnitChanges(): boolean {
    const unitDiff = this.unitDiff();
    const departmentDiff = this.unitDepartmentDiff();
    return unitDiff.toAdd.length > 0 ||
      unitDiff.toRemove.length > 0 ||
      departmentDiff.toAdd.length > 0 ||
      departmentDiff.toRemove.length > 0;
  }

  isPromoting(memberId: string): boolean {
    return this.busyPromoteUserId() === memberId;
  }

  isMemberUnitAdmin(memberId: string, unitId: string | null): boolean {
    if (!unitId) {
      return false;
    }

    const managedSiteIds = this.memberManagedSiteIdsByUser()[memberId] || [];
    return managedSiteIds.some(siteId => siteId.toUpperCase() === unitId.toUpperCase());
  }

  getMemberName(member: GroupMemberItem): string {
    const resolvedName = this.memberNameById()[member.id];
    if (resolvedName && resolvedName.trim().length > 0) {
      return resolvedName.trim();
    }

    const label = (member.displayName || '').trim();
    if (!label) {
      return '';
    }

    if (label.toUpperCase() === member.id.toUpperCase()) {
      return '';
    }

    return label;
  }

  getPrimaryUnitIdForGlobalRow(row: GlobalUserRow): string | null {
    return null;
  }

  getPrimaryUnitNameForGlobalRow(row: GlobalUserRow): string {
    const unitId = this.getPrimaryUnitIdForGlobalRow(row);
    return this.resolveUnitName(unitId);
  }

  trackByDepartment(index: number, item: DepartmentView): string {
    return item.id;
  }

  trackByUnit(index: number, item: UnitMenuView): string {
    return item.unitId;
  }

  trackByUnitOption(index: number, item: UnitOption): string {
    return item.id;
  }

  trackByMember(index: number, item: GroupMemberItem): string {
    return item.id;
  }

  trackByGlobalUser(index: number, item: GlobalUserRow): string {
    return item.member.id;
  }

  private mapUnitIdsToOptions(unitIds: string[]): UnitOption[] {
    const normalizedIds = this.normalizeUnitIds(unitIds);
    return normalizedIds
      .map(unitId => ({
        id: unitId,
        name: this.resolveUnitName(unitId)
      }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  private mapDepartmentIdsToLabels(departmentIds: string[]): string[] {
    const departmentsById = new Map(this.departments().map(department => [department.id, department]));
    const labels: string[] = [];
    const seen = new Set<string>();

    for (const departmentId of this.normalizeUnitIds(departmentIds)) {
      const resolved = departmentsById.get(departmentId);
      let label = '';

      if (resolved) {
        label = resolved.displayName + ' (' + resolved.unitName + ')';
      } else {
        const technicalName = this.toTechnicalName(departmentId);
        const unitId = this.extractUnitId(technicalName);
        const departmentName = this.formatDepartmentName(technicalName);
        label = unitId
          ? departmentName + ' (' + this.resolveUnitName(unitId) + ')'
          : (departmentName || departmentId);
      }

      if (!label || seen.has(label)) {
        continue;
      }

      seen.add(label);
      labels.push(label);
    }

    return labels.sort((a, b) => a.localeCompare(b));
  }

  private normalizeUnitIds(unitIds: string[]): string[] {
    const normalized: string[] = [];
    for (const unitId of unitIds || []) {
      const clean = (unitId || '').trim();
      if (!clean) {
        continue;
      }
      if (!normalized.includes(clean)) {
        normalized.push(clean);
      }
    }
    return normalized;
  }

  private computeUnitDiff(): UnitDiff {
    const availableUnits = this.availableUnits();
    const allowedUnits = availableUnits.length > 0 ? new Set(availableUnits.map(unit => unit.id)) : null;
    const original = this.normalizeUnitIds(this.originalUnitIds().filter(unitId => !allowedUnits || allowedUnits.has(unitId)));
    const rawTarget = this.normalizeUnitIds(this.unitTargetIds().filter(unitId => !allowedUnits || allowedUnits.has(unitId)));

    let target = rawTarget;
    let final = rawTarget;
    let transferFrom: string | null = null;

    if (this.unitReassignMode() === 'DEPARTMENTS') {
      const originalSet = new Set(original);
      target = rawTarget.filter(unitId => originalSet.has(unitId));
      final = original;
    } else if (this.unitReassignMode() === 'ADD') {
      const originalSet = new Set(original);
      target = rawTarget.filter(unitId => !originalSet.has(unitId));
      final = this.normalizeUnitIds(original.concat(target));
    } else {
      target = rawTarget.length > 0 ? [rawTarget[0]] : [];
      const selectedFrom = (this.unitTransferFromId() || '').trim();
      transferFrom = original.includes(selectedFrom)
        ? selectedFrom
        : (original.length === 1 ? original[0] : null);

      if (target.length === 0) {
        final = original;
      } else if (transferFrom) {
        final = this.normalizeUnitIds(original.filter(unitId => unitId !== transferFrom).concat(target));
      } else {
        final = target;
      }
    }

    const toAdd = final.filter(unitId => !original.includes(unitId));
    const toRemove = original.filter(unitId => !final.includes(unitId));

    return {
      original,
      target,
      final,
      toAdd,
      toRemove,
      transferFrom
    };
  }

  private computeDepartmentDiff(): DepartmentDiff {
    const selectedDepartmentIds = new Set(this.departments().map(department => department.id));
    const original = this.normalizeUnitIds(
      this.originalUnitDepartmentIds().filter(departmentId => selectedDepartmentIds.has(departmentId))
    );
    const unitDiff = this.unitDiff();
    const assignmentUnitIds = this.departmentAssignmentUnitIds();
    const assignmentUnitIdSet = new Set(assignmentUnitIds);
    const removedUnitIdSet = new Set(unitDiff.toRemove);

    const originalOutsideChangedUnits = original.filter(departmentId => {
      const department = this.departments().find(item => item.id === departmentId);
      if (!department || !department.unitId) {
        return false;
      }
      return !assignmentUnitIdSet.has(department.unitId) && !removedUnitIdSet.has(department.unitId);
    });

    const selectedInAssignmentUnits = this.normalizeUnitIds(
      this.selectedUnitDepartmentIds().filter(departmentId => {
        const department = this.departments().find(item => item.id === departmentId);
        return !!department && !!department.unitId && assignmentUnitIdSet.has(department.unitId);
      })
    );

    const desired = this.normalizeUnitIds(originalOutsideChangedUnits.concat(selectedInAssignmentUnits));
    const toAdd = desired.filter(departmentId => !original.includes(departmentId));
    const toRemove = original.filter(departmentId => !desired.includes(departmentId));

    return {
      desired,
      toAdd,
      toRemove
    };
  }

  private getUnitDepartmentSelectionMessage(): string {
    const sections = this.unitDepartmentSections();
    if (sections.length === 0) {
      return '';
    }

    const selectedSet = new Set(this.selectedUnitDepartmentIds());
    const missingUnits = sections
      .filter(section => !section.departments.some(department => selectedSet.has(department.id)))
      .map(section => section.unitName);

    if (missingUnits.length === 0) {
      return '';
    }

    if (this.unitReassignMode() === 'TRANSFER') {
      return 'Selecciona al menos un departamento en la unidad destino.';
    }

    if (this.unitReassignMode() === 'DEPARTMENTS') {
      return 'Selecciona al menos un departamento en cada unidad elegida: ' + missingUnits.join(', ') + '.';
    }

    return 'Selecciona al menos un departamento en cada unidad destino: ' + missingUnits.join(', ') + '.';
  }

  private syncSelectedDepartmentsForCurrentTargets(): void {
    const targetUnitIds = new Set(this.departmentAssignmentUnitIds());
    const current = this.selectedUnitDepartmentIds();
    const originalByTargetUnit = this.originalUnitDepartmentIds().filter(departmentId => {
      const department = this.departments().find(item => item.id === departmentId);
      return !!department && !!department.unitId && targetUnitIds.has(department.unitId);
    });

    const merged = this.normalizeUnitIds(current.concat(originalByTargetUnit));
    const allowedDepartmentIds = new Set(
      this.departments()
        .filter(department => !!department.unitId && targetUnitIds.has(department.unitId))
        .map(department => department.id)
    );

    const scoped = merged.filter(departmentId => allowedDepartmentIds.has(departmentId));
    this.selectedUnitDepartmentIds.set(scoped);
  }

  private ensureSelectedGroup(): void {
    const selected = this.selectedGroupId();
    const groups = this.getVisibleDepartmentsFromMenu();

    if (groups.length === 0) {
      this.selectedGroupId.set(null);
      this.expandedUnitId.set(null);
      this.members.set([]);
      return;
    }

    const preferredUnit = this.resolvePreferredUnitFromNavigation();
    if (preferredUnit) {
        this.expandedUnitId.set(preferredUnit.unitId);
        this.selectedUnitId.set(preferredUnit.unitId);
        this.selectedUnitName.set(preferredUnit.unitName);

        if (!selected || !preferredUnit.departments.some(department => department.id === selected)) {
          this.selectedGroupId.set(null);
          this.memberSearch.set('');
          this.memberListPage.set(1);
          this.loadMembersForSelectedUnit();
          return;
        }
    }

    const exists = selected && groups.some(group => group.id === selected);
    if (!exists) {
      this.selectedGroupId.set(groups[0].id);
      if (groups[0].unitId) {
        this.expandedUnitId.set(groups[0].unitId);
        this.selectedUnitId.set(groups[0].unitId);
        this.selectedUnitName.set(groups[0].unitName);
      }
      this.loadMembersForSelectedGroup();
      return;
    }

    const selectedDepartment = groups.find(group => group.id === selected);
    if (selectedDepartment && selectedDepartment.unitId) {
      this.expandedUnitId.set(selectedDepartment.unitId);
      this.selectedUnitId.set(selectedDepartment.unitId);
      this.selectedUnitName.set(selectedDepartment.unitName);
    }
  }

  private scheduleGlobalSearch(term: string, page: number): void {
    if (this.globalSearchDebounceTimer) {
      clearTimeout(this.globalSearchDebounceTimer);
      this.globalSearchDebounceTimer = null;
    }

    this.globalSearchDebounceTimer = setTimeout(() => {
      this.searchGlobalUsersByScope(term, page);
    }, 250);
  }

  private searchGlobalUsersByScope(term: string, page: number = 1): void {
    this.globalSearching.set(true);
    this.globalSearchError.set('');

    if (this.isGlobalAdmin) {
      const currentPage = Math.max(1, page);
      const skipCount = (currentPage - 1) * this.globalPageSize;
      this.userService.getUsers(this.globalPageSize, skipCount, undefined, term || undefined).subscribe({
        next: (response: UserListResponse) => {
          const rows = this.mapUsersToGlobalRows(response.users || []);
          this.globalResults.set(rows);
          this.globalTotalUsers.set(response.totalUsers || rows.length);
          this.globalHasMore.set(!!response.hasMore);
          this.globalPage.set(currentPage);
          this.globalSearching.set(false);

          const pendingUserId = this.pendingFocusUserId();
          if (pendingUserId) {
            const pendingRow = rows.find(row => row.member.id.toUpperCase() === pendingUserId.toUpperCase());
            if (pendingRow) {
              this.pendingFocusUserId.set('');
              this.openUserActionsDrawer(pendingRow.member, null, '');
              this.clearTransientQueryParams();
            }
          }
        },
        error: (err) => {
          console.error('Error en busqueda global:', err);
          this.globalResults.set([]);
          this.globalTotalUsers.set(0);
          this.globalHasMore.set(false);
          this.globalSearchError.set('No se pudo completar la busqueda global de usuarios.');
          this.globalSearching.set(false);
        }
      });
      return;
    }

    if (this.isUnitAdmin) {
      const targetSiteIds = this.authService.getManagedSiteIds();
      if (targetSiteIds.length === 0) {
        this.globalResults.set([]);
        this.globalTotalUsers.set(0);
        this.globalHasMore.set(false);
        this.globalSearchError.set('No hay sitios disponibles para buscar usuarios.');
        this.globalSearching.set(false);
        return;
      }

      const requests = targetSiteIds.map(siteId => this.userService.getUsers(500, 0, siteId));
      forkJoin(requests).subscribe({
        next: (responses: UserListResponse[]) => {
          const mergedById = new Map<string, User>();
          for (const response of responses) {
            for (const user of response.users || []) {
              if (!mergedById.has(user.id)) {
                mergedById.set(user.id, user);
              }
            }
          }

          const filteredUsers = Array.from(mergedById.values()).filter(user => this.userMatchesTerm(user, term));
          const rows = this.mapUsersToGlobalRows(filteredUsers);
          this.globalResults.set(rows);
          this.globalTotalUsers.set(rows.length);
          this.globalHasMore.set(false);
          this.globalPage.set(1);
          this.globalSearching.set(false);
        },
        error: (err) => {
          console.error('Error en busqueda por sitios:', err);
          this.globalResults.set([]);
          this.globalTotalUsers.set(0);
          this.globalHasMore.set(false);
          this.globalSearchError.set('No se pudo completar la busqueda de usuarios en los sitios gestionados.');
          this.globalSearching.set(false);
        }
      });
      return;
    }

    this.globalResults.set([]);
    this.globalTotalUsers.set(0);
    this.globalHasMore.set(false);
    this.globalSearchError.set('Tu usuario no tiene permisos para buscar usuarios.');
    this.globalSearching.set(false);
  }

  private mapUsersToGlobalRows(users: User[]): GlobalUserRow[] {
    return [...(users || [])]
      .sort((left, right) => (left.id || '').localeCompare(right.id || ''))
      .map(user => ({
        member: {
          id: user.id,
          displayName: this.getUserFullName(user),
          memberType: 'PERSON'
        },
        email: user.email || '—',
        enabled: !!user.enabled
      }));
  }

  private getUserDepartmentInfo(groups: Group[]): { labels: string[]; ids: string[] } {
    const labels: string[] = [];
    const ids: string[] = [];
    const seenLabels = new Set<string>();

    for (const group of groups || []) {
      if (!group || !group.id) {
        continue;
      }

      const parsed = this.parseActionGroupForDisplay(group.id);
      if (!parsed || parsed.type !== 'department') {
        continue;
      }

      const label = parsed.label + ' (' + this.resolveUnitName(parsed.unitId) + ')';

      if (!seenLabels.has(label)) {
        seenLabels.add(label);
        labels.push(label);
      }

      if (!ids.includes(group.id)) {
        ids.push(group.id);
      }
    }

    return { labels, ids };
  }

  private getUserSiteLabels(sites: UserSiteMembership[]): string[] {
    const labels: string[] = [];
    const seen = new Set<string>();

    for (const site of sites || []) {
      if (!site) {
        continue;
      }

      const label = (site.siteTitle && site.siteTitle.trim().length > 0)
        ? site.siteTitle.trim()
        : site.siteId;

      if (!seen.has(label)) {
        seen.add(label);
        labels.push(label);
      }
    }

    return labels;
  }

  private getUserAdminSiteLabels(sites: UserSiteMembership[]): string[] {
    const labels: string[] = [];
    const seen = new Set<string>();

    for (const site of sites || []) {
      if (!site || !this.isSiteManagerRole(site.role)) {
        continue;
      }

      const label = (site.siteTitle && site.siteTitle.trim().length > 0)
        ? site.siteTitle.trim()
        : site.siteId;

      if (!seen.has(label)) {
        seen.add(label);
        labels.push(label);
      }
    }

    return labels;
  }

  private toDepartmentView(group: GroupItem): DepartmentView {
    const technicalName = this.toTechnicalName(group.id);
    const unitId = this.extractUnitId(technicalName);

    return {
      id: group.id,
      technicalName: technicalName,
      displayName: this.formatDepartmentName(technicalName, unitId),
      unitId: unitId,
      unitName: this.resolveUnitName(unitId)
    };
  }

  private isDepartmentVisibleByScope(group: DepartmentView): boolean {
    if (!this.isUnitAdmin) {
      return true;
    }

    if (!group.unitId) {
      return false;
    }

    return this.managedSiteIdSet.has(group.unitId.toUpperCase());
  }

  private toTechnicalName(groupId: string): string {
    if (!groupId) {
      return '';
    }
    return groupId.startsWith('GROUP_') ? groupId.substring(6) : groupId;
  }

  private extractUnitId(technicalName: string): string | null {
    const normalized = (technicalName || '').trim();
    if (!normalized) {
      return null;
    }

    const upperTechnicalName = normalized.toUpperCase();
    const knownUnitIds = Object.keys(this.unitNameById())
      .sort((a, b) => b.length - a.length);

    for (const knownUnitId of knownUnitIds) {
      const upperUnitId = knownUnitId.toUpperCase();
      if (
        upperTechnicalName === upperUnitId ||
        upperTechnicalName.startsWith(upperUnitId + '_') ||
        upperTechnicalName.includes('_' + upperUnitId + '_') ||
        upperTechnicalName.endsWith('_' + upperUnitId) ||
        upperTechnicalName.startsWith('SITE_' + upperUnitId + '_')
      ) {
        return knownUnitId;
      }
    }

    const siteMatch = upperTechnicalName.match(/^SITE_([^_]+)_/i);
    if (siteMatch?.[1]) {
      return siteMatch[1];
    }

    const legacyMatch = upperTechnicalName.match(/^(E\d{4,})_/i);
    if (legacyMatch?.[1]) {
      return legacyMatch[1].toUpperCase();
    }

    return null;
  }

  private formatDepartmentName(technicalName: string, unitId?: string | null): string {
    if (!technicalName) {
      return '';
    }

    const tokens = technicalName
      .split('_')
      .filter(token => token && token.trim().length > 0);

    if (tokens.length === 0) {
      return technicalName;
    }

    const upperUnitId = (unitId || '').toUpperCase();
    const rawTokens = [...tokens];

    if (rawTokens[0]?.toUpperCase() === 'SITE') {
      rawTokens.shift();
    }

    if (upperUnitId && rawTokens[0]?.toUpperCase() === upperUnitId) {
      rawTokens.shift();
    }

    const hasLegacyUnitPrefix = rawTokens[0] && /^E\d{4,}$/i.test(rawTokens[0]);
    const normalizedTokens = hasLegacyUnitPrefix ? rawTokens.slice(1) : rawTokens;

    const withoutCategory = normalizedTokens.filter(token => token.toLowerCase() !== 'negociados');
    const finalTokens = withoutCategory.length > 0 ? withoutCategory : normalizedTokens;

    if (finalTokens.length === 0) {
      return technicalName.replace(/_/g, ' ');
    }

    return finalTokens.join(' ');
  }

  private resolveUnitName(unitId: string | null): string {
    if (!unitId) {
      return 'Sin unidad';
    }

    const knownName = this.unitNameById()[unitId];
    if (knownName && knownName.trim().length > 0) {
      return knownName;
    }

    return unitId;
  }

  private getSiteDisplayName(siteId: string, title?: string, description?: string): string {
    if (description && description.trim().length > 0) {
      return description.trim();
    }
    if (title && title.trim().length > 0) {
      return title.trim();
    }
    return siteId;
  }

  private getAssignableDepartmentIds(): string[] {
    if (this.viewMode() === 'global') {
      return this.departments().map(group => group.id);
    }

    const selectedDepartment = this.selectedDepartment();
    if (selectedDepartment && selectedDepartment.unitId) {
      return this.departments()
        .filter(group => group.unitId === selectedDepartment.unitId)
        .map(group => group.id);
    }

    return this.departments().map(group => group.id);
  }

  private getVisibleDepartmentsFromMenu(): DepartmentView[] {
    return this.unitMenus().reduce((acc, menu) => acc.concat(menu.departments), [] as DepartmentView[]);
  }

  private getUserFullName(user: User): string {
    const firstName = (user.firstName || '').trim();
    const lastName = (user.lastName || '').trim();
    const fullName = (firstName + ' ' + lastName).trim();
    return fullName.length > 0 ? fullName : user.id;
  }

  private enrichMemberNames(members: GroupMemberItem[]): void {
    const pendingIds = Array.from(new Set(
      (members || [])
        .filter(member => member.memberType === 'PERSON')
        .filter(member => {
          const display = (member.displayName || '').trim();
          return !display || display.toUpperCase() === member.id.toUpperCase();
        })
        .map(member => member.id)
    ));

    if (pendingIds.length === 0) {
      return;
    }

    const maxLookups = 60;
    const requests = pendingIds.slice(0, maxLookups).map(userId =>
      this.userService.getUsers(10, 0, undefined, userId).pipe(
        map(response => {
          const users = response.users || [];
          const exact = users.find(user => user.id.toUpperCase() === userId.toUpperCase());
          const match = exact || users[0];
          if (!match) {
            return { userId, fullName: '' };
          }
          const fullName = this.getUserFullName(match);
          return { userId, fullName: fullName.toUpperCase() === userId.toUpperCase() ? '' : fullName };
        }),
        catchError(() => of({ userId, fullName: '' }))
      )
    );

    if (requests.length === 0) {
      return;
    }

    forkJoin(requests).subscribe(results => {
      const current = { ...this.memberNameById() };
      for (const result of results) {
        if (result.fullName && result.fullName.trim().length > 0) {
          current[result.userId] = result.fullName.trim();
        }
      }
      this.memberNameById.set(current);
    });
  }

  private enrichMemberManagedSites(members: GroupMemberItem[]): void {
    const pendingIds = Array.from(new Set(
      (members || [])
        .filter(member => member.memberType === 'PERSON')
        .map(member => member.id)
    ));

    if (pendingIds.length === 0) {
      return;
    }

    const maxLookups = 60;
    const requests = pendingIds.slice(0, maxLookups).map(userId =>
      this.userService.getUserSites(userId).pipe(
        map(response => {
          const managedSiteIds = this.normalizeUnitIds(
            (response.sites || [])
              .filter(site => this.isSiteManagerRole(site.role))
              .map(site => site.siteId)
          );
          return { userId, managedSiteIds };
        }),
        catchError(() => of({ userId, managedSiteIds: [] as string[] }))
      )
    );

    if (requests.length === 0) {
      return;
    }

    forkJoin(requests).subscribe(results => {
      const current = { ...this.memberManagedSiteIdsByUser() };
      for (const result of results) {
        current[result.userId] = result.managedSiteIds;
      }
      this.memberManagedSiteIdsByUser.set(current);
    });
  }

  private refreshMemberManagedSitesForUser(userId: string): void {
    this.userService.getUserSites(userId).pipe(
      map(response => this.normalizeUnitIds(
        (response.sites || [])
          .filter(site => this.isSiteManagerRole(site.role))
          .map(site => site.siteId)
      )),
      catchError(() => of([] as string[]))
    ).subscribe(managedSiteIds => {
      const current = { ...this.memberManagedSiteIdsByUser() };
      current[userId] = managedSiteIds;
      this.memberManagedSiteIdsByUser.set(current);
    });
  }

  private userMatchesTerm(user: User, term: string): boolean {
    const normalized = term.toLowerCase();
    const fullName = this.getUserFullName(user).toLowerCase();

    return user.id.toLowerCase().includes(normalized) ||
      (user.email || '').toLowerCase().includes(normalized) ||
      fullName.includes(normalized);
  }

  private isSiteManagerRole(role: string | null | undefined): boolean {
    return (role || '').trim().toLowerCase() === 'sitemanager';
  }

  private clearTransientQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true
    });
  }

  private loadActionUserDetails(userId: string): void {
    this.actionDetailsLoading.set(true);
    this.actionDetailsError.set('');

    forkJoin({
      users: this.userService.getUsers(20, 0, undefined, userId).pipe(
        catchError(() => of({ users: [], totalUsers: 0, hasMore: false } as UserListResponse))
      ),
      groups: this.userService.getUserGroups(userId).pipe(
        catchError(() => of({ groups: [], totalItems: 0 } as UserGroupListResponse))
      ),
      sites: this.userService.getUserSites(userId).pipe(
        catchError(() => of({ sites: [], totalItems: 0 } as UserSiteMembershipListResponse))
      )
    }).subscribe({
      next: ({ users, groups, sites }) => {
        const exactUser = (users.users || []).find(user => user.id.toUpperCase() === userId.toUpperCase()) || users.users?.[0];
        const fullName = exactUser ? this.getUserFullName(exactUser) : this.getMemberName({ id: userId, displayName: userId, memberType: 'PERSON' });
        const email = exactUser?.email || '—';

        const departmentsByUnit = new Map<string, string[]>();
        const technicalGroupsByUnit = new Map<string, string[]>();
        for (const group of groups.groups || []) {
          const parsed = this.parseActionGroupForDisplay(group.id);
          if (!parsed) {
            continue;
          }

          const targetMap = parsed.type === 'technical' ? technicalGroupsByUnit : departmentsByUnit;
          const current = targetMap.get(parsed.unitId) || [];
          if (!current.includes(parsed.label)) {
            current.push(parsed.label);
          }
          targetMap.set(parsed.unitId, current);
        }

        const cards: UserUnitMembershipCard[] = [];
        const seen = new Set<string>();
        for (const site of sites.sites || []) {
          if (seen.has(site.siteId)) {
            continue;
          }
          seen.add(site.siteId);
          cards.push({
            siteId: site.siteId,
            siteName: this.resolveUnitName(site.siteId),
            role: this.formatUserSiteRole(site.role),
            isAdmin: this.isSiteManagerRole(site.role),
            departments: (departmentsByUnit.get(site.siteId) || []).sort((a, b) => a.localeCompare(b)),
            technicalGroups: (technicalGroupsByUnit.get(site.siteId) || []).sort((a, b) => a.localeCompare(b))
          });
        }

        const unitsFromGroups = new Set<string>([
          ...departmentsByUnit.keys(),
          ...technicalGroupsByUnit.keys()
        ]);

        for (const unitId of unitsFromGroups.values()) {
          if (seen.has(unitId)) {
            continue;
          }

          cards.push({
            siteId: unitId,
            siteName: this.resolveUnitName(unitId),
            role: 'Miembro',
            isAdmin: false,
            departments: [...(departmentsByUnit.get(unitId) || [])].sort((a, b) => a.localeCompare(b)),
            technicalGroups: [...(technicalGroupsByUnit.get(unitId) || [])].sort((a, b) => a.localeCompare(b))
          });
        }

        cards.sort((a, b) => a.siteName.localeCompare(b.siteName));
        const hasAnyAdmin = cards.some(card => card.isAdmin);

        this.actionUserFullName.set(fullName || userId);
        this.actionUserEmail.set(email);
        this.actionUserRole.set(hasAnyAdmin ? 'Administrador de sitio en alguna unidad' : 'Usuario de sitio');
        this.actionUserUnits.set(cards);
        this.actionDetailsLoading.set(false);
      },
      error: (err) => {
        console.error('Error cargando detalle de usuario para ficha:', err);
        this.actionDetailsError.set('No se pudo cargar la ficha de usuario.');
        this.actionDetailsLoading.set(false);
      }
    });
  }

  private formatUserSiteRole(role: string | null | undefined): string {
    const normalized = (role || '').trim().toLowerCase();
    if (normalized === 'sitemanager') {
      return 'Administrador de sitio';
    }
    if (normalized === 'sitecollaborator') {
      return 'Colaborador';
    }
    if (normalized === 'sitecontributor') {
      return 'Contribuidor';
    }
    if (normalized === 'siteconsumer') {
      return 'Lector';
    }
    return role || 'Miembro';
  }

  private parseActionGroupForDisplay(groupId: string): { unitId: string; label: string; type: 'department' | 'technical' } | null {
    const technicalName = this.toTechnicalName(groupId);
    const unitId = this.extractUnitId(technicalName);
    if (!unitId) {
      return null;
    }

    const suffix = this.extractGroupSuffixForUnit(technicalName, unitId);
    if (!suffix) {
      return null;
    }

    const normalizedSuffix = suffix.replace(/_/g, '').toUpperCase();
    if (normalizedSuffix === unitId.replace(/_/g, '').toUpperCase()) {
      return null;
    }

    const normalizedRole = this.normalizeSiteRoleGroupLabel(suffix);
    if (normalizedRole) {
      return { unitId, label: normalizedRole, type: 'technical' };
    }

    const departmentLabel = this.formatDepartmentName(technicalName, unitId).trim();
    if (!departmentLabel) {
      return null;
    }

    const normalizedFromDepartment = this.normalizeSiteRoleGroupLabel(departmentLabel);
    if (normalizedFromDepartment) {
      return { unitId, label: normalizedFromDepartment, type: 'technical' };
    }

    return { unitId, label: departmentLabel, type: 'department' };
  }

  private extractGroupSuffixForUnit(technicalName: string, unitId: string): string {
    const upper = (technicalName || '').toUpperCase();
    const upperUnitId = (unitId || '').toUpperCase();

    const sitePrefix = 'SITE_' + upperUnitId + '_';
    if (upper.startsWith(sitePrefix)) {
      return technicalName.substring(sitePrefix.length);
    }

    const plainPrefix = upperUnitId + '_';
    if (upper.startsWith(plainPrefix)) {
      return technicalName.substring(plainPrefix.length);
    }

    if (upper === upperUnitId) {
      return '';
    }

    const tokens = technicalName.split('_').filter(token => token && token.trim().length > 0);
    if (tokens.length === 0) {
      return '';
    }

    let index = 0;
    if ((tokens[0] || '').toUpperCase() === 'SITE') {
      index = 1;
    }

    if ((tokens[index] || '').toUpperCase() === upperUnitId) {
      index++;
    }

    return tokens.slice(index).join('_');
  }

  private normalizeSiteRoleGroupLabel(value: string): string | null {
    const normalized = (value || '').replace(/_/g, '').trim().toUpperCase();
    if (normalized === 'SITECONSUMER') {
      return 'Lector de sitio';
    }
    if (normalized === 'SITECOLLABORATOR') {
      return 'Colaborador de sitio';
    }
    if (normalized === 'SITECONTRIBUTOR') {
      return 'Contribuidor de sitio';
    }
    if (normalized === 'SITEMANAGER') {
      return 'Administrador de sitio';
    }
    return null;
  }

  private resolvePreferredUnitFromNavigation(): UnitMenuView | null {
    const units = this.unitMenus();
    if (units.length === 0) {
      return null;
    }

    const rawUnitId = (this.selectedUnitId() || '').trim();
    const rawUnitName = (this.selectedUnitName() || '').trim();

    if (rawUnitId) {
      const byIdExact = units.find(unit => unit.unitId.toUpperCase() === rawUnitId.toUpperCase());
      if (byIdExact) {
        return byIdExact;
      }
    }

    const normalizedId = this.normalizeForComparison(rawUnitId);
    if (normalizedId) {
      const byIdNormalized = units.find(unit => this.normalizeForComparison(unit.unitId) === normalizedId);
      if (byIdNormalized) {
        return byIdNormalized;
      }
    }

    const normalizedName = this.normalizeForComparison(rawUnitName);
    if (normalizedName) {
      const byNameExact = units.find(unit => this.normalizeForComparison(unit.unitName) === normalizedName);
      if (byNameExact) {
        return byNameExact;
      }

      const byNameContains = units.find(unit =>
        this.normalizeForComparison(unit.unitName).includes(normalizedName) ||
        normalizedName.includes(this.normalizeForComparison(unit.unitName))
      );
      if (byNameContains) {
        return byNameContains;
      }
    }

    return null;
  }

  private normalizeForComparison(value: string): string {
    return (value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^A-Za-z0-9]/g, '')
      .toUpperCase();
  }
}
