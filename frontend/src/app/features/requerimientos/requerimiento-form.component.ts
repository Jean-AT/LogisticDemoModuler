import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { MaestrosService } from '../../core/services/maestros.service';
import { RequerimientosService } from '../../core/services/requerimientos.service';
import { Almacen, Item, Moneda, Proveedor } from '../../core/models';
import { errorMessage, formatAmount } from '../../core/utils';

@Component({
  selector: 'app-requerimiento-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
  ],
  templateUrl: './requerimiento-form.component.html',
    styles: [
      `
      .form-header { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 16px; }
      .detalle-card { padding: 16px; margin-bottom: 16px; }
      .detalle-row {
        display: grid;
        grid-template-columns: 1.6fr 1.6fr 90px 140px 110px 48px;
        gap: 10px;
        align-items: center;
        margin-bottom: 8px;
      }
      .totals { display: flex; justify-content: flex-end; gap: 24px; padding: 8px 0; font-size: 15px; }
      .totals b { font-size: 18px; }
      .sticky-action-bar { margin-top: 12px; }
      .full { width: 100%; }

      @media (max-width: 800px) {
        .form-header { grid-template-columns: 1fr; }
      }
      @media (max-width: 760px) {
        .detalle-row { grid-template-columns: 1fr 1fr; gap: 8px; }
        .detalle-row .money { justify-self: end; }
        .detalle-row .del-btn { justify-self: end; }
        .detalle-row.header { display: none; }
        .sticky-action-bar { flex-wrap: wrap; }
        .sticky-action-bar .grow-btn { flex: 1 1 100%; }
      }
      `,
    ],
})
export class RequerimientoFormComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly maestros = inject(MaestrosService);
  private readonly service = inject(RequerimientosService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly editId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly items = signal<Item[]>([]);
  readonly almacenes = signal<Almacen[]>([]);
  readonly proveedores = signal<Proveedor[]>([]);
  readonly monedas: Moneda[] = ['PEN', 'USD'];

  readonly rows = signal<number[]>([]);
  readonly subtotales = signal<number[]>([]);
  readonly total = signal<number>(0);

  readonly form = this.fb.group({
    descripcion: this.fb.control('', Validators.required),
    proveedorId: this.fb.control<number | null>(null, Validators.required),
    moneda: this.fb.control<Moneda>('PEN', Validators.required),
    detalles: this.fb.array<any>([]),
  });

  get detalles(): FormArray {
    return this.form.get('detalles') as FormArray;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.editId.set(idParam ? Number(idParam) : null);

    this.maestros.getProveedores().subscribe((r) => this.proveedores.set(r));
    this.maestros.getItems().subscribe((r) => this.items.set(r));
    this.maestros.getAlmacenes().subscribe((r) => this.almacenes.set(r));

    this.form.valueChanges.subscribe(() => this.recompute());

    if (this.editId()) {
      this.service.getById(this.editId()!).subscribe({
        next: (req) => {
          this.form.patchValue({
            descripcion: req.descripcion,
            proveedorId: req.proveedor.id,
            moneda: req.moneda,
          });
          this.detalles.clear();
          for (const d of req.detalles) {
            this.detalles.push(this.nuevaFila(d.itemId, d.almacenId, d.cantidad, d.precioUnitarioEstimado));
          }
          this.recompute();
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.snack.open(errorMessage(err), 'Cerrar');
        },
      });
    } else {
      this.detalles.push(this.nuevaFila(null, null, 1, 0));
      this.recompute();
      this.loading.set(false);
    }
  }

  private nuevaFila(itemId: number | null, almacenId: number | null, cantidad: number, precio: number): AbstractControl {
    return this.fb.group({
      itemId: [itemId, Validators.required],
      almacenId: [almacenId, Validators.required],
      cantidad: [cantidad, Validators.min(1)],
      precioUnitarioEstimado: [precio, Validators.min(0)],
    });
  }

  private recompute(): void {
    const controls = this.detalles.controls;
    const subs = controls.map((c) => {
      const v = c.getRawValue();
      return (v.cantidad || 0) * (v.precioUnitarioEstimado || 0);
    });
    this.subtotales.set(subs);
    this.total.set(subs.reduce((a, b) => a + b, 0));
    this.rows.set(controls.map((_, i) => i));
    this.cdr.markForCheck();
  }

  agregarFila(): void {
    this.detalles.push(this.nuevaFila(null, null, 1, 0));
    this.recompute();
  }

  quitarFila(index: number): void {
    this.detalles.removeAt(index);
    this.recompute();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snack.open('Revisa los campos obligatorios', 'Cerrar');
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      descripcion: value.descripcion.trim(),
      proveedorId: value.proveedorId!,
      moneda: value.moneda,
      detalles: this.detalles.controls.map((d) => d.getRawValue()),
    };
    this.saving.set(true);
    const call = this.editId() ? this.service.update(this.editId()!, payload) : this.service.create(payload);
    call.subscribe({
      next: (req) => {
        this.saving.set(false);
        this.snack.open('Requerimiento guardado en borrador', 'OK', { duration: 3000 });
        if (!this.editId()) {
          this.router.navigate(['/requerimientos', req.id], { queryParams: { creado: 'true' } });
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.snack.open(errorMessage(err), 'Cerrar');
      },
    });
  }

  amount(v: number): string {
    return formatAmount(v);
  }

  cancelar(): void {
    this.router.navigate([this.editId() ? '/requerimientos/' + this.editId() : '/requerimientos']);
  }
}
