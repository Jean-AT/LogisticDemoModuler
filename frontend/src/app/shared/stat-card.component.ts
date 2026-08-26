import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="stat">
      <div class="icon" [style.background]="bg" [style.color]="color">
        <mat-icon>{{ icon }}</mat-icon>
      </div>
      <div class="content">
        <div class="label">{{ label }}</div>
        <div class="value">{{ value }}</div>
        <div class="hint" *ngIf="hint">{{ hint }}</div>
      </div>
    </div>
  `,
  styles: [
    `
      .stat {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 16px;
      }
      .icon {
        width: 46px;
        height: 46px;
        border-radius: 12px;
        display: grid;
        place-items: center;
      }
      .label { font-size: 13px; color: var(--color-text-soft); }
      .value { font-size: 26px; font-weight: 700; color: var(--color-text); font-family: 'Poppins', sans-serif; }
      .hint { font-size: 12px; color: var(--color-text-soft); }
    `,
  ],
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: number | string = 0;
  @Input() icon = 'bar_chart';
  @Input() color = 'var(--color-primary)';
  @Input() bg = 'var(--color-primary-soft)';
  @Input() hint = '';
}
