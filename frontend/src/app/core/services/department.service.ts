import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DepartmentListResponse } from '../models/department.model';

@Injectable({
    providedIn: 'root'
})
export class DepartmentService {
    private readonly API_URL = '/api/nodes';

    constructor(private http: HttpClient) { }

    /**
     * Obtiene la lista de departamentos de una unidad.
     * 
     * @param siteId ID de la unidad
     * @param maxItems Número máximo a obtener
     * @param skipCount Número a omitir (para paginación)
     * @returns Observable con la lista
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
}
