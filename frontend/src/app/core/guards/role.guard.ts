import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { AppRole } from '../models/auth.model';

export const roleGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const authService = inject(AuthService);

    // 1. Verify Authentication
    if (!authService.isAuthenticated()) {
        router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
    }

    // 2. Extrac user data and expected roles
    const user = authService.getUserData();
    const expectedRoles: AppRole[] = route.data?.['roles'] || [];

    if (!user || expectedRoles.length === 0) {
        // If no roles required, allow
        return true;
    }

    // 3. Verify user matches one of the expected app roles
    if (expectedRoles.includes(user.role as AppRole)) {
        return true;
    }

    // 4. Deny access if they lack roles
    console.warn('Usuario sin permisos para esta ruta. Redirigiendo a /dashboard');
    router.navigate(['/dashboard']);
    return false;
};
