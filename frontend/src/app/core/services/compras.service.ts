import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Moneda, OrdenCompra, Page, PdfHeaderData } from '../models';

@Injectable({ providedIn: 'root' })
export class ComprasService {
  private readonly base = '/api/ordenes-compra';

  constructor(private readonly http: HttpClient) {}

  generarDesdeRequerimiento(requerimientoId: number): Observable<OrdenCompra> {
    return this.http.post<OrdenCompra>(`${this.base}/desde-requerimiento/${requerimientoId}`, null);
  }

  list(filtros?: {
    numero?: string;
    proveedorId?: number;
    moneda?: Moneda;
    requerimientoId?: number;
    fechaDesde?: string;
    fechaHasta?: string;
    page?: number;
    size?: number;
  }): Observable<Page<OrdenCompra>> {
    let params = new HttpParams();
    if (filtros?.numero) params = params.set('numero', filtros.numero);
    if (filtros?.proveedorId) params = params.set('proveedorId', filtros.proveedorId);
    if (filtros?.moneda) params = params.set('moneda', filtros.moneda);
    if (filtros?.requerimientoId) params = params.set('requerimientoId', filtros.requerimientoId);
    if (filtros?.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros?.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);
    if (filtros?.page != null) params = params.set('page', filtros.page);
    if (filtros?.size != null) params = params.set('size', filtros.size);
    return this.http.get<Page<OrdenCompra>>(this.base, { params });
  }

  getById(id: number): Observable<OrdenCompra> {
    return this.http.get<OrdenCompra>(`${this.base}/${id}`);
  }

  downloadPdf(id: number, header: PdfHeaderData): Observable<Blob> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(header as Record<string, string | undefined>)) {
      if (value) params = params.set(key, value);
    }
    return this.http.get(`${this.base}/${id}/pdf`, { params, responseType: 'blob' });
  }
}
