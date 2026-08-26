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
      .topbar {
        display: flex;
        align-items: center;
        gap: 8px;
        height: 64px;
        background: rgba(91, 60, 196, 0.70) !important;
        --mat-toolbar-container-background-color: transparent;
        --mat-toolbar-container-text-color: #fff;
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
        border-bottom: 1px solid rgba(255, 255, 255, 0.18);
      }
      .topbar .menu-btn { color: #fff; }
      .topbar .title { font-family: 'Poppins', sans-serif; font-weight: 600; font-size: 18px; white-space: nowrap; flex-shrink: 1; overflow: hidden; text-overflow: ellipsis; min-width: 0; }
      .topbar .spacer { flex: 1; }
      .topbar .brand-icon { font-size: 22px; flex-shrink: 0; }
      @media (max-width: 640px) {
        .topbar { height: 56px; }
        .topbar .title { font-size: 15px; }
        .user-box .meta { display: none; }
      }
      .user-box {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 6px 12px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.16);
        border: 1px solid rgba(255, 255, 255, 0.25);
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

      .sidenav {
        width: 300px;
        height: 850px;
        margin: 1rem;
        background: rgba(255, 255, 255, 0.72) !important;
        backdrop-filter: blur(18px);
        -webkit-backdrop-filter: blur(18px);
        border: 1px solid var(--color-border);
        border-radius: 1rem;
        box-shadow: 4px 0 24px rgba(36, 27, 57, 0.06);
      }
      .sidenav ::ng-deep .mat-drawer-inner-container {
        overflow-x: hidden;
        overflow-y: auto;
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 14px 14px 10px;
      }
      .brand .logo {
        width: 32px;
        height: 32px;
        border-radius: 9px;
        background: var(--color-primary);
        color: #fff;
        display: grid;
        place-items: center;
        box-shadow: 0 6px 20px rgba(91, 60, 196, 0.35);
      }
      .brand .name { font-family: 'Poppins', sans-serif; font-weight: 700; font-size: 14px; line-height: 1.1; }
      .brand .sub { font-size: 11px; color: var(--color-text-soft); }

      mat-list { padding-top: 4px; }
      .nav-item {
        border-radius: 10px;
        margin: 2px 8px;
        color: var(--color-text);
        --mat-list-list-item-leading-icon-start-space: 14px;
        --mat-list-list-item-leading-icon-end-space: 14px;
      }
      .nav-item.active {
        background: rgba(238, 232, 255, 0.9);
        color: var(--color-primary-strong);
        font-weight: 600;
      }
      .nav-item mat-icon { margin-right: 10px; }
      .content { flex: 1; overflow: auto; padding: 24px; }
      .content-inner { max-width: 1200px; margin: 0 1rem; width: 100%; }
      .rest { flex: 1; min-height: 0; }
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
