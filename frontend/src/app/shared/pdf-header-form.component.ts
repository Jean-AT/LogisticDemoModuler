import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { PdfHeaderData } from '../core/models';

@Component({
  selector: 'app-pdf-header-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatExpansionModule],
  template: `
    <mat-expansion-panel [expanded]="expanded">
      <mat-expansion-panel-header>
        <mat-panel-title>Encabezado del documento</mat-panel-title>
        <mat-panel-description>Datos que aparecen en el PDF</mat-panel-description>
      </mat-expansion-panel-header>
      <form class="grid" [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field appearance="outline"><mat-label>Entidad</mat-label><input matInput formControlName="entidad"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Área solicitante</mat-label><input matInput formControlName="areaSolicitante"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Oficina que aprueba</mat-label><input matInput formControlName="oficinaQueAprueba"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Asunto</mat-label><input matInput formControlName="asunto"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Referencia</mat-label><input matInput formControlName="referencia"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Fecha del documento</mat-label><input matInput formControlName="fechaDocumento"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Destinatario</mat-label><input matInput formControlName="destinatario"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Cargo del destinatario</mat-label><input matInput formControlName="cargoDestinatario"></mat-form-field>
        <mat-form-field appearance="outline" class="wide"><mat-label>Observaciones de oficio</mat-label><input matInput formControlName="observaciones"></mat-form-field>
        <mat-form-field appearance="outline" class="wide"><mat-label>Pie de firma</mat-label><input matInput formControlName="pieFirma"></mat-form-field>
      </form>
    </mat-expansion-panel>
  `,
  styles: [
    `
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 12px;
        padding-top: 8px;
      }
      .grid .wide { grid-column: 1 / -1; }
    `,
  ],
})
export class PdfHeaderFormComponent implements OnInit {
  @Input() model: PdfHeaderData = {};
  @Input() expanded = false;
  @Output() submitRequested = new EventEmitter<PdfHeaderData>();

  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    entidad: [''],
    areaSolicitante: [''],
    oficinaQueAprueba: [''],
    asunto: [''],
    referencia: [''],
    fechaDocumento: [''],
    destinatario: [''],
    cargoDestinatario: [''],
    observaciones: [''],
    pieFirma: [''],
  });

  ngOnInit(): void {
    this.form.patchValue(this.model ?? {});
  }

  submit(): void {
    this.submitRequested.emit(this.value());
  }

  value(): PdfHeaderData {
    return this.form.value as PdfHeaderData;
  }
}
