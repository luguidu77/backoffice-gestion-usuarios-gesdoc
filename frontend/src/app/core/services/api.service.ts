/**
 * Servicio principal de comunicación con el backend Spring Boot.
 *
 * Buenas prácticas seguidas:
 *   - Centraliza TODAS las llamadas HTTP en un único lugar
 *   - Los componentes no llaman a HttpClient directamente; usan este servicio
 *   - Facilita el testing (se puede mockear este servicio en lugar de HttpClient)
 *   - providedIn: 'root' → singleton disponible en toda la aplicación
 *
 * Uso en un componente:
 *   constructor(private api: ApiService) {}
 *
 *   this.api.ping().subscribe({
 *     next: (resp) => console.log(resp),  // "OK BACKOFFICE"
 *     error: (err)  => console.error(err)
 *   });
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'  // Singleton global: Angular lo instancia una sola vez
})
export class ApiService {

  constructor(private http: HttpClient) {}

  /**
   * Llama a GET /ping del backend Spring Boot.
   *
   * El proxy (proxy.conf.json) redirige /ping → http://localhost:8085/ping
   * durante el desarrollo con `npm start`.
   *
   * responseType: 'text' → el backend devuelve un String, no JSON.
   *
   * @returns Observable<string> que emite "OK BACKOFFICE" si el servidor responde
   */
  ping(): Observable<string> {
    return this.http.get('/ping', { responseType: 'text' });
  }

  // ============================================================
  // Aquí se añadirán futuros métodos para conectar con Alfresco:
  //   getDocumentos(): Observable<Documento[]> { ... }
  //   getUsuarios():   Observable<Usuario[]>   { ... }
  // ============================================================

}
