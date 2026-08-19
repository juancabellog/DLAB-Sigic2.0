import { TipoRRHHDTO } from '../../../core/models/backend-dtos';

export function getTipoRrhhLabel(
  types: TipoRRHHDTO[],
  id: string | number | null | undefined,
  fallbackLabel?: string | null
): string {
  if (fallbackLabel?.trim()) {
    return fallbackLabel.trim();
  }
  if (id == null || id === '') {
    return '—';
  }
  const numericId = typeof id === 'string' ? Number(id) : id;
  if (!Number.isFinite(numericId)) {
    return String(id);
  }
  const tipo = types.find(t => t.id === numericId);
  if (!tipo) {
    return String(id);
  }
  return (tipo.descripcion || tipo.idDescripcion || String(numericId)).trim();
}

export function tipoRrhhIdFromValue(value: number | string | null | undefined): string {
  if (value == null || value === '') {
    return '';
  }
  return String(value);
}

export function sortTipoRrhhTypes(types: TipoRRHHDTO[]): TipoRRHHDTO[] {
  return [...types].sort((a, b) =>
    (a.descripcion || a.idDescripcion || '').localeCompare(
      b.descripcion || b.idDescripcion || '',
      undefined,
      { sensitivity: 'base' }
    )
  );
}

export function findTipoRrhhByDescription(
  types: TipoRRHHDTO[],
  ...needles: string[]
): TipoRRHHDTO | undefined {
  const normalizedNeedles = needles.map(n => n.toLowerCase());
  return types.find(tipo => {
    const label = (tipo.descripcion || tipo.idDescripcion || '').toLowerCase();
    return normalizedNeedles.some(needle => label.includes(needle));
  });
}
