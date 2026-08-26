import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AprobacionDecisionRequest, Page, PdfHeaderData, Requerimiento } from '../models';

@Injectable({ providedIn: 'root' })
export class AprobacionesService {
  private readonly base = '/api/aprobaciones';

  constructor(private readonly http: HttpClient) {}

  list(filtros?: {
    id?: number;
    estado?: string;
    numero?: string;
    proveedorId?: number;
    fechaDesde?: string;
    fechaHasta?: string;
    page?: number;
    size?: number;
  }): Observable<Page<Requerimiento>> {
    let params = new HttpParams();
    if (filtros?.id) params = params.set('id', filtros.id);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.numero) params = params.set('numero', filtros.numero);
    if (filtros?.proveedorId) params = params.set('proveedorId', filtros.proveedorId);
    if (filtros?.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros?.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);
    if (filtros?.page != null) params = params.set('page', filtros.page);
    if (filtros?.size != null) params = params.set('size', filtros.size);
    return this.http.get<Page<Requerimiento>>(this.base, { params });
  }

  aprobar(id: number, request?: AprobacionDecisionRequest): Observable<Requerimiento> {
    return this.http.post<Requerimiento>(`${this.base}/${id}/aprobar`, request ?? {});
  }

  observar(id: number, request: AprobacionDecisionRequest): Observable<Requerimiento> {
    return this.http.post<Requerimiento>(`${this.base}/${id}/observar`, request);
  }

  rechazar(id: number, request: AprobacionDecisionRequest): Observable<Requerimiento> {
    return this.http.post<Requerimiento>(`${this.base}/${id}/rechazar`, request);
  }

  downloadPdf(id: number, header: PdfHeaderData): Observable<Blob> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(header as Record<string, string | undefined>)) {
      if (value) params = params.set(key, value);
    }
    return this.http.get(`${this.base}/${id}/pdf`, { params, responseType: 'blob' });
  }
}
