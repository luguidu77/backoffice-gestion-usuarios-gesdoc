import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Site, SiteListResponse } from '../models/site.model';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class SiteService {
    private apiUrl = `${environment.apiUrl}/sites`;

    constructor(private http: HttpClient) { }

    /**
     * Obtiene la lista de sitios de Alfresco.
     *
     * @param maxItems Número máximo de sitios a obtener
     * @param skipCount Número de sitios a omitir (para paginación)
     * @returns Observable con la lista de sitios
     */
    getSites(maxItems: number = 100, skipCount: number = 0): Observable<SiteListResponse> {
        const params = new HttpParams()
            .set('maxItems', maxItems.toString())
            .set('skipCount', skipCount.toString());

        return this.http.get<SiteListResponse>(this.apiUrl, { params });
    }

    /**
     * Obtiene información de un sitio específico.
     *
     * @param siteId ID del sitio
     * @returns Observable con la información del sitio
     */
    getSiteById(siteId: string): Observable<Site> {
        return this.http.get<Site>(`${this.apiUrl}/${siteId}`);
    }
}
