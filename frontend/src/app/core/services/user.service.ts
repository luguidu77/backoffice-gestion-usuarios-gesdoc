import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserListResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  /**
   * Obtiene la lista de usuarios de Alfresco.
   * 
   * @param maxItems Número máximo de usuarios a obtener
   * @param skipCount Número de usuarios a omitir (para paginación)
   * @returns Observable con la lista de usuarios
   */
  getUsers(maxItems: number = 100, skipCount: number = 0): Observable<UserListResponse> {
    const params = new HttpParams()
      .set('maxItems', maxItems.toString())
      .set('skipCount', skipCount.toString());

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
}
