import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  User,
  UserListResponse,
  GroupListResponse,
  UserSiteMembershipListResponse,
  UnitReassignmentProofPayload,
  UnitReassignmentProofResponse,
  UnitReassignmentAuditListResponse,
  UserOriginAuditListResponse
} from '../models/user.model';

export interface UnitReassignmentAuditQuery {
  userId?: string;
  mode?: 'TRANSFER' | 'ADD' | 'DEPARTMENTS' | '';
  fromDate?: string;
  toDate?: string;
  sort?: 'newest' | 'oldest' | 'user-asc' | 'user-desc';
  includeMetadata?: boolean;
}

export interface UserOriginAuditQuery {
  searchTerm?: string;
  status?: 'all' | 'active' | 'inactive';
}
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  /**
   * Obtiene la lista de usuarios de Alfresco.
   * 
   * @param maxItems Número máximo de usuarios a obtener
   * @param skipCount Número de usuarios a omitir (para paginación)
   * @param siteId ID del sitio por el que filtrar (opcional)
   * @param searchTerm Término de búsqueda (opcional)
   * @returns Observable con la lista de usuarios
   */
  getUsers(
    maxItems: number = 100,
    skipCount: number = 0,
    siteId?: string,
    searchTerm?: string
  ): Observable<UserListResponse> {
    let params = new HttpParams()
      .set('maxItems', maxItems.toString())
      .set('skipCount', skipCount.toString());

    if (siteId) {
      params = params.set('siteId', siteId);
    }

    if (searchTerm) {
      params = params.set('searchTerm', searchTerm);
    }

    return this.http.get<UserListResponse>(this.apiUrl, { params });
  }

  /**
   * Obtiene información de un usuario específico.
   * 
   * @param userId ID del usuario
   * @returns Observable con la información del usuario
   */
  getUserById(userId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${userId}`);
  }

  /**
   * Obtiene los grupos a los que pertenece un usuario.
   * 
   * @param userId ID del usuario
   * @returns Observable con la lista de grupos
   */
  getUserGroups(userId: string): Observable<GroupListResponse> {
    return this.http.get<GroupListResponse>(`${this.apiUrl}/${userId}/groups`);
  }

  /**
   * Obtiene las unidades (sitios) a las que pertenece un usuario, con su rol en cada una.
   *
   * @param userId ID del usuario
   * @returns Observable con las membresías de sitio del usuario
   */
  getUserSites(userId: string): Observable<UserSiteMembershipListResponse> {
    return this.http.get<UserSiteMembershipListResponse>(`${this.apiUrl}/${userId}/sites`);
  }

  updateUserEnabled(userId: string, enabled: boolean): Observable<User> {
    const encodedUserId = encodeURIComponent(userId);
    const endpoint = `${this.apiUrl}/${encodedUserId}/enabled`;
    return this.http.put<User>(endpoint, { enabled }).pipe(
      catchError((error) => {
        if (error?.status === 405) {
          return this.http.post<User>(endpoint, { enabled });
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Sube un PDF justificante asociado a una reasignacion de unidad.
   */
  uploadUnitReassignmentProof(
    userId: string,
    payload: UnitReassignmentProofPayload
  ): Observable<UnitReassignmentProofResponse> {
    const formData = new FormData();
    formData.append('file', payload.file);
    formData.append('operationMode', payload.operationMode);

    for (const unitId of payload.fromUnitIds) {
      formData.append('fromUnitIds', unitId);
    }
    for (const unitId of payload.targetUnitIds) {
      formData.append('targetUnitIds', unitId);
    }
    for (const unitId of payload.finalUnitIds) {
      formData.append('finalUnitIds', unitId);
    }
    if (payload.transferFromUnitId && payload.transferFromUnitId.trim().length > 0) {
      formData.append('transferFromUnitId', payload.transferFromUnitId.trim());
    }

    const encodedUserId = encodeURIComponent(userId);
    return this.http.post<UnitReassignmentProofResponse>(
      `${this.apiUrl}/${encodedUserId}/unit-reassignment-proof`,
      formData
    );
  }

  getUnitReassignmentAudits(
    query?: UnitReassignmentAuditQuery,
    maxItems: number = 20,
    skipCount: number = 0
  ): Observable<UnitReassignmentAuditListResponse> {
    let params = new HttpParams()
      .set('maxItems', maxItems.toString())
      .set('skipCount', skipCount.toString());

    const userId = query?.userId || '';
    if (userId.trim().length > 0) {
      params = params.set('userId', userId.trim());
    }
    if (query?.mode && query.mode.trim().length > 0) {
      params = params.set('mode', query.mode);
    }
    if (query?.fromDate && query.fromDate.trim().length > 0) {
      params = params.set('fromDate', query.fromDate.trim());
    }
    if (query?.toDate && query.toDate.trim().length > 0) {
      params = params.set('toDate', query.toDate.trim());
    }
    if (query?.sort && query.sort.trim().length > 0) {
      params = params.set('sort', query.sort);
    }
    if (query?.includeMetadata) {
      params = params.set('includeMetadata', 'true');
    }

    return this.http.get<UnitReassignmentAuditListResponse>(`${this.apiUrl}/unit-reassignment-audits`, { params });
  }

  downloadUnitReassignmentAuditPdf(nodeId: string): Observable<Blob> {
    const encodedNodeId = encodeURIComponent(nodeId);
    return this.http.get(`${this.apiUrl}/unit-reassignment-audits/${encodedNodeId}/content`, {
      responseType: 'blob'
    });
  }

  getUserOriginAudits(
    query?: UserOriginAuditQuery,
    maxItems: number = 20,
    skipCount: number = 0
  ): Observable<UserOriginAuditListResponse> {
    let params = new HttpParams()
      .set('maxItems', maxItems.toString())
      .set('skipCount', skipCount.toString());

    if (query?.searchTerm && query.searchTerm.trim().length > 0) {
      params = params.set('searchTerm', query.searchTerm.trim());
    }

    if (query?.status && query.status.trim().length > 0) {
      params = params.set('status', query.status);
    }

    return this.http.get<UserOriginAuditListResponse>(`${this.apiUrl}/user-origin-audits`, { params });
  }
}
