import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupListResponse, GroupMembersResponse } from '../models/group.model';

@Injectable({
    providedIn: 'root'
})
export class GroupService {
    private readonly API_URL = '/api/groups';

    constructor(private http: HttpClient) { }

    /**
     * Obtiene la lista de grupos de Alfresco.
     *
     * @param onlyRoot Si true, solo devuelve grupos raíz (los principales)
     * @param searchTerm Filtro de búsqueda
     * @param maxItems Paginación
     * @param skipCount Paginación
     */
    getGroups(
        onlyRoot: boolean = false,
        searchTerm?: string,
        maxItems: number = 200,
        skipCount: number = 0
    ): Observable<GroupListResponse> {
        let params = new HttpParams()
            .set('onlyRoot', onlyRoot.toString())
            .set('maxItems', maxItems.toString())
            .set('skipCount', skipCount.toString());

        if (searchTerm) {
            params = params.set('searchTerm', searchTerm);
        }

        return this.http.get<GroupListResponse>(this.API_URL, { params });
    }

    /**
     * Obtiene los miembros de un grupo.
     *
     * @param groupId ID del grupo (GROUP_xxx)
     * @param maxItems Paginación
     * @param skipCount Paginación
     */
    getGroupMembers(
        groupId: string,
        maxItems: number = 200,
        skipCount: number = 0
    ): Observable<GroupMembersResponse> {
        const params = new HttpParams()
            .set('maxItems', maxItems.toString())
            .set('skipCount', skipCount.toString());

        return this.http.get<GroupMembersResponse>(`${this.API_URL}/${groupId}/members`, { params });
    }

    /**
     * Anade un usuario a un grupo.
     */
    addUserToGroup(groupId: string, userId: string): Observable<void> {
        return this.http.post<void>(`${this.API_URL}/memberships`, { groupId, userId });
    }

    /**
     * Quita un usuario de un grupo.
     */
    removeUserFromGroup(groupId: string, userId: string): Observable<void> {
        const params = new HttpParams()
            .set('groupId', groupId)
            .set('userId', userId);
        return this.http.delete<void>(`${this.API_URL}/memberships`, { params });
    }
}
