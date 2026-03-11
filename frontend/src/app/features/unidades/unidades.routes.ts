import { Routes } from '@angular/router';
import { UnidadesPageComponent } from './pages/unidades-page/unidades-page.component';
import { UnidadDetailPageComponent } from './pages/unidad-detail-page/unidad-detail-page.component';

export const unidadesRoutes: Routes = [
  {
    path: '',
    component: UnidadesPageComponent,
    title: 'Unidades - Backoffice'
  },
  {
    path: ':id',
    component: UnidadDetailPageComponent,
    title: 'Detalle de Unidad - Backoffice'
  }
];
