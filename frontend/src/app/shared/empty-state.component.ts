import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="empty">
      <mat-icon>{{ icon }}</mat-icon>
      <div class="title">{{ title }}</div>
      <div class="subtitle" *ngIf="subtitle">{{ subtitle }}</div>
      <ng-content />
    </div>
  `,
  styles: [
    `
      .empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        text-align: center;
        padding: 48px 24px;
        color: var(--color-text-soft);
      }
      mat-icon {
        font-size: 44px;
        height: 44px;
        width: 44px;
        color: var(--color-border);
        margin-bottom: 12px;
      }
      .title {
        font-weight: 600;
        color: var(--color-text);
        font-size: 16px;
        margin-bottom: 4px;
      }
      .subtitle {
        font-size: 14px;
        max-width: 360px;
        margin-bottom: 16px;
      }
    `,
  ],
})
export class EmptyStateComponent {
  @Input() title = 'Sin datos';
  @Input() subtitle = '';
  @Input() icon = 'inbox';
}
