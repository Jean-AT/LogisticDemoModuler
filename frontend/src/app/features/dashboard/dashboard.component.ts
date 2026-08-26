import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatCardComponent } from '../../shared/stat-card.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { RequerimientosService } from '../../core/services/requerimientos.service';
import { SessionService } from '../../core/session.service';
import { Dashboard, Requerimiento } from '../../core/models';
import { estadoChip, formatAmount, formatDate } from '../../core/utils';
import { UserRole } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    StatCardComponent,
    StatusChipComponent,
    EmptyStateComponent,
  ],
  templateUrl: './dashboard.component.html',
    styles: [
      `
      .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
      .section { margin-bottom: 24px; }
      .section h3 { font-size: 16px; margin: 0 0 12px; }
      .section .card {
        padding: 8px 16px 16px;
        border: 1px solid var(--color-border);
        background: rgba(255, 255, 255, 0.78);
        backdrop-filter: blur(14px);
        -webkit-backdrop-filter: blur(14px);
      }
      table { width: 100%; }
      .row-link { cursor: pointer; }
      .cta { margin-top: 8px; }
    `,
    ],
})
export class DashboardComponent implements OnInit {
  private readonly dashboard = inject(DashboardService);
  private readonly requerimientos = inject(RequerimientosService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly recentes = signal<Requerimiento[]>([]);
  readonly role = this.session.role;
  readonly columns = ['numero', 'descripcion', 'proveedor', 'estado', 'fecha', 'acciones'];

  readonly isSolicitante = computed(() => this.role() === 'SOLICITANTE' || this.role() === 'ADMIN');

  ngOnInit(): void {
    this.dashboard.resumen().subscribe({
      next: (d) => this.data.set(d),
      complete: () => this.loading.set(false),
      error: () => this.loading.set(false),
    });
    const estado = this.role() === 'COMPRAS' ? 'APROBADO' : this.role() === 'APROBADOR' ? 'ENVIADO' : undefined;
    this.requerimientos.list({ estado, page: 0, size: 5 }).subscribe((page) => this.recentes.set(page.content));
  }

  estadoChip(estado: string) {
    return estadoChip(estado);
  }

  money(value: number | undefined) {
    return formatAmount(value);
  }

  fecha(value: string | undefined) {
    return formatDate(value);
  }

  goRequerimiento(id: number): void {
    if (this.role() === 'APROBADOR') {
      this.router.navigate(['/aprobaciones', id]);
    } else if (this.role() === 'COMPRAS') {
      this.router.navigate(['/compras']);
    } else {
      this.router.navigate(['/requerimientos', id]);
    }
  }

  goNuevo(): void {
    this.router.navigate(['/requerimientos/nuevo']);
  }
}
