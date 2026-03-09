# Autenticación con Alfresco - Basic Authentication

## Implementación

El sistema utiliza **Basic Authentication** directamente sin tickets de Alfresco.

### ¿Por qué Basic Auth en lugar de Tickets?

- **Compatibilidad**: Funciona aunque los tickets estén deshabilitados en Alfresco
- **Simplicidad**: No requiere gestión de tickets en Alfresco
- **Universal**: Basic Auth está siempre disponible en Alfresco

### Flujo de Autenticación

```
1. Usuario envía credenciales (username + password)
   ↓
2. Backend valida contra Alfresco:
   GET /api/-default-/public/alfresco/versions/1/people/{username}
   Header: Authorization: Basic {base64(username:password)}
   ↓
3. Si Alfresco responde 200 OK:
   - Credenciales válidas ✓
   - Se genera token: Base64(username:password)
   - Se obtiene información del usuario (firstName, lastName, email)
   ↓
4. Frontend almacena el token en localStorage
   ↓
5. En cada petición subsecuente:
   - AuthInterceptor añade: Authorization: Basic {token}
   - Backend/Alfresco valida el token
```

## Endpoints Backend

### POST /api/auth/login
```json
Request:
{
  "username": "admin",
  "password": "admin"
}

Response (éxito):
{
  "success": true,
  "ticket": "YWRtaW46YWRtaW4=",  // Base64(admin:admin)
  "username": "admin",
  "firstName": "Administrator",
  "lastName": "",
  "email": "admin@alfresco.com"
}

Response (error):
{
  "success": false,
  "message": "Credenciales inválidas"
}
```

### POST /api/auth/logout
```json
Request:
{
  "ticket": "YWRtaW46YWRtaW4="
}

Response:
{
  "success": true,
  "message": "Sesión cerrada correctamente"
}
```

### GET /api/auth/validate?ticket={token}
```json
Response:
{
  "valid": true,
  "message": "Token válido"
}
```

## Seguridad

### ✅ Configuración Actual

**Tu instancia de Alfresco usa HTTPS** ✓
- URL: `https://alfresco23pre.guardiacivil.es/alfresco`
- Las credenciales están protegidas durante la transmisión
- Basic Auth sobre HTTPS es seguro para producción

### Consideraciones Importantes

1. **HTTPS en tu Backend (Recomendado para producción)**
   - Tu backend debe usar HTTPS también
   - Configura SSL/TLS en Spring Boot con un certificado válido
   - Evita que las credenciales viajen en HTTP entre cliente y backend

2. **Token en localStorage**
   - El token (credenciales codificadas) se guarda en localStorage
   - Es vulnerable a ataques XSS
   - Implementar Content Security Policy (CSP)

3. **Validación continua**
   - Cada petición valida las credenciales contra Alfresco
   - Si cambia la contraseña en Alfresco, el token se invalida automáticamente

### Mejoras de Seguridad (Futuro)

1. **HttpOnly Cookies**: Mover el token a cookies HttpOnly
2. **Session tokens**: Generar tokens de sesión en lugar de usar credenciales directamente
3. **Token refresh**: Implementar refresh tokens con expiración
4. **CORS**: Configurar orígenes permitidos específicos

## Archivos Modificados

### Backend
- `AlfrescoAuthService.java` - Validación con Basic Auth
- `AuthController.java` - Endpoints REST de autenticación

### Frontend
- `auth.service.ts` - Gestión de tokens
- `auth.interceptor.ts` - Añade Authorization header
- `auth.guard.ts` - Protección de rutas
- `login-page.component.*` - Formulario de login
- `header.component.*` - Información de usuario y logout

## Configuración

### application.properties
```properties
# URL base de Alfresco (HTTPS)
alfresco.base-url=https://alfresco23pre.guardiacivil.es/alfresco

# Endpoints API
alfresco.api.authentication=/api/-default-/public/authentication/versions/1
alfresco.api.core=/api/-default-/public/alfresco/versions/1

# Timeouts (milisegundos)
alfresco.connection.timeout=5000
alfresco.read.timeout=10000
```

## Testing

### Probar manualmente con PowerShell

```powershell
# 1. Login
$body = @{
    username = "admin"
    password = "admin"
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "http://localhost:8085/api/auth/login" `
    -Method POST `
    -Body $body `
    -ContentType "application/json"

$token = $response.ticket
Write-Host "Token: $token"

# 2. Validar token
$validateResponse = Invoke-RestMethod `
    -Uri "http://localhost:8085/api/auth/validate?ticket=$token" `
    -Method GET

Write-Host "Token válido: $($validateResponse.valid)"

# 3. Usar el token en una petición
$headers = @{
    Authorization = "Basic $token"
}

$data = Invoke-RestMethod `
    -Uri "http://localhost:8085/api/ping" `
    -Method GET `
    -Headers $headers
```

### Probar desde el navegador

1. Abrir: `http://localhost:8085`
2. Redirige automáticamente a `/login`
3. Introducir credenciales de Alfresco
4. Se guarda el token en localStorage
5. Redirige a `/dashboard`
6. El header muestra el nombre de usuario y botón "Salir"

## FAQ

**¿Las credenciales viajan en cada petición?**
Sí, el token (credenciales en Base64) se envía en el header `Authorization` de cada petición.

**¿Es seguro Basic Auth?**
Solo sobre HTTPS. Sin HTTPS las credenciales son visibles en texto plano.

**¿Qué pasa si cambio la contraseña en Alfresco?**
El token se invalida automáticamente y el usuario debe hacer login de nuevo.

**¿Puedo volver a usar tickets?**
Sí, si se habilitan los tickets en Alfresco, hay que modificar `AlfrescoAuthService.java` para usar:
- POST `/tickets` para crear ticket
- DELETE `/tickets/{ticketId}` para eliminar ticket
