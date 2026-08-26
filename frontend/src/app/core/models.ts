export type UserRole = 'SOLICITANTE' | 'APROBADOR' | 'COMPRAS' | 'ADMIN';

export type EstadoRequerimiento =
  | 'BORRADOR'
  | 'ENVIADO'
  | 'APROBADO'
  | 'OBSERVADO'
  | 'RECHAZADO'
  | 'CONVERTIDO_OC';

export type AccionAprobacion = 'APROBAR' | 'OBSERVAR' | 'RECHAZAR';
export type Moneda = 'PEN' | 'USD';

export interface Usuario {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  user: Usuario;
}

export interface Proveedor {
  id: number;
  code: string;
  name: string;
}

export interface Item {
  id: number;
  code: string;
  name: string;
  unitMeasure: string;
}

export interface Almacen {
  id: number;
  code: string;
  name: string;
}

export interface RequerimientoDetalle {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  almacenId: number;
  almacenCode: string;
  almacenName: string;
  cantidad: number;
  precioUnitarioEstimado: number;
  subtotalLinea: number;
}

export interface RequerimientoDetalleRequest {
  itemId: number;
  almacenId: number;
  cantidad: number;
  precioUnitarioEstimado: number;
}

export interface RequerimientoCreateRequest {
  descripcion: string;
  proveedorId: number;
  moneda: Moneda;
  detalles: RequerimientoDetalleRequest[];
}

export interface Aprobacion {
  id: number;
  accion: AccionAprobacion;
  comentario?: string;
  decisionAt: string;
  usuario: string;
}

export interface EstadoHistorial {
  id: number;
  estadoAnterior: EstadoRequerimiento | null;
  estadoNuevo: EstadoRequerimiento;
  comentario?: string;
  usuario: string;
  fechaHora: string;
}

export interface OrdenCompraResumen {
  id: number;
  numero: string;
  moneda: Moneda;
  subtotal: number;
  igv: number;
  total: number;
}

export interface Requerimiento {
  id: number;
  numero: string;
  descripcion: string;
  estado: EstadoRequerimiento;
  moneda: Moneda;
  proveedor: Proveedor;
  detalles: RequerimientoDetalle[];
  aprobaciones: Aprobacion[];
  historialEstados: EstadoHistorial[];
  ordenCompra: OrdenCompraResumen | null;
  createdBy: string;
  createdAt: string;
  updatedBy: string;
  updatedAt: string;
}

export interface OrdenCompraDetalle {
  id: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  almacenId: number;
  almacenCode: string;
  almacenName: string;
  cantidad: number;
  precioUnitario: number;
  subtotalLinea: number;
}

export interface OrdenCompra {
  id: number;
  numero: string;
  requerimientoId: number;
  requerimientoNumero: string;
  proveedor: Proveedor;
  moneda: Moneda;
  tipoCambio: number;
  subtotal: number;
  igv: number;
  total: number;
  generatedAt: string;
  detalles: OrdenCompraDetalle[];
  createdBy: string;
  createdAt: string;
}

export interface AprobacionDecisionRequest {
  comentario?: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PdfHeaderData {
  entidad?: string;
  areaSolicitante?: string;
  oficinaQueAprueba?: string;
  asunto?: string;
  referencia?: string;
  fechaDocumento?: string;
  destinatario?: string;
  cargoDestinatario?: string;
  observaciones?: string;
  pieFirma?: string;
}

export interface MiActividad {
  total: number;
  borradores: number;
  enviados: number;
  observados: number;
  aprobados: number;
  rechazados: number;
  montoEstimado: number;
}

export interface Dashboard {
  totalRequerimientos: number;
  borradores: number;
  pendientesAprobacion: number;
  observados: number;
  rechazados: number;
  aprobados: number;
  convertidosOC: number;
  pendientesPorGenerarOC: number;
  enviadosHoy: number;
  aprobadosHoy: number;
  ocGeneradasHoy: number;
  montoEstimadoPendiente: number;
  miActividad: MiActividad | null;
}
