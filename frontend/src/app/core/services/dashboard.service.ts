import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Dashboard } from '../models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly base = '/api/dashboard';

  constructor(private readonly http: HttpClient) {}

  resumen(): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.base);
  }
}
