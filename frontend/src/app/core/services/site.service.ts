import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Site, SiteListResponse } from '../models/site.model';
import { AuthService } from './auth.service';

@Injectable({
    providedIn: 'root'
})
export class SiteService {
    private readonly API_URL = '/api/sites';

    constructor(
        private http: HttpClient,
        private authService: AuthService
    ) { }

    /**
     * Obtiene la lista de sitios de Alfresco.
     *
     * @param maxItems Número máximo de sitios a obtener
     * @param skipCount Número de sitios a omitir (para paginación)
     * @returns Observable con la lista de sitios
     */
    getSites(maxItems: number = 100, skipCount: number = 0): Observable<SiteListResponse> {
        let params = new HttpParams()
            .set('maxItems', maxItems.toString())
            .set('skipCount', skipCount.toString());

        // Añadir rol y userId si están disponibles
        const user = this.authService.getUserData();
        if (user) {
            if (user.role) {
                params = params.set('role', user.role);
            }
            if (user.username) {
                params = params.set('userId', user.username);
            }
        }

        return this.http.get<SiteListResponse>(this.API_URL, { params });
    }

    /**
     * Obtiene información de un sitio específico.
     *
     * @param siteId ID del sitio
     * @returns Observable con la información del sitio
     */
    getSiteById(siteId: string): Observable<Site> {
        return this.http.get<Site>(`${this.API_URL}/${siteId}`);
    }
}
