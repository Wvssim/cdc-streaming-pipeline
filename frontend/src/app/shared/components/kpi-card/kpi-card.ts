import { Component, input } from '@angular/core';
import { IconBadge } from '../icon-badge/icon-badge';

@Component({
  selector: 'app-kpi-card',
  imports: [IconBadge],
  templateUrl: './kpi-card.html',
  styleUrl: './kpi-card.scss',
})
export class KpiCard {
  value = input.required<string | number>();
  label = input.required<string>();
  variant = input<'neutral' | 'dark'>('neutral');
}
