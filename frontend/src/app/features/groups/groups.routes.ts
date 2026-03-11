import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const groupsRoutes: Routes = [
    {
        path: '',
        canActivate: [authGuard],
        children: [
            {
                path: '',
                loadComponent: () => import('./pages/groups-page/groups-page.component')
                    .then(m => m.GroupsPageComponent)
            }
        ]
    }
];
