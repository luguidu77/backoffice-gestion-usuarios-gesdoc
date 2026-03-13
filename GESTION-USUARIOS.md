# Gestión de Usuarios - Documentación

Sistema de gestión de usuarios integrado con Alfresco REST API v1.

---

## 🎯 Funcionalidades Implementadas

✅ **Login de usuario contra Alfresco**  
✅ **Logout y validación de sesión**  
✅ **Dashboard con comprobación de backend (`/ping`)**  
✅ **Listado de usuarios desde Alfresco**  
✅ **Paginación** (maxItems, skipCount)  
✅ **Visualización de estado** (activo/inactivo)  
✅ **Interfaz responsive**  
✅ **Protección de rutas** con authGuard  

---

## 📁 Estructura Backend

### Servicios

**AlfrescoUserService.java**
- Ubicación: `src/main/java/es/dggc/backoffice/service/`
- Métodos principales:
  - `listUsers(token, maxItems, skipCount)` - Lista usuarios con paginación
  - `listUsers(token)` - Lista usuarios (máximo 100, sin paginación)

### Controladores

**AuthController.java**
- Ubicación: `src/main/java/es/dggc/backoffice/controller/`
- Endpoints:
  - `POST /api/auth/login` - Login contra Alfresco (Basic Auth)
  - `POST /api/auth/logout` - Cierre de sesión
  - `GET /api/auth/validate` - Validación de token

**UserController.java**
- Ubicación: `src/main/java/es/dggc/backoffice/controller/`
- Endpoints:
  - `GET /api/users` - Lista todos los usuarios
    - Query params: `maxItems`, `skipCount`
    - Header: `Authorization: Basic {token}`
    - Response: `UserListResponse`
  - `GET /api/users/{userId}` - Endpoint expuesto (pendiente de implementación)

**PingController.java**
- Ubicación: `src/main/java/es/dggc/backoffice/controller/`
- Endpoint:
  - `GET /ping` - Comprobación de estado del backend

### DTOs

**AlfrescoPeopleListResponse.java**
- Mapea la respuesta completa de Alfresco API
- Incluye paginación y lista de usuarios

**UserListResponse.java**
- DTO simplificado para el frontend
- Incluye:
  - `users[]` - Lista de usuarios
  - `totalUsers` - Total de usuarios
  - `hasMore` - Si hay más usuarios disponibles

---

## 📁 Estructura Frontend

### Componentes

**LoginPageComponent**
- Ubicación: `frontend/src/app/features/auth/pages/login-page/`
- Funcionalidad:
  - Formulario reactivo con validaciones
  - Redirección a ruta solicitada (`returnUrl`) tras login

**DashboardPageComponent**
- Ubicación: `frontend/src/app/features/dashboard/pages/dashboard-page/`
- Funcionalidad:
  - Llamada a `/ping`
  - Estado visual de disponibilidad del backend

**UserListPageComponent**
- Ubicación: `frontend/src/app/features/users/pages/user-list-page/`
- Archivos:
  - `user-list-page.component.ts` - Lógica del componente
  - `user-list-page.component.html` - Vista HTML
  - `user-list-page.component.scss` - Estilos

### Servicios

**AuthService**
- Ubicación: `frontend/src/app/core/services/auth.service.ts`
- Métodos:
  - `login(credentials)`
  - `logout()`
  - `validateTicket(ticket)`
  - Gestión de sesión en `localStorage`

**UserService**
- Ubicación: `frontend/src/app/core/services/user.service.ts`
- Métodos:
  - `getUsers(maxItems, skipCount)` - Obtiene lista de usuarios
  - `getUserById(userId)` - Obtiene usuario específico (pendiente backend)

### Modelos

**user.model.ts**
- Interfaces TypeScript:
  - `User` - Usuario individual
  - `UserListResponse` - Respuesta de la lista

---

## 🎨 Características de la UI

### Vista de Lista
- **Tabla responsive** con columnas:
  - ID del usuario
  - Nombre completo
  - Correo electrónico
  - Estado (Activo/Inactivo)
  - Acciones (Ver detalles)

### Estados Visuales
- ✅ **Estado activo**: Badge verde
- ❌ **Estado inactivo**: Badge rojo con opacidad reducida en la fila

### Funcionalidades
- 🔄 Botón de actualizar
- 📊 Contador de usuarios totales
- ⏳ Indicador de carga (spinner)
- ⚠️ Mensajes de error con botón de reintentar
- 📭 Estado vacío cuando no hay usuarios

---

## 🔧 API de Alfresco Utilizada

### Base URL (Preproducción GC)

```
https://alfresco23pre.guardiacivil.es/alfresco
```

### Endpoint de Listado

```
GET https://alfresco23pre.guardiacivil.es/alfresco/api/-default-/public/alfresco/versions/1/people
```

**Headers:**
```
Authorization: Basic {base64(username:password)}
Content-Type: application/json
```

**Query Parameters:**
- `maxItems` (opcional) - Número máximo de usuarios (default: 100)
- `skipCount` (opcional) - Número de usuarios a omitir (default: 0)

**Respuesta:**
```json
{
  "list": {
    "pagination": {
      "count": 2,
      "hasMoreItems": false,
      "totalItems": 2,
      "skipCount": 0,
      "maxItems": 100
    },
    "entries": [
      {
        "entry": {
          "id": "admin",
          "firstName": "Administrator",
          "lastName": "",
          "email": "admin@alfresco.com",
          "enabled": true
        }
      }
    ]
  }
}
```

---

## 🚀 Uso

### Acceder a la Gestión de Usuarios

1. Arrancar la aplicación:
   ```bash
   java -jar admin-usuarios.jar
   ```

2. Acceder a: `http://localhost:8085` (entorno local/VM)

3. Hacer login con credenciales de Alfresco

4. En el header, hacer clic en **"Usuarios"**

5. La lista de usuarios se carga automáticamente

---

## 🔐 Seguridad

- ✅ Ruta protegida con `authGuard`
- ✅ Todas las peticiones incluyen Authorization header
- ✅ Token validado en cada petición al backend
- ✅ Comunicación HTTPS con Alfresco
- ✅ Certificado PEM de entorno GC empaquetado en el JAR

---

## 📝 Próximos Pasos Sugeridos

### Funcionalidades Pendientes

- [ ] **Ver detalles de usuario** - Implementar vista individual
- [ ] **Crear usuario** - Formulario para crear usuarios en Alfresco
- [ ] **Editar usuario** - Modificar datos de usuario existente
- [ ] **Desactivar/Activar usuario** - Toggle de estado
- [ ] **Eliminar usuario** - Con confirmación
- [ ] **Buscar usuarios** - Filtro por nombre, email, etc.
- [ ] **Paginación avanzada** - Navegación por páginas
- [ ] **Ordenamiento** - Por nombre, email, estado, etc.
- [ ] **Gestión de grupos** - Asignar usuarios a grupos de Alfresco
- [ ] **Cambiar contraseña** - Endpoint para resetear password

### Mejoras Técnicas

- [ ] **Manejo de errores mejorado** - Toast notifications
- [ ] **Cache de datos** - Evitar peticiones repetidas
- [ ] **Lazy loading mejorado** - Virtual scrolling para muchos usuarios
- [ ] **Tests unitarios** - Para servicios y componentes
- [ ] **Tests e2e** - Con Cypress o Playwright

---

## 📚 Referencias

- [Alfresco REST API Explorer](https://api-explorer.alfresco.com/api-explorer/)
- [Alfresco REST API Documentation](https://docs.alfresco.com/content-services/latest/develop/rest-api-guide/)
- [Spring Boot REST](https://spring.io/guides/gs/rest-service/)
- [Angular Signals](https://angular.io/guide/signals)
