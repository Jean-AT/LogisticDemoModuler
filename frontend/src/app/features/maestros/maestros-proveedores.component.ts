import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { MaestrosService } from '../../core/services/maestros.service';
import { Proveedor } from '../../core/models';
import { errorMessage } from '../../core/utils';

@Component({
  selector: 'app-maestros-proveedores',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    EmptyStateComponent,
  ],
  templateUrl: './maestros-proveedores.component.html',
  styles: [
    `table { width: 100%; }
     .form-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }`,
  ],
})
export class MaestrosProveedoresComponent implements OnInit {
  private readonly service = inject(MaestrosService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly proveedores = signal<Proveedor[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly columns = ['code', 'name'];

  readonly form = this.fb.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
  });

  ngOnInit(): void {
    this.service.getProveedores().subscribe({
      next: (r) => this.proveedores.set(r),
      complete: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.snack.open(errorMessage(err), 'Cerrar'); },
    });
  }

  toggleForm(): void {
    this.showForm.set(!this.showForm());
  }

  submit(): void {
    if (this.form.invalid) return;
    this.service.createProveedor(this.form.getRawValue() as { code: string; name: string }).subscribe({
      next: (p) => {
        this.snack.open(`Proveedor ${p.code} creado`, 'OK', { duration: 3000 });
        this.proveedores.update((list) => [...list, p]);
        this.form.reset();
        this.showForm.set(false);
      },
      error: (err) => this.snack.open(errorMessage(err), 'Cerrar'),
    });
  }
}
