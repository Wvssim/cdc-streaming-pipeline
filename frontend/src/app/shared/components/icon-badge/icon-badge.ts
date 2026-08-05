import { Component, input } from '@angular/core';

@Component({
  selector: 'app-icon-badge',
  imports: [],
  templateUrl: './icon-badge.html',
  styleUrl: './icon-badge.scss',
})
export class IconBadge {
  variant = input<'neutral' | 'dark'>('neutral');
}
