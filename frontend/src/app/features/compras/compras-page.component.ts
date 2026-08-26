import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { RequerimientosService } from '../../core/services/requerimientos.service';
import { ComprasService } from '../../core/services/compras.service';
import { OrdenCompra, Page, Requerimiento } from '../../core/models';
import { errorMessage, formatAmount, formatDate, formatMoney } from '../../core/utils';

@Component({
  selector: 'app-compras-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTabsModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    EmptyStateComponent,
  ],
  templateUrl: './compras-page.component.html',
  styles: [
    `
      .card { padding: 16px; }
      table { width: 100%; }
      .repo { margin-bottom: 16px; }
    `,
  ],
})
export class ComprasPageComponent implements OnInit {
  private readonly serviceReq = inject(RequerimientosService);
  private readonly service = inject(ComprasService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  readonly cola = signal<Requerimiento[]>([]);
  readonly ocs = signal<Page<OrdenCompra> | null>(null);
  readonly loadingCola = signal(true);
  readonly loadingOcs = signal(true);

  readonly colaColumns = ['numero', 'descripcion', 'proveedor', 'moneda', 'actualizado', 'total', 'acciones'];
  readonly ocColumns = ['numero', 'req', 'proveedor', 'moneda', 'total', 'fecha', 'acciones'];

  ngOnInit(): void {
    this.serviceReq.list({ estado: 'APROBADO', page: 0, size: 50 }).subscribe({
      next: (p) => this.cola.set(p.content),
      complete: () => this.loadingCola.set(false),
      error: (err) => { this.loadingCola.set(false); this.snack.open(errorMessage(err), 'Cerrar'); },
    });
    this.loadOcs();
  }

  loadOcs(): void {
    this.service.list({ page: 0, size: 50 }).subscribe({
      next: (p) => this.ocs.set(p),
      complete: () => this.loadingOcs.set(false),
      error: (err) => { this.loadingOcs.set(false); this.snack.open(errorMessage(err), 'Cerrar'); },
    });
  }

  verRequerimiento(id: number): void {
    this.router.navigate(['/requerimientos', id]);
  }

  generarOC(r: Requerimiento): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Generar orden de compra',
        message: `¿Generar la orden de compra para ${r.numero} (${r.proveedor.name})?`,
        confirmText: 'Generar OC',
      },
    });
    ref.afterClosed().subscribe((res) => {
      if (!res) return;
      this.service.generarDesdeRequerimiento(r.id).subscribe({
        next: (oc) => {
          this.snack.open(`OC ${oc.numero} generada`, 'OK', { duration: 3000 });
          this.router.navigate(['/compras/ordenes', oc.id]);
        },
        error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
      });
    });
  }

  verOC(id: number): void {
    this.router.navigate(['/compras/ordenes', id]);
  }

  money(v: number, moneda: string) {
    return formatMoney(v, moneda as 'PEN' | 'USD');
  }

  amount(v: number) {
    return formatAmount(v);
  }

  total(r: Requerimiento): number {
    const sub = r.detalles.reduce((s, d) => s + d.subtotalLinea, 0);
    return sub * 1.18;
  }

  fecha(v: string) {
    return formatDate(v);
  }
}
