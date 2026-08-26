# Frontend - Logística Beneficencia de Lima

Frontend Angular (standalone components) para el módulo de logística: requerimientos, aprobaciones, órdenes de compra y maestros. Sigue la identidad visual institucional (morado) definida en `UXUI_PLAN.md` y `UX_UIPlanImplementation.md`.

## Stack

- Angular (standalone components, signals)
- Angular Router (con guards por rol)
- Reactive Forms
- Angular Material (tematizado)

## Requisitos

- Node (verificado con `v24.x`)
- Backend Spring Boot corriendo en `http://localhost:8080` (repositorio raíz del proyecto)

## Desarrollo

```bash
npm install
npm start        # ng serve --open  → http://localhost:4200
```

El `proxy.conf.json` redirige `/api` a `http://localhost:8080`, por lo que no requiere configuración CORS en desarrollo.

## Build

```bash
npm run build    # salida en `dist/frontend`
```

## Cuentas demo (backend)

| Usuario     | Rol          | Contraseña |
| ----------- | ------------ | ---------- |
| solicitante | SOLICITANTE  | demo123    |
| aprobador   | APROBADOR    | demo123    |
| compras     | COMPRAS      | demo123    |
| admin       | ADMIN        | demo123    |

## Rutas

- `/login` — autenticación JWT
- `/dashboard` — panel por rol
- `/requerimientos`, `/requerimientos/nuevo`, `/requerimientos/:id`, `/requerimientos/:id/editar`
- `/aprobaciones`, `/aprobaciones/:id`
- `/compras`, `/compras/ordenes/:id`
- `/maestros/items`, `/maestros/almacenes`, `/maestros/proveedores`

## Funcionalidades cubiertas

- Login con JWT + interceptor (Bearer) + guards por rol
- Dashboard con KPIs por rol
- Requerimientos: listado con filtros y paginación, creación/edición (borrador), detalle, envío y corrección de observados
- Aprobaciones: bandeja, detalle, aprobar/observar/rechazar, exportación PDF con encabezados configurables
- Compras: cola de pendientes, generación de OC, listado y detalle de órdenes
- Maestros: items, almacenes y proveedores (consulta + alta)
