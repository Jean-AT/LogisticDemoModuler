import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { AprobacionesService } from '../../core/services/aprobaciones.service';
import { EstadoRequerimiento, Page, Requerimiento } from '../../core/models';
import { errorMessage, formatDate } from '../../core/utils';

@Component({
  selector: 'app-aprobaciones-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatInputModule,
    MatDialogModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    StatusChipComponent,
    EmptyStateComponent,
  ],
  templateUrl: './aprobaciones-list.component.html',
    styles: [
      `
      .filters { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; padding: 16px; margin-bottom: 16px; }
      .filters mat-form-field { width: 180px; }
      @media (max-width: 640px) {
        .filters mat-form-field { width: 100%; }
        .filters button { flex: 1; }
      }
    `,
    ],
})
export class AprobacionesListComponent implements OnInit {
  private readonly service = inject(AprobacionesService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  readonly columns = ['numero', 'descripcion', 'proveedor', 'moneda', 'fecha', 'estado', 'solicitante', 'acciones'];
  readonly page = signal<Page<Requerimiento> | null>(null);
  readonly loading = signal(false);

  readonly fNumero = new FormControl('');
  readonly fEstado = new FormControl<EstadoRequerimiento>('ENVIADO');
  readonly fDesde = new FormControl('');
  readonly fHasta = new FormControl('');

  readonly estados: EstadoRequerimiento[] = ['ENVIADO', 'APROBADO', 'OBSERVADO', 'RECHAZADO', 'CONVERTIDO_OC'];

  ngOnInit(): void {
    this.cargar(0, 10);
  }

  cargar(page: number, size: number): void {
    this.loading.set(true);
    this.service
      .list({
        estado: this.fEstado.value || undefined,
        numero: this.fNumero.value || undefined,
        fechaDesde: this.fDesde.value || undefined,
        fechaHasta: this.fHasta.value || undefined,
        page,
        size,
      })
      .subscribe({
        next: (p) => this.page.set(p),
        error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
        complete: () => this.loading.set(false),
      });
  }

  buscar(): void {
    this.cargar(0, this.page()?.size ?? 10);
  }

  limpiar(): void {
    this.fNumero.reset();
    this.fEstado.setValue('ENVIADO');
    this.fDesde.reset();
    this.fHasta.reset();
    this.buscar();
  }

  ver(id: number): void {
    this.router.navigate(['/aprobaciones', id]);
  }

  private decidir(r: Requerimiento, accion: 'aprobar' | 'observar' | 'rechazar'): void {
    const needComentario = accion !== 'aprobar';
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '460px',
      data: {
        title: `¿${accion} el requerimiento ${r.numero}?`,
        message:
          accion === 'aprobar'
            ? `Confirma la aprobación de ${r.numero}.`
            : accion === 'observar'
              ? `Se marcará como observado. Comentario obligatorio.`
              : `Se rechazará definitivamente. Comentario obligatorio.`,
        confirmText: accion.charAt(0).toUpperCase() + accion.slice(1),
        danger: accion === 'rechazar',
        showComentario: needComentario,
        comentarioLabel: 'Comentario',
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
      const call =
        accion === 'aprobar'
          ? this.service.aprobar(r.id, payload)
          : accion === 'observar'
            ? this.service.observar(r.id, payload)
            : this.service.rechazar(r.id, payload);
      call.subscribe({
        next: () => {
          this.snack.open('Decisión registrada', 'OK', { duration: 3000 });
          this.buscar();
        },
        error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
      });
    });
  }

  aprobar(r: Requerimiento): void {
    this.decidir(r, 'aprobar');
  }

  observar(r: Requerimiento): void {
    this.decidir(r, 'observar');
  }

  rechazar(r: Requerimiento): void {
    this.decidir(r, 'rechazar');
  }

  fecha(value: string): string {
    return formatDate(value);
  }
}
