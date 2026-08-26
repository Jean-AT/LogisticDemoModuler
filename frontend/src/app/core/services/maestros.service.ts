import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Almacen, Item, Proveedor } from '../models';

@Injectable({ providedIn: 'root' })
export class MaestrosService {
  private readonly base = '/api';

  constructor(private readonly http: HttpClient) {}

  getItems(): Observable<Item[]> {
    return this.http.get<Item[]>(`${this.base}/items`);
  }

  createItem(payload: { code: string; name: string; unitMeasure: string }): Observable<Item> {
    return this.http.post<Item>(`${this.base}/items`, payload);
  }

  getAlmacenes(): Observable<Almacen[]> {
    return this.http.get<Almacen[]>(`${this.base}/almacenes`);
  }

  createAlmacen(payload: { code: string; name: string }): Observable<Almacen> {
    return this.http.post<Almacen>(`${this.base}/almacenes`, payload);
  }

  getProveedores(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(`${this.base}/proveedores`);
  }

  createProveedor(payload: { code: string; name: string }): Observable<Proveedor> {
    return this.http.post<Proveedor>(`${this.base}/proveedores`, payload);
  }
}
