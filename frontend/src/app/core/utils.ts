import { Moneda } from './models';

const monedaSymbol: Record<Moneda, string> = { PEN: 'S/', USD: '$' };

export function formatMoney(value: number | null | undefined, moneda: Moneda = 'PEN'): string {
  const n = value ?? 0;
  return `${monedaSymbol[moneda]} ${n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatAmount(value: number | null | undefined): string {
  return (value ?? 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDateOnly(value: string | null | undefined): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export interface EstadoVisual {
  label: string;
  color: string;
  bg: string;
}

export const estadoVisual: Record<string, EstadoVisual> = {
  BORRADOR: { label: 'Borrador', color: '#5D4E80', bg: '#EDE9F6' },
  ENVIADO: { label: 'Enviado', color: '#1D64C2', bg: '#DBEAFB' },
  APROBADO: { label: 'Aprobado', color: '#15803D', bg: '#D8F3E5' },
  OBSERVADO: { label: 'Observado', color: '#B45309', bg: '#FCEFD9' },
  RECHAZADO: { label: 'Rechazado', color: '#B4233A', bg: '#FBE1E4' },
  CONVERTIDO_OC: { label: 'Convertido a OC', color: '#4726A8', bg: '#EEE8FF' },
};

export const accionLabel: Record<string, string> = {
  APROBAR: 'Aprobó',
  OBSERVAR: 'Observó',
  RECHAZAR: 'Rechazó',
};

export function estadoChip(estado: string): EstadoVisual {
  return estadoVisual[estado] ?? { label: estado, color: '#6A6280', bg: '#EEEAF5' };
}

export function errorMessage(err: unknown): string {
  const anyErr = err as { error?: { message?: string; detail?: string }; message?: string };
  return anyErr?.error?.message || anyErr?.message || 'Ocurrió un error inesperado.';
}
