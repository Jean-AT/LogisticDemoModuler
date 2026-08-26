import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="page-header">
      <div>
        <h2 class="title">{{ title }}</h2>
        <p class="subtitle" *ngIf="subtitle">{{ subtitle }}</p>
      </div>
      <div class="actions">
        <ng-content select="[actions]" />
      </div>
    </div>
  `,
  styles: [
    `
      .page-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 16px;
        margin-bottom: 20px;
      }
      .title {
        margin: 0;
        font-size: 22px;
        font-weight: 700;
        color: var(--color-text);
      }
      .subtitle {
        margin: 4px 0 0;
        color: var(--color-text-soft);
        font-size: 14px;
      }
      .actions {
        display: flex;
        gap: 8px;
        flex-shrink: 0;
        flex-wrap: wrap;
      }
      @media (max-width: 640px) {
        .page-header { flex-direction: column; align-items: stretch; }
        .actions { width: 100%; flex-wrap: wrap; }
      }
    `,
  ],
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
}
