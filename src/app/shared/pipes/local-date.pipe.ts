import { Pipe, PipeTransform } from '@angular/core';
import { formatLocalDate, LocalDateFormat } from '../../core/utils/date.util';

@Pipe({
  name: 'localDate',
  standalone: true
})
export class LocalDatePipe implements PipeTransform {
  transform(value: string | null | undefined, format: LocalDateFormat = 'MMM dd, yyyy'): string {
    return formatLocalDate(value, format);
  }
}
