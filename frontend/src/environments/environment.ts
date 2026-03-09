/**
 * Configuración de entorno para DESARROLLO local.
 *
 * Angular utiliza este fichero cuando se ejecuta:
 *   npm start  (ng serve)
 *
 * El proxy definido en proxy.conf.json redirige las llamadas
 * relativas ( /ping, /api/... ) al backend en localhost:8085.
 * Por eso apiUrl es una ruta relativa vacía, no una URL absoluta.
 */
export const environment = {
  production: false,

  // URL base para las llamadas a la API del backend.
  // En desarrollo usa rutas relativas + proxy (ver proxy.conf.json)
  apiUrl: '/api',

  // URL del backend. Útil si algún servicio necesita construir URLs absolutas.
  backendUrl: 'http://localhost:8085'
};
