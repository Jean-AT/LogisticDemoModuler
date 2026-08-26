import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { RequerimientosService } from '../../core/services/requerimientos.service';
import { Aprobacion, EstadoHistorial, Requerimiento } from '../../core/models';
import { accionLabel, errorMessage, formatAmount, formatDate, formatMoney } from '../../core/utils';

@Component({
  selector: 'app-requerimiento-detalle',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatCardModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    PageHeaderComponent,
    StatusChipComponent,
  ],
  templateUrl: './requerimiento-detalle.component.html',
  styles: [
    `
      .grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
      @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
      .card { padding: 20px; }
      .grid h3 { margin-top: 0; }
      .detail-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
      .detail-list .k { display: block; font-size: 12px; color: var(--color-text-soft); }
      .detail-list .v { font-weight: 600; }
      table { width: 100%; }
      .totals { display: flex; justify-content: flex-end; gap: 24px; font-size: 14px; }
      .totals b { font-size: 18px; }
      .timeline { position: relative; padding-left: 20px; }
      .timeline .tl-item { position: relative; padding-bottom: 18px; border-left: 2px solid var(--color-border); margin-left: 6px; padding-left: 16px; }
      .timeline .tl-item:last-child { border-left-color: transparent; }
      .tl-date { font-size: 12px; color: var(--color-text-soft); }
      .tl-title { display: flex; align-items: center; gap: 8px; font-weight: 600; }
      .oc-link { margin-top: 12px; }
    `,
  ],
})
export class RequerimientoDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(RequerimientosService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  readonly req = signal<Requerimiento | null>(null);
  readonly loading = signal(true);
  readonly columns = ['item', 'almacen', 'cantidad', 'precio', 'subtotal'];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.getById(id).subscribe({
      next: (r) => this.req.set(r),
      error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
      complete: () => this.loading.set(false),
    });
  }

  reload(): void {
    const id = this.req()!.id;
    this.service.getById(id).subscribe((r) => this.req.set(r));
  }

  enviar(): void {
    const r = this.req()!;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Enviar requerimiento', message: `¿Envías ${r.numero} a aprobación?`, confirmText: 'Enviar' },
    });
    ref.afterClosed().subscribe((res) => {
      if (res) {
        this.service.enviar(r.id).subscribe({
          next: () => {
            this.snack.open('Enviado a aprobación', 'OK', { duration: 3000 });
            this.reload();
          },
          error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
        });
      }
    });
  }

  editar(): void {
    this.router.navigate(['/requerimientos', this.req()!.id, 'editar']);
  }

  money(v: number, moneda: string) {
    return formatMoney(v, moneda as 'PEN' | 'USD');
  }

  amount(v: number) {
    return formatAmount(v);
  }

  fecha(v: string) {
    return formatDate(v);
  }

  accionLabel(a: Aprobacion) {
    return accionLabel[a.accion] ?? a.accion;
  }

  goOC(): void {
    this.router.navigate(['/compras/ordenes', this.req()!.ordenCompra!.id]);
  }

  historialText(h: EstadoHistorial): string {
    const from = h.estadoAnterior ?? 'INICIAL';
    return `${from} → ${h.estadoNuevo}`;
  }

  subtotal(r: Requerimiento): number {
    return r.detalles.reduce((s, d) => s + d.subtotalLinea, 0);
  }

  igv(r: Requerimiento): number {
    return this.subtotal(r) * 0.18;
  }

  total(r: Requerimiento): number {
    return this.subtotal(r) + this.igv(r);
  }
}
