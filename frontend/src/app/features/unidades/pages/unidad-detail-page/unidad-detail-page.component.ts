import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { SiteService } from '../../../../core/services/site.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../../../core/services/user.service';
import { User, UserSiteMembership } from '../../../../core/models/user.model';
import { finalize, forkJoin } from 'rxjs';
import { DepartmentListComponent } from '../../components/department-list/department-list.component';

interface UserUnitMembershipCard {
  siteId: string;
  siteName: string;
  role: string;
  departments: string[];
}

@Component({
  selector: 'app-unidad-detail-page',
  standalone: true,
  imports: [CommonModule, DepartmentListComponent],
  templateUrl: './unidad-detail-page.component.html',
  styleUrl: './unidad-detail-page.component.scss'
})
export class UnidadDetailPageComponent implements OnInit {
  private readonly membersPageSize = 100;
  private readonly membersUiPageSize = 10;

  routeSiteId = signal<string>('');
  siteId = signal<string>('');
  siteName = signal<string>('');
  activeTab = signal<'resumen' | 'departamentos' | 'miembros' | 'ajustes'>('departamentos');

  isGlobalAdmin = signal<boolean>(false);

  members = signal<User[]>([]);
  loadingMembers = signal<boolean>(false);
  membersError = signal<string>('');
  membersCurrentPage = signal<number>(1);

  membersTotalPages = computed<number>(() => {
    const total = this.members().length;
    return Math.max(1, Math.ceil(total / this.membersUiPageSize));
  });

  paginatedMembers = computed<User[]>(() => {
    const start = (this.membersCurrentPage() - 1) * this.membersUiPageSize;
    return this.members().slice(start, start + this.membersUiPageSize);
  });

  isUserDrawerOpen = signal<boolean>(false);
  selectedMember = signal<User | null>(null);
  userDetailsLoading = signal<boolean>(false);
  userDetailsError = signal<string>('');
  userRoleInCurrentSite = signal<string>('');
  userUnits = signal<UserUnitMembershipCard[]>([]);

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private userService: UserService,
    private siteService: SiteService
  ) { }

  ngOnInit(): void {
    const id = (this.route.snapshot.paramMap.get('id') || '').trim();
    if (id) {
      this.routeSiteId.set(id);
      this.siteId.set(id);
    }

    // Intentar obtener el nombre del sitio: primero desde query param, luego desde la lista de sitios
    const nameFromParam = (this.route.snapshot.queryParamMap.get('siteName') || '').trim();
    const tabFromParam = this.route.snapshot.queryParamMap.get('tab');

    if (tabFromParam === 'resumen' || tabFromParam === 'departamentos' || tabFromParam === 'miembros' || tabFromParam === 'ajustes') {
      this.activeTab.set(tabFromParam);
    }

    if (nameFromParam) {
      this.siteName.set(nameFromParam);
    }

    this.resolveSiteContext(id, nameFromParam);

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
    this.membersCurrentPage.set(1);

    this.loadMembersPage((this.siteId() || '').trim(), 0, []);
  }

  private loadMembersPage(siteId: string, skipCount: number, accumulator: User[]): void {
    this.userService.getUsers(this.membersPageSize, skipCount, siteId).subscribe({
      next: (response) => {
        const pageUsers = response?.users || [];
        const merged = [...accumulator, ...pageUsers];
        const hasMore = !!response?.hasMore && pageUsers.length > 0;

        if (hasMore) {
          this.loadMembersPage(siteId, skipCount + this.membersPageSize, merged);
          return;
        }

        const uniqueById = new Map<string, User>();
        for (const member of merged) {
          if (!member?.id) {
            continue;
          }
          if (!uniqueById.has(member.id)) {
            uniqueById.set(member.id, member);
          }
        }
        const deduplicated = Array.from(uniqueById.values())
          .sort((a, b) => (a.id || '').localeCompare(b.id || ''));

        this.members.set(deduplicated);
        this.membersCurrentPage.set(1);
        this.loadingMembers.set(false);
      },
      error: (err) => {
        console.error('Error cargando miembros:', err);
        this.membersError.set('Error al cargar los miembros de la unidad');
        this.loadingMembers.set(false);
      }
    });
  }

  previousMembersPage(): void {
    if (this.membersCurrentPage() > 1) {
      this.membersCurrentPage.set(this.membersCurrentPage() - 1);
    }
  }

  nextMembersPage(): void {
    if (this.membersCurrentPage() < this.membersTotalPages()) {
      this.membersCurrentPage.set(this.membersCurrentPage() + 1);
    }
  }

  openUserDetailsDrawer(member: User): void {
    if (!member?.id) {
      return;
    }

    this.selectedMember.set(member);
    this.userDetailsError.set('');
    this.userRoleInCurrentSite.set('');
    this.userUnits.set([]);
    this.isUserDrawerOpen.set(true);
    this.loadUserDetailsForDrawer(member.id);
  }

  closeUserDetailsDrawer(): void {
    this.isUserDrawerOpen.set(false);
    this.selectedMember.set(null);
    this.userDetailsLoading.set(false);
    this.userDetailsError.set('');
    this.userRoleInCurrentSite.set('');
    this.userUnits.set([]);
  }

  private loadUserDetailsForDrawer(userId: string): void {
    this.userDetailsLoading.set(true);
    this.userDetailsError.set('');

    forkJoin({
      sites: this.userService.getUserSites(userId),
      groups: this.userService.getUserGroups(userId)
    })
      .pipe(finalize(() => this.userDetailsLoading.set(false)))
      .subscribe({
        next: ({ sites, groups }) => {
          const siteMemberships = sites?.sites || [];
          const groupEntries = groups?.groups || [];

          const canonicalSiteIds = [
            ...siteMemberships.map(site => (site.siteId || '').trim()).filter(value => value.length > 0),
            (this.siteId() || '').trim()
          ];

          const role = siteMemberships.find(site => (site.siteId || '').toUpperCase() === this.siteId().toUpperCase())?.role || 'Sin rol asignado';
          this.userRoleInCurrentSite.set(this.formatRole(role));

          const departmentsBySite = this.groupDepartmentsBySiteId(
            groupEntries.map(group => group.id),
            canonicalSiteIds
          );
          const cards: UserUnitMembershipCard[] = siteMemberships.map(site => ({
            siteId: site.siteId,
            siteName: (site.siteTitle || '').trim() || site.siteId,
            role: this.formatRole(site.role),
            departments: (departmentsBySite.get(site.siteId) || []).sort((a, b) => a.localeCompare(b))
          }));

          this.userUnits.set(cards.sort((a, b) => a.siteName.localeCompare(b.siteName)));
        },
        error: (err) => {
          console.error('Error cargando detalle del usuario:', err);
          this.userDetailsError.set('No se pudo cargar el detalle del usuario.');
        }
      });
  }

  private formatRole(role: string): string {
    const normalized = (role || '').trim();
    if (!normalized) {
      return 'Sin rol asignado';
    }

    if (normalized === 'SiteManager') {
      return 'Administrador de Unidad';
    }

    if (normalized === 'SiteContributor') {
      return 'Colaborador';
    }

    if (normalized === 'SiteConsumer') {
      return 'Lector';
    }

    return normalized;
  }

  private groupDepartmentsBySiteId(groupIds: string[], canonicalSiteIds: string[]): Map<string, string[]> {
    const grouped = new Map<string, string[]>();
    const canonicalByUpper = new Map<string, string>();

    for (const siteId of canonicalSiteIds || []) {
      const clean = (siteId || '').trim();
      if (!clean) {
        continue;
      }
      canonicalByUpper.set(clean.toUpperCase(), clean);
    }

    for (const groupId of groupIds || []) {
      const parsed = this.parseGroupForDepartment(groupId);

      if (!parsed) {
        continue;
      }

      const siteId = canonicalByUpper.get(parsed.unitId.toUpperCase()) || parsed.unitId;
      const departmentName = parsed.label;

      if (!grouped.has(siteId)) {
        grouped.set(siteId, []);
      }

      const current = grouped.get(siteId)!;
      if (!current.includes(departmentName)) {
        current.push(departmentName);
      }
    }

    return grouped;
  }

  private toTechnicalName(groupId: string): string {
    const value = (groupId || '').trim();
    if (value.startsWith('GROUP_')) {
      return value.substring('GROUP_'.length);
    }
    return value;
  }

  private parseGroupForDepartment(groupId: string): { unitId: string; label: string } | null {
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

    if (this.normalizeSiteRoleGroupLabel(suffix)) {
      return null;
    }

    const departmentLabel = this.formatDepartmentName(technicalName, unitId).trim();
    if (!departmentLabel) {
      return null;
    }

    if (this.normalizeSiteRoleGroupLabel(departmentLabel)) {
      return null;
    }

    return { unitId, label: departmentLabel };
  }

  private extractUnitId(technicalName: string): string {
    const normalized = (technicalName || '').trim();
    if (!normalized) {
      return '';
    }

    const upperTechnicalName = normalized.toUpperCase();

    const siteMatch = upperTechnicalName.match(/^SITE_([^_]+)_/i);
    if (siteMatch?.[1]) {
      return siteMatch[1];
    }

    const legacyMatch = upperTechnicalName.match(/^(E\d{4,})_/i);
    if (legacyMatch?.[1]) {
      return legacyMatch[1].toUpperCase();
    }

    return '';
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

  private formatDepartmentName(technicalName: string, unitId?: string): string {
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

  private resolveSiteContext(routeSiteId: string, routeSiteName: string): void {
    if (!routeSiteId) {
      return;
    }

    this.siteService.getSites(1000, 0).subscribe({
      next: (resp) => {
        const normalizedRouteId = this.normalizeKey(routeSiteId);
        const normalizedRouteName = this.normalizeKey(routeSiteName);

        const match = (resp.sites || []).find(site => {
          const idMatch = this.normalizeKey(site.id) === normalizedRouteId;
          const titleMatch = normalizedRouteName && this.normalizeKey(site.title || '') === normalizedRouteName;
          const descMatch = normalizedRouteName && this.normalizeKey(site.description || '') === normalizedRouteName;
          const idByNameMatch = normalizedRouteName && this.normalizeKey(site.id) === normalizedRouteName;
          const routeAsDesc = this.normalizeKey(site.description || '') === normalizedRouteId;
          const routeAsTitle = this.normalizeKey(site.title || '') === normalizedRouteId;
          return idMatch || titleMatch || descMatch || idByNameMatch || routeAsDesc || routeAsTitle;
        });

        if (match) {
          this.siteId.set(match.id);
          this.siteName.set((match.description || '').trim() || (match.title || '').trim() || match.id);
          return;
        }

        if (!this.siteName()) {
          this.siteName.set(routeSiteName || routeSiteId);
        }
      },
      error: () => {
        if (!this.siteName()) {
          this.siteName.set(routeSiteName || routeSiteId);
        }
      }
    });
  }

  private normalizeKey(value: string): string {
    return (value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^A-Za-z0-9]/g, '')
      .toUpperCase();
  }
}
