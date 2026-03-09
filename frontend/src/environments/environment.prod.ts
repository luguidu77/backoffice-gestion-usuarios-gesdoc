/**
 * Configuración de entorno para PRODUCCIÓN.
 *
 * Angular usa este fichero cuando se ejecuta:
 *   npm run build:prod  (ng build --configuration production)
 *
 * En producción, el frontend compilado se despliega normalmente
 * en el mismo servidor que el backend, por eso las rutas /api y /ping
 * no necesitan proxy: el navegador las envía directamente al servidor.
 */
export const environment = {
  production: true,

  // En producción, el frontend y el backend viven en el mismo origen
  apiUrl: '',

  backendUrl: ''
};
