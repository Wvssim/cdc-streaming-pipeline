import { Component, input } from '@angular/core';

export type IconName =
  | 'grid'
  | 'file-text'
  | 'history'
  | 'mail'
  | 'shield'
  | 'link'
  | 'search'
  | 'upload-cloud'
  | 'alert-triangle'
  | 'more-horizontal'
  | 'download'
  | 'check-circle'
  | 'scan-text'
  | 'chevron-right';

@Component({
  selector: 'app-icon',
  imports: [],
  templateUrl: './icon.html',
  styleUrl: './icon.scss',
})
export class Icon {
  name = input.required<IconName>();
}
