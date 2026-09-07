# ERP Web - Documentacion del MVP1

Esta carpeta es la fuente de verdad para la evolucion del repositorio de Logistica hacia el MVP1 del ERP web.

## Objetivo actual

Implementar en este mismo repositorio un monolito modular con los dominios Plataforma, Cuadro de Necesidades, Presupuesto y Logistica.

El flujo demostrable del MVP1 sera:

`Configuracion -> Cuadro anual -> Revision -> Consolidacion -> Presupuesto PIA/PIM -> Requerimiento -> Precompromiso -> Cotizacion -> Orden de compra -> Compromiso -> Recepcion -> Stock`

## Documentos

- [Decisiones y alcance](MVP1_DECISIONES_Y_ALCANCE.md)
- [Arquitectura monolito modular y DDD](MVP1_ARQUITECTURA_DDD.md)
- [Modelo de datos e integraciones](MVP1_MODELO_DATOS_E_INTEGRACIONES.md)
- [Requerimientos](MVP1_REQUERIMIENTOS.md)
- [Backlog ordenado](MVP1_BACKLOG.md)
- [Criterios de aceptacion y validacion](MVP1_VALIDACION.md)

## Reglas de mantenimiento

1. Todo cambio funcional debe referenciar un requisito `*-RF*`.
2. Toda implementacion debe corresponder a una tarea del backlog.
3. Una tarea solo pasa a `COMPLETADA` cuando cumple pruebas y criterios de aceptacion.
4. Las decisiones nuevas o modificadas deben registrarse primero en `MVP1_DECISIONES_Y_ALCANCE.md`.
5. Las migraciones de base de datos son incrementales; no se modifican scripts Flyway ya aplicados.
6. No se permite acceso directo a repositorios, entidades JPA o tablas de otro modulo.

## Estados del backlog

- `PENDIENTE`: aun no iniciada.
- `EN_PROGRESO`: existe trabajo parcial no validado.
- `BLOQUEADA`: depende de una decision o recurso externo documentado.
- `COMPLETADA`: implementada, probada y aceptada.

## Estado inicial comprobado

- Backend Spring Boot, Angular y PostgreSQL operativos como demo de Logistica.
- Existen autenticacion JWT, maestros, requerimientos, aprobaciones, ordenes de compra y PDFs.
- Las 19 pruebas backend actuales pasan con `mvn test`.
- La ejecucion de pruebas Angular requiere corregir la configuracion/resolucion del entorno de test.

