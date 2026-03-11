import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    DepartmentListResponse,
    NodePermissionsResponse,
    UpdateNodePermissionsRequest
} from '../models/department.model';

@Injectable({
    providedIn: 'root'
})
export class DepartmentService {
    private readonly API_URL = '/api/nodes';

    constructor(private http: HttpClient) { }

    /**
     * Obtiene la lista de departamentos de una unidad.
     */
    getDepartments(
        siteId: string,
        maxItems: number = 100,
        skipCount: number = 0
    ): Observable<DepartmentListResponse> {
        const params = new HttpParams()
            .set('maxItems', maxItems.toString())
            .set('skipCount', skipCount.toString());

        return this.http.get<DepartmentListResponse>(
            `${this.API_URL}/sites/${siteId}/departments`,
            { params }
        );
    }

    /**
     * Obtiene los permisos actuales de un nodo (local y heredados).
     */
    getNodePermissions(nodeId: string): Observable<NodePermissionsResponse> {
        return this.http.get<NodePermissionsResponse>(`${this.API_URL}/${nodeId}/permissions`);
    }

    /**
     * Actualiza los permisos de un nodo.
     * Puede cambiar la herencia y/o reemplazar la lista de permisos locales.
     */
    updateNodePermissions(
        nodeId: string,
        request: UpdateNodePermissionsRequest
    ): Observable<NodePermissionsResponse> {
        return this.http.put<NodePermissionsResponse>(
            `${this.API_URL}/${nodeId}/permissions`,
            request
        );
    }
}

