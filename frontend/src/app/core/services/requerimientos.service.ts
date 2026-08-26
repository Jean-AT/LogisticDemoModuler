import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EstadoRequerimiento,
  OrdenCompraResumen,
  Page,
  PdfHeaderData,
  Requerimiento,
  RequerimientoCreateRequest,
} from '../models';

@Injectable({ providedIn: 'root' })
export class RequerimientosService {
  private readonly base = '/api/requerimientos';

  constructor(private readonly http: HttpClient) {}

  list(filtros?: {
    estado?: EstadoRequerimiento;
    numero?: string;
    proveedorId?: number;
    fechaDesde?: string;
    fechaHasta?: string;
    page?: number;
    size?: number;
  }): Observable<Page<Requerimiento>> {
    let params = new HttpParams();
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.numero) params = params.set('numero', filtros.numero);
    if (filtros?.proveedorId) params = params.set('proveedorId', filtros.proveedorId);
    if (filtros?.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros?.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);
    if (filtros?.page != null) params = params.set('page', filtros.page);
    if (filtros?.size != null) params = params.set('size', filtros.size);
    return this.http.get<Page<Requerimiento>>(this.base, { params });
  }

  getById(id: number): Observable<Requerimiento> {
    return this.http.get<Requerimiento>(`${this.base}/${id}`);
  }

  create(request: RequerimientoCreateRequest): Observable<Requerimiento> {
    return this.http.post<Requerimiento>(this.base, request);
  }

  update(id: number, request: RequerimientoCreateRequest): Observable<Requerimiento> {
    return this.http.put<Requerimiento>(`${this.base}/${id}`, request);
  }

  enviar(id: number): Observable<Requerimiento> {
    return this.http.post<Requerimiento>(`${this.base}/${id}/enviar`, null);
  }

  downloadPdf(id: number, header: PdfHeaderData): Observable<Blob> {
    let params = new HttpParams();
    params = this.appendHeaderParams(params, header);
    return this.http.get(`${this.base}/${id}/pdf`, { params, responseType: 'blob' });
  }

  private appendHeaderParams(params: HttpParams, header: PdfHeaderData): HttpParams {
    const map = header as Record<string, string | undefined>;
    for (const [key, value] of Object.entries(map)) {
      if (value) params = params.set(key, value);
    }
    return params;
  }
}
