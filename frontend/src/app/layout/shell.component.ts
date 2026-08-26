import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BreakpointObserver } from '@angular/cdk/layout';
import { SessionService } from '../core/session.service';
import { UserRole } from '../core/models';
import { AuthService } from '../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  link: string;
  roles: UserRole[];
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', icon: 'dashboard', link: '/dashboard', roles: ['SOLICITANTE', 'APROBADOR', 'COMPRAS', 'ADMIN'] },
  { label: 'Mis requerimientos', icon: 'description', link: '/requerimientos', roles: ['SOLICITANTE', 'ADMIN'] },
  { label: 'Aprobaciones', icon: 'fact_check', link: '/aprobaciones', roles: ['APROBADOR', 'ADMIN'] },
  { label: 'Compras', icon: 'shopping_cart', link: '/compras', roles: ['COMPRAS', 'ADMIN'] },
  { label: 'Maestros', icon: 'inventory_2', link: '/maestros', roles: ['ADMIN'] },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
    MatTooltipModule,
  ],
  templateUrl: './shell.component.html',
  styles: [
    `
      .shell { height: 100vh; display: flex; flex-direction: column; }
      .topbar { display: flex; align-items: center; gap: 8px; height: 64px; }
      .topbar .menu-btn { color: #fff; }
      .topbar .title { font-family: 'Poppins', sans-serif; font-weight: 600; font-size: 18px; }
      .topbar .spacer { flex: 1; }
      .topbar .brand-icon { font-size: 22px; }
      .user-box {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 6px 12px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.12);
        color: #fff;
      }
      .user-box .avatar {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        background: #fff;
        color: var(--color-primary);
        display: grid;
        place-items: center;
        font-weight: 700;
      }
      .user-box .meta { display: flex; flex-direction: column; line-height: 1.1; }
      .user-box .name { font-size: 13px; font-weight: 600; }
      .user-box .role { font-size: 11px; opacity: 0.85; }
      .logout-btn { color: #fff !important; }

      .sidenav { width: 264px; border-right: 1px solid var(--color-border); }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 18px 18px 12px;
      }
      .brand .logo {
        width: 38px;
        height: 38px;
        border-radius: 10px;
        background: var(--color-primary);
        color: #fff;
        display: grid;
        place-items: center;
      }
      .brand .name { font-family: 'Poppins', sans-serif; font-weight: 700; font-size: 15px; line-height: 1.1; }
      .brand .sub { font-size: 11px; color: var(--color-text-soft); }

      mat-list { padding-top: 8px; }
      .nav-item {
        border-radius: 10px;
        margin: 2px 10px;
        color: var(--color-text);
      }
      .nav-item.active {
        background: var(--color-primary-soft);
        color: var(--color-primary-strong);
        font-weight: 600;
      }
      .nav-item mat-icon { margin-right: 12px; }
      .content { flex: 1; overflow: auto; }
    `,
  ],
})
export class ShellComponent {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly breakpoint = inject(BreakpointObserver);

  readonly user = this.session.user;
  readonly role = this.session.role;
  readonly isMobile = signal(false);

  readonly navItems = computed<NavItem[]>(() => {
    const role = this.role();
    if (!role) return [];
    return NAV_ITEMS.filter((item) => item.roles.includes(role));
  });

  readonly mobileOpened = signal(false);

  constructor() {
    this.breakpoint.observe('(max-width: 960px)').subscribe((result) => {
      this.isMobile.set(result.matches);
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  goTo(link: string): void {
    this.mobileOpened.set(false);
    this.router.navigate([link]);
  }
}
