import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { estadoChip } from '../core/utils';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="chip" [style.color]="visual.color" [style.background]="visual.bg">
      {{ visual.label }}
    </span>
  `,
  styles: [
    `
      .chip {
        display: inline-block;
        padding: 3px 10px;
        border-radius: 999px;
        font-size: 12px;
        font-weight: 600;
        white-space: nowrap;
      }
    `,
  ],
})
export class StatusChipComponent {
  @Input() estado!: string;
  protected get visual() {
    return estadoChip(this.estado);
  }
}
