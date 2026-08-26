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
import { Item } from '../../core/models';
import { errorMessage } from '../../core/utils';

@Component({
  selector: 'app-maestros-items',
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
  templateUrl: './maestros-items.component.html',
  styles: [
    `table { width: 100%; }
     .form-card { padding: 20px; margin-bottom: 16px; }
     .form-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }`,
  ],
})
export class MaestrosItemsComponent implements OnInit {
  private readonly service = inject(MaestrosService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);

  readonly items = signal<Item[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly columns = ['code', 'name', 'unit'] as const;

  readonly form = this.fb.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    unitMeasure: ['', Validators.required],
  });

  ngOnInit(): void {
    this.service.getItems().subscribe({
      next: (r) => this.items.set(r),
      complete: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.snack.open(errorMessage(err), 'Cerrar'); },
    });
  }

  toggleForm(): void {
    this.showForm.set(!this.showForm());
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.service.createItem(this.form.getRawValue() as { code: string; name: string; unitMeasure: string }).subscribe({
      next: (it) => {
        this.snack.open(`Ítem ${it.code} creado`, 'OK', { duration: 3000 });
        this.items.update((list) => [...list, it]);
        this.form.reset();
        this.showForm.set(false);
        this.saving.set(false);
      },
      error: (err) => { this.saving.set(false); this.snack.open(errorMessage(err), 'Cerrar'); },
    });
  }
}
