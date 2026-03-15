import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UnitReassignmentAuditItem, UserOriginAuditItem } from '../../../../core/models/user.model';
import { SiteService } from '../../../../core/services/site.service';

type AuditSectionId = 'reassignments' | 'user-origin' | 'user-activity';

interface AuditSectionCard {
  id: AuditSectionId;
  title: string;
  description: string;
  available: boolean;
}

@Component({
  selector: 'app-user-audit-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-audit-page.component.html',
  styleUrl: './user-audit-page.component.scss'
})
export class UserAuditPageComponent implements OnInit {
  readonly pageSize = 20;
  readonly sectionCards: AuditSectionCard[] = [
    {
      id: 'reassignments',
      title: 'Reasignaciones de usuarios',
      description: 'Consulta de justificantes PDF y detalle de movimientos entre unidades.',
      available: true
    },
    {
      id: 'user-origin',
      title: 'Fecha y creador de usuario',
      description: 'Consulta de alta de cuenta y origen de creación del usuario.',
      available: true
    },
    {
      id: 'user-activity',
      title: 'Actividad de usuario',
      description: 'Trazabilidad de acciones relevantes y eventos asociados a la cuenta.',
      available: false
    }
  ];

  isGlobalAdmin = signal<boolean>(false);
  activeSection = signal<AuditSectionId>('reassignments');
  loading = signal<boolean>(false);
  error = signal<string>('');

  filterUserId = signal<string>('');
  filterMode = signal<'' | 'TRANSFER' | 'ADD' | 'DEPARTMENTS'>('');
  filterFromDate = signal<string>('');
  filterToDate = signal<string>('');
  sortOrder = signal<'newest' | 'oldest' | 'user-asc' | 'user-desc'>('newest');
  audits = signal<UnitReassignmentAuditItem[]>([]);
  unitNameById = signal<Record<string, string>>({});
  totalItems = signal<number>(0);
  hasMore = signal<boolean>(false);
  page = signal<number>(1);
  reassignmentHasSearched = signal<boolean>(false);

  originLoading = signal<boolean>(false);
  originError = signal<string>('');
  originSearchTerm = signal<string>('');
  originStatus = signal<'all' | 'active' | 'inactive'>('all');
  originItems = signal<UserOriginAuditItem[]>([]);
  originTotalItems = signal<number>(0);
  originHasMore = signal<boolean>(false);
  originPage = signal<number>(1);
  originHasSearched = signal<boolean>(false);

  canGoPrevious = computed(() => this.page() > 1 && !this.loading());
  canGoNext = computed(() => this.hasMore() && !this.loading());
  canGoPreviousOrigin = computed(() => this.originPage() > 1 && !this.originLoading());
  canGoNextOrigin = computed(() => this.originHasMore() && !this.originLoading());

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private siteService: SiteService
  ) { }

  ngOnInit(): void {
    this.isGlobalAdmin.set(this.authService.getUserRole() === 'GLOBAL_ADMIN');
    if (this.isGlobalAdmin()) {
      this.loadUnitsMap();
    }
  }

  onFilterSubmit(): void {
    this.page.set(1);
    this.loadAudits();
  }

  selectSection(sectionId: AuditSectionId): void {
    this.activeSection.set(sectionId);
    this.error.set('');

    if (sectionId === 'user-origin') {
      this.originError.set('');
    }

  }

  resetFilter(): void {
    this.filterUserId.set('');
    this.filterMode.set('');
    this.filterFromDate.set('');
    this.filterToDate.set('');
    this.sortOrder.set('newest');
    this.page.set(1);
    this.reassignmentHasSearched.set(true);
    this.reassignmentHasSearched.set(false);
    this.audits.set([]);
    this.totalItems.set(0);
    this.hasMore.set(false);
    this.error.set('');
  }

  goPrevious(): void {
    if (!this.canGoPrevious()) {
      return;
    }
    this.page.set(this.page() - 1);
    this.loadAudits();
  }

  goNext(): void {
    if (!this.canGoNext()) {
      return;
    }
    this.page.set(this.page() + 1);
    this.loadAudits();
  }

  onUserOriginFilterSubmit(): void {
    this.originPage.set(1);
    this.originHasSearched.set(true);
    this.loadUserOriginAudits();
  }

  resetUserOriginFilter(): void {
    this.originSearchTerm.set('');
    this.originStatus.set('all');
    this.originPage.set(1);
    this.originHasSearched.set(false);
    this.originItems.set([]);
    this.originTotalItems.set(0);
    this.originHasMore.set(false);
    this.originError.set('');
  }

  canSearchReassignments(): boolean {
    return this.filterUserId().trim().length > 0
      || this.filterFromDate().trim().length > 0
      || this.filterToDate().trim().length > 0;
  }

  canSearchUserOrigin(): boolean {
    return this.originSearchTerm().trim().length > 0;
  }

  goPreviousOrigin(): void {
    if (!this.canGoPreviousOrigin()) {
      return;
    }
    this.originPage.set(this.originPage() - 1);
    this.loadUserOriginAudits();
  }

  goNextOrigin(): void {
    if (!this.canGoNextOrigin()) {
      return;
    }
    this.originPage.set(this.originPage() + 1);
    this.loadUserOriginAudits();
  }

  openAudit(item: UnitReassignmentAuditItem): void {
    this.userService.downloadUnitReassignmentAuditPdf(item.nodeId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => {
        console.error('Error abriendo PDF de auditoria:', err);
        this.error.set('No se pudo abrir el PDF de auditoría seleccionado.');
      }
    });
  }

  getModeLabel(mode: string | undefined): string {
    const normalized = (mode || '').trim().toUpperCase();
    if (normalized === 'TRANSFER') {
      return 'Traslado';
    }
    if (normalized === 'DEPARTMENTS') {
      return 'Reasignación de departamentos';
    }
    if (normalized === 'ADD') {
      return 'Alta en unidad';
    }
    return '—';
  }

  getFormattedDate(value: string | undefined): string {
    if (!value) {
      return '—';
    }
    const parsed = new Date(value);
    if (isNaN(parsed.getTime())) {
      return value;
    }
    return parsed.toLocaleString('es-ES');
  }

  getAuditUnits(item: UnitReassignmentAuditItem, key: 'fromUnitIds' | 'targetUnitIds' | 'finalUnitIds'): string[] {
    const metadata = item.metadata as Record<string, unknown> | undefined;
    const raw = metadata ? metadata[key] : undefined;
    if (!Array.isArray(raw)) {
      return [];
    }

    const values: string[] = [];
    for (const value of raw) {
      if (typeof value === 'string') {
        const clean = value.trim();
        if (clean && !values.includes(clean)) {
          values.push(clean);
        }
      }
    }

    return values;
  }

  getAuditSummary(item: UnitReassignmentAuditItem): string {
    const fromUnits = this.getAuditUnits(item, 'fromUnitIds');
    const targetUnits = this.getAuditUnits(item, 'targetUnitIds');
    const finalUnits = this.getAuditUnits(item, 'finalUnitIds');

    const metadata = item.metadata as Record<string, unknown> | undefined;
    const transferFromUnitId = typeof metadata?.['transferFromUnitId'] === 'string'
      ? String(metadata?.['transferFromUnitId']).trim()
      : '';
    const mode = (item.operationMode || '').toUpperCase();

    if (mode === 'TRANSFER') {
      const removedUnits = fromUnits.filter(unitId => !finalUnits.includes(unitId));
      const addedUnits = finalUnits.filter(unitId => !fromUnits.includes(unitId));

      const sourceId = transferFromUnitId
        || removedUnits[0]
        || fromUnits.find(unitId => !targetUnits.includes(unitId))
        || fromUnits[0]
        || '';

      const targetId = targetUnits.find(unitId => unitId !== sourceId)
        || addedUnits.find(unitId => unitId !== sourceId)
        || targetUnits[0]
        || addedUnits[0]
        || finalUnits.find(unitId => unitId !== sourceId)
        || '';

      return this.resolveUnitName(sourceId) + ' → ' + this.resolveUnitName(targetId);
    }

    const fromLabel = fromUnits.length > 0 ? fromUnits.map(id => this.resolveUnitName(id)).join(', ') : '—';
    const targetLabel = targetUnits.length > 0 ? targetUnits.map(id => this.resolveUnitName(id)).join(', ') : '—';
    const finalLabel = finalUnits.length > 0 ? finalUnits.map(id => this.resolveUnitName(id)).join(', ') : '—';

    return 'Origen: ' + fromLabel + ' · Destino: ' + targetLabel + ' · Resultado: ' + finalLabel;
  }

  resolveUnitName(unitId: string): string {
    const clean = (unitId || '').trim();
    if (!clean) {
      return '—';
    }

    const map = this.unitNameById();
    return map[clean]
      || map[clean.toLowerCase()]
      || map[clean.toUpperCase()]
      || clean;
  }

  getIsSectionActive(sectionId: AuditSectionId): boolean {
    return this.activeSection() === sectionId;
  }

  getUserOriginSourceLabel(source: string): string {
    const normalized = (source || '').trim().toUpperCase();
    if (normalized === 'ALFRESCO_PERSON') {
      return 'Directo (persona)';
    }
    if (normalized === 'AUDITORIA_REASIGNACIONES') {
      return 'Fallback auditoría';
    }
    return 'No disponible';
  }

  private loadUnitsMap(): void {
    this.siteService.getSites(1000, 0).subscribe({
      next: (response) => {
        const map: Record<string, string> = {};
        for (const site of response.sites || []) {
          const normalizedName = (site.description && site.description.trim().length > 0)
            ? site.description.trim()
            : ((site.title && site.title.trim().length > 0) ? site.title.trim() : site.id);
          const rawId = (site.id || '').trim();
          if (rawId) {
            map[rawId] = normalizedName;
            map[rawId.toLowerCase()] = normalizedName;
            map[rawId.toUpperCase()] = normalizedName;
          }
        }
        this.unitNameById.set(map);
      },
      error: () => {
        this.unitNameById.set({});
      }
    });
  }

  private loadAudits(): void {
    if (!this.canSearchReassignments()) {
      this.error.set('Aplica al menos un filtro (usuario o rango de fechas) para consultar reasignaciones.');
      this.audits.set([]);
      this.totalItems.set(0);
      this.hasMore.set(false);
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const skipCount = (this.page() - 1) * this.pageSize;
    this.userService.getUnitReassignmentAudits({
      userId: this.filterUserId().trim(),
      mode: this.filterMode(),
      fromDate: this.filterFromDate().trim(),
      toDate: this.filterToDate().trim(),
      sort: this.sortOrder(),
      includeMetadata: true
    }, this.pageSize, skipCount)
      .subscribe({
        next: (response) => {
          this.audits.set(response.items || []);
          this.totalItems.set(response.totalItems || 0);
          this.hasMore.set(!!response.hasMore);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error cargando auditorias:', err);
          this.error.set('No se pudieron cargar las auditorías de reasignaciones.');
          this.audits.set([]);
          this.totalItems.set(0);
          this.hasMore.set(false);
          this.loading.set(false);
        }
      });
  }

  private loadUserOriginAudits(): void {
    if (!this.canSearchUserOrigin()) {
      this.originError.set('Introduce TIP, nombre o email para consultar fecha y creador.');
      this.originItems.set([]);
      this.originTotalItems.set(0);
      this.originHasMore.set(false);
      this.originLoading.set(false);
      return;
    }

    this.originLoading.set(true);
    this.originError.set('');

    const skipCount = (this.originPage() - 1) * this.pageSize;
    this.userService.getUserOriginAudits({
      searchTerm: this.originSearchTerm().trim(),
      status: this.originStatus()
    }, this.pageSize, skipCount)
      .subscribe({
        next: (response) => {
          this.originItems.set(response.items || []);
          this.originTotalItems.set(response.totalItems || 0);
          this.originHasMore.set(!!response.hasMore);
          this.originLoading.set(false);
        },
        error: (err) => {
          console.error('Error cargando auditoría de fecha/creador:', err);
          this.originError.set('No se pudo cargar la auditoría de fecha y creador de usuario.');
          this.originItems.set([]);
          this.originTotalItems.set(0);
          this.originHasMore.set(false);
          this.originLoading.set(false);
        }
      });
  }
}
