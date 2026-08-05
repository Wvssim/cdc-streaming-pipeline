import { Component, computed, input } from '@angular/core';

export type StatusTone = 'success' | 'warning' | 'danger' | 'neutral';

@Component({
  selector: 'app-status-pill',
  imports: [],
  templateUrl: './status-pill.html',
  styleUrl: './status-pill.scss',
})
export class StatusPill {
  status = input.required<StatusTone>();
  label = input.required<string>();

  dotClass = computed(() => `status-pill__dot--${this.status()}`);
}
