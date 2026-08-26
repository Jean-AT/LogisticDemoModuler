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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { RequerimientosService } from '../../core/services/requerimientos.service';
import { EstadoRequerimiento, Page, Requerimiento } from '../../core/models';
import { errorMessage, formatDate } from '../../core/utils';

@Component({
  selector: 'app-requerimientos-list',
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
    MatProgressSpinnerModule,
    MatPaginatorModule,
    PageHeaderComponent,
    StatusChipComponent,
    EmptyStateComponent,
  ],
  templateUrl: './requerimientos-list.component.html',
  styles: [
    `
      .filters {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
        align-items: center;
        padding: 16px;
        margin-bottom: 16px;
      }
      .filters mat-form-field { width: 180px; }
      @media (max-width: 640px) {
        .filters mat-form-field { width: 100%; }
        .filters button { flex: 1; }
      }
      .btn-row { padding: 16px 16px 0; }
    `,
  ],
})
export class RequerimientosListComponent implements OnInit {
  private readonly service = inject(RequerimientosService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  readonly columns = ['numero', 'descripcion', 'proveedor', 'moneda', 'estado', 'fecha', 'acciones'];
  readonly loading = signal(false);
  readonly page = signal<Page<Requerimiento> | null>(null);

  readonly fNumero = new FormControl('');
  readonly fEstado = new FormControl<EstadoRequerimiento | ''>('');
  readonly fDesde = new FormControl('');
  readonly fHasta = new FormControl('');

  readonly estados: EstadoRequerimiento[] = ['BORRADOR', 'ENVIADO', 'APROBADO', 'OBSERVADO', 'RECHAZADO', 'CONVERTIDO_OC'];

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
    this.fEstado.reset();
    this.fDesde.reset();
    this.fHasta.reset();
    this.buscar();
  }

  ver(id: number): void {
    this.router.navigate(['/requerimientos', id]);
  }

  nuevo(): void {
    this.router.navigate(['/requerimientos/nuevo']);
  }

  enviar(r: Requerimiento): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Enviar requerimiento',
        message: `¿Envías el requerimiento ${r.numero} a aprobación?`,
        confirmText: 'Enviar',
      },
    });
    ref.afterClosed().subscribe((res) => {
      if (res) {
        this.service.enviar(r.id).subscribe({
          next: () => {
            this.snack.open(`Requerimiento ${r.numero} enviado a aprobación`, 'OK', { duration: 3000 });
            this.buscar();
          },
          error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
        });
      }
    });
  }

  fecha(value: string): string {
    return formatDate(value);
  }
}
