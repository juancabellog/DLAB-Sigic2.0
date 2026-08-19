import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { getPersonInitials } from '../../models/laboratory.models';

@Component({
  selector: 'app-person-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="person-avatar" [class.person-avatar--lg]="size === 'lg'" [class.person-avatar--sm]="size === 'sm'">
      <img *ngIf="imageUrl; else initialsTpl" [src]="imageUrl" [alt]="name || 'Person'" />
      <ng-template #initialsTpl>
        <span class="person-avatar__initials">{{ initials }}</span>
      </ng-template>
    </div>
  `,
  styles: [`
    .person-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      flex-shrink: 0;
      background: #e0e0e0;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #d5d5d5;
    }
    .person-avatar--lg {
      width: 56px;
      height: 56px;
    }
    .person-avatar--sm {
      width: 32px;
      height: 32px;
    }
    .person-avatar img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .person-avatar__initials {
      font-size: 13px;
      font-weight: 600;
      color: #555;
      letter-spacing: 0.5px;
    }
    .person-avatar--lg .person-avatar__initials {
      font-size: 16px;
    }
  `]
})
export class PersonAvatarComponent {
  @Input() name = '';
  @Input() iniciales = '';
  @Input() imageUrl: string | null = null;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  get initials(): string {
    return getPersonInitials(this.name, this.iniciales);
  }
}
