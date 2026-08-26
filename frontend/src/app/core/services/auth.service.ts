import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginResponse, Usuario } from '../models';
import { SessionService } from '../session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = '/api/auth';

  constructor(
    private readonly http: HttpClient,
    private readonly session: SessionService,
  ) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, { username, password });
  }

  currentUser(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.base}/me`);
  }

  logout(): void {
    this.session.logout();
  }
}
