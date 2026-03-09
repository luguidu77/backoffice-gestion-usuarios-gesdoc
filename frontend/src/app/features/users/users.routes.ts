import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const usersRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/user-list-page/user-list-page.component')
          .then(m => m.UserListPageComponent)
      }
    ]
  }
];
