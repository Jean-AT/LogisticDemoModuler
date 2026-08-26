import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      {
        path: 'requerimientos',
        loadComponent: () => import('./features/requerimientos/requerimientos-list.component').then((m) => m.RequerimientosListComponent),
        canActivate: [roleGuard(['SOLICITANTE', 'ADMIN'])],
      },
      {
        path: 'requerimientos/nuevo',
        loadComponent: () => import('./features/requerimientos/requerimiento-form.component').then((m) => m.RequerimientoFormComponent),
        canActivate: [roleGuard(['SOLICITANTE', 'ADMIN'])],
      },
      { path: 'requerimientos/:id', loadComponent: () => import('./features/requerimientos/requerimiento-detalle.component').then((m) => m.RequerimientoDetalleComponent) },
      {
        path: 'requerimientos/:id/editar',
        loadComponent: () => import('./features/requerimientos/requerimiento-form.component').then((m) => m.RequerimientoFormComponent),
        canActivate: [roleGuard(['SOLICITANTE', 'ADMIN'])],
      },
      {
        path: 'aprobaciones',
        loadComponent: () => import('./features/aprobaciones/aprobaciones-list.component').then((m) => m.AprobacionesListComponent),
        canActivate: [roleGuard(['APROBADOR', 'ADMIN'])],
      },
      {
        path: 'aprobaciones/:id',
        loadComponent: () => import('./features/aprobaciones/aprobacion-detalle.component').then((m) => m.AprobacionDetalleComponent),
        canActivate: [roleGuard(['APROBADOR', 'ADMIN', 'COMPRAS'])],
      },
      {
        path: 'compras',
        loadComponent: () => import('./features/compras/compras-page.component').then((m) => m.ComprasPageComponent),
        canActivate: [roleGuard(['COMPRAS', 'ADMIN'])],
      },
      {
        path: 'compras/ordenes/:id',
        loadComponent: () => import('./features/compras/orden-compra-detalle.component').then((m) => m.OrdenCompraDetalleComponent),
        canActivate: [roleGuard(['COMPRAS', 'ADMIN', 'APROBADOR'])],
      },
      { path: 'maestros', pathMatch: 'full', redirectTo: 'maestros/items' },
      {
        path: 'maestros/items',
        loadComponent: () => import('./features/maestros/maestros-items.component').then((m) => m.MaestrosItemsComponent),
        canActivate: [roleGuard(['ADMIN'])],
      },
      {
        path: 'maestros/almacenes',
        loadComponent: () => import('./features/maestros/maestros-almacenes.component').then((m) => m.MaestrosAlmacenesComponent),
        canActivate: [roleGuard(['ADMIN'])],
      },
      {
        path: 'maestros/proveedores',
        loadComponent: () => import('./features/maestros/maestros-proveedores.component').then((m) => m.MaestrosProveedoresComponent),
        canActivate: [roleGuard(['ADMIN'])],
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
