import { ChangeDetectionStrategy, Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { PdfHeaderFormComponent } from '../../shared/pdf-header-form.component';
import { RequerimientosService } from '../../core/services/requerimientos.service';
import { AprobacionesService } from '../../core/services/aprobaciones.service';
import { PdfHeaderData, Requerimiento } from '../../core/models';
import { accionLabel, downloadBlob, errorMessage, formatDate, formatMoney } from '../../core/utils';
@Component({
  selector: 'app-aprobacion-detalle',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    PageHeaderComponent,
    StatusChipComponent,
    PdfHeaderFormComponent,
  ],
  templateUrl: './aprobacion-detalle.component.html',
  styles: [
    `
      .grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
      @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
      .card { padding: 20px; margin-bottom: 20px; }
      .detail-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
      .detail-list .k { display: block; font-size: 12px; color: var(--color-text-soft); }
      .detail-list .v { font-weight: 600; }
      table { width: 100%; }
      .totals { display: flex; justify-content: flex-end; gap: 24px; font-size: 14px; }
      .totals b { font-size: 18px; }
      .timeline { display: flex; flex-direction: column; gap: 14px; }
      .timeline .tl { display: flex; align-items: center; gap: 10px; font-size: 14px; }
      .timeline .tl .text-soft { flex-basis: 100%; margin-top: 2px; }
    `,
  ],
})
export class AprobacionDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly serviceReq = inject(RequerimientosService);
  private readonly service = inject(AprobacionesService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  @ViewChild(PdfHeaderFormComponent) headerForm!: PdfHeaderFormComponent;

  readonly req = signal<Requerimiento | null>(null);
  readonly loading = signal(true);
  readonly exporting = signal(false);
  readonly columns = ['item', 'almacen', 'cantidad', 'precio', 'subtotal'];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.serviceReq.getById(id).subscribe({
      next: (r) => this.req.set(r),
      error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
      complete: () => this.loading.set(false),
    });
  }

  reload(): void {
    this.serviceReq.getById(this.req()!.id).subscribe((r) => this.req.set(r));
  }

  money(v: number, moneda: string) {
    return formatMoney(v, moneda as 'PEN' | 'USD');
  }

  fecha(v: string) {
    return formatDate(v);
  }

  subtotal(r: Requerimiento): number {
    return r.detalles.reduce((s, d) => s + d.subtotalLinea, 0);
  }

  accionLabel(a: { accion: string }): string {
    return accionLabel[a.accion] ?? a.accion;
  }

  decidir(accion: 'aprobar' | 'observar' | 'rechazar'): void {
    const r = this.req()!;
    const needComentario = accion !== 'aprobar';
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '480px',
      data: {
        title: `¿${accion} ${r.numero}?`,
        message:
          accion === 'aprobar'
            ? `Confirmas la aprobación de ${r.numero}.`
            : accion === 'observar'
              ? `El requerimiento quedará como observado.`
              : `El requerimiento será rechazado definitivamente.`,
        confirmText: accion.charAt(0).toUpperCase() + accion.slice(1),
        danger: accion === 'rechazar',
        showComentario: needComentario,
      },
    });
    ref.afterClosed().subscribe((comentario) => {
      if (comentario === null) return;
      const payload =
        accion === 'aprobar'
          ? typeof comentario === 'string' && comentario.trim()
            ? { comentario }
            : {}
          : { comentario };
      const call = accion === 'aprobar' ? this.service.aprobar(r.id, payload) : accion === 'observar' ? this.service.observar(r.id, payload) : this.service.rechazar(r.id, payload);
      call.subscribe({
        next: () => {
          this.snack.open('Decisión registrada', 'OK', { duration: 3000 });
          this.reload();
        },
        error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
      });
    });
  }

  exportarPdf(): void {
    const r = this.req()!;
    const header: PdfHeaderData = this.headerForm?.value() ?? {};
    this.exporting.set(true);
    this.service.downloadPdf(r.id, header).subscribe({
      next: (blob) => downloadBlob(blob, `aprobacion-${r.numero}.pdf`),
      error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
      complete: () => this.exporting.set(false),
    });
  }

  verRequerimiento(): void {
    this.router.navigate(['/requerimientos', this.req()!.id]);
  }

  volver(): void {
    this.router.navigate(['/aprobaciones']);
  }
}
