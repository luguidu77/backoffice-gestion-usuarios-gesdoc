import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    Department,
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
        const encodedSiteId = encodeURIComponent(siteId);
        const params = new HttpParams()
            .set('maxItems', maxItems.toString())
            .set('skipCount', skipCount.toString());

        return this.http.get<DepartmentListResponse>(
            `${this.API_URL}/sites/${encodedSiteId}/departments`,
            { params }
        );
    }

    createDepartment(siteId: string, name: string): Observable<Department> {
        const encodedSiteId = encodeURIComponent(siteId);
        return this.http.post<Department>(`${this.API_URL}/sites/${encodedSiteId}/departments`, { name });
    }

    renameDepartment(nodeId: string, name: string): Observable<void> {
        const encodedNodeId = encodeURIComponent(nodeId);
        return this.http.put<void>(`${this.API_URL}/${encodedNodeId}/name`, { name });
    }

    deleteDepartment(nodeId: string): Observable<void> {
        const encodedNodeId = encodeURIComponent(nodeId);
        return this.http.delete<void>(`${this.API_URL}/${encodedNodeId}`);
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

