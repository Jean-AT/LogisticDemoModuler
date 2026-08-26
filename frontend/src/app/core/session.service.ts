import { Injectable, computed, signal } from '@angular/core';
import { Usuario, UserRole } from './models';

const TOKEN_KEY = 'logistica.token';
const USER_KEY = 'logistica.user';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly tokenSignal = signal<string | null>(readToken());
  private readonly userSignal = signal<Usuario | null>(readUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal() && !!this.userSignal());
  readonly role = computed<UserRole | null>(() => this.userSignal()?.role ?? null);

  setSession(token: string, user: Usuario): void {
    this.tokenSignal.set(token);
    this.userSignal.set(user);
    safeWrite(() => localStorage.setItem(TOKEN_KEY, token));
    safeWrite(() => localStorage.setItem(USER_KEY, JSON.stringify(user)));
  }

  updateUser(user: Usuario): void {
    this.userSignal.set(user);
    safeWrite(() => localStorage.setItem(USER_KEY, JSON.stringify(user)));
  }

  logout(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    safeWrite(() => localStorage.removeItem(TOKEN_KEY));
    safeWrite(() => localStorage.removeItem(USER_KEY));
  }

  hasAnyRole(...roles: UserRole[]): boolean {
    const current = this.role();
    return current !== null && roles.includes(current);
  }
}

function safeWrite(action: () => void): void {
  try {
    action();
  } catch {
    // El almacenamiento puede estar bloqueado (file://, privacidad). No debe romper la app.
  }
}

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function readUser(): Usuario | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw) as Usuario;
  } catch {
    return null;
  }
}
