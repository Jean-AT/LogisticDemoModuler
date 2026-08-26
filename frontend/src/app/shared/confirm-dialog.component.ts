import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  showComentario?: boolean;
  comentarioLabel?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="wrap">
      <div class="head">
        <mat-icon [class.danger]="data.danger">{{ data.danger ? 'warning' : 'help' }}</mat-icon>
        <h3>{{ data.title }}</h3>
      </div>
      <p class="msg">{{ data.message }}</p>
      <mat-form-field appearance="outline" class="full" *ngIf="data.showComentario">
        <mat-label>{{ data.comentarioLabel || 'Comentario' }}</mat-label>
        <textarea matInput rows="3" [formControl]="comentario" placeholder="Escribe el motivo..."></textarea>
      </mat-form-field>
      <div class="actions">
        <button mat-button (click)="cancel()">{{ data.cancelText || 'Cancelar' }}</button>
        <button
          mat-flat-button
          [class]="data.danger ? 'btn-danger' : 'btn-primary'"
          (click)="confirm()"
          [disabled]="data.showComentario && !comentario.value?.trim()"
        >
          {{ data.confirmText || 'Confirmar' }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .wrap { max-width: 420px; }
      .head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
      .head h3 { margin: 0; font-size: 18px; }
      .head mat-icon { font-size: 28px; height: 28px; width: 28px; color: var(--color-primary); }
      .head mat-icon.danger { color: var(--color-danger); }
      .msg { color: var(--color-text-soft); margin: 0 0 16px; }
      .full { width: 100%; }
      .actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
    `,
  ],
})
export class ConfirmDialogComponent {
  readonly comentario = new FormControl<string>('');

  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData,
  ) {}

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    this.dialogRef.close(this.data.showComentario ? (this.comentario.value ?? '') : true);
  }
}
