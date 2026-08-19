import { NewsCategory, TRANSLATION_STATUS, TranslationStatus } from '../../news/models/news.models';
import {
  AgendaEditorialMetadata,
  AgendaEvent,
  EVENT_MODE,
  EventMode,
  PUBLICATION_STATUS,
  PublicationStatus,
  buildSpanishSnapshot
} from '../models/agenda.models';

export interface LocalizedTextApi {
  us?: string;
  es?: string;
}

export interface AgendaApiDTO {
  id?: number;
  title?: LocalizedTextApi;
  description?: LocalizedTextApi;
  estado?: { id?: number; code?: string; label?: string };
  image?: string;
  eventDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  eventMode?: string;
  onlineUrl?: string;
  feature?: string;
  categories?: { id: string; label?: string; slug?: string }[];
  username?: string;
  createdAt?: string;
  updatedAt?: string;
  basal?: string;
}

export interface PaginatedAgendaApi {
  content: AgendaApiDTO[];
  totalElements: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

const ESTADO_TO_STATUS: Record<number, PublicationStatus> = {
  1: PUBLICATION_STATUS.PUBLISHED,
  2: PUBLICATION_STATUS.DRAFT,
  3: PUBLICATION_STATUS.UNPUBLISHED,
  4: PUBLICATION_STATUS.READY_TO_PUBLISH
};

const STATUS_TO_ESTADO: Record<PublicationStatus, number> = {
  [PUBLICATION_STATUS.PUBLISHED]: 1,
  [PUBLICATION_STATUS.DRAFT]: 2,
  [PUBLICATION_STATUS.UNPUBLISHED]: 3,
  [PUBLICATION_STATUS.READY_TO_PUBLISH]: 4
};

export function mapEstadoToPublicationStatus(estado?: { id?: number; code?: string }): PublicationStatus {
  if (estado?.id && ESTADO_TO_STATUS[estado.id]) {
    return ESTADO_TO_STATUS[estado.id];
  }
  const code = (estado?.code || '').toUpperCase();
  if (code === 'PUBLISHED') return PUBLICATION_STATUS.PUBLISHED;
  if (code === 'UNPUBLISHED') return PUBLICATION_STATUS.UNPUBLISHED;
  if (code === 'READY_TO_PUBLISH') return PUBLICATION_STATUS.READY_TO_PUBLISH;
  return PUBLICATION_STATUS.DRAFT;
}

export function mapPublicationStatusToEstadoId(status: PublicationStatus): number {
  return STATUS_TO_ESTADO[status] ?? 2;
}

function mapEventMode(value?: string | null, fallback?: EventMode | ''): EventMode | '' {
  if (!value?.trim()) return fallback ?? '';
  const normalized = value.trim().toLowerCase();
  if (normalized === EVENT_MODE.IN_PERSON) return EVENT_MODE.IN_PERSON;
  if (normalized === EVENT_MODE.ONLINE) return EVENT_MODE.ONLINE;
  if (normalized === EVENT_MODE.HYBRID) return EVENT_MODE.HYBRID;
  return fallback ?? '';
}

function mapFeatureToBoolean(feature?: string | null): boolean | undefined {
  if (feature == null || feature === '') return undefined;
  const v = feature.trim().toUpperCase();
  if (v === 'S' || v === 'Y' || v === '1') return true;
  if (v === 'N' || v === '0') return false;
  return undefined;
}

export function mergeApiWithMetadata(dto: AgendaApiDTO, meta: AgendaEditorialMetadata): AgendaEvent {
  const categories: NewsCategory[] = (dto.categories || []).map(c => ({
    id: c.id,
    label: c.label || c.id,
    slug: c.slug
  }));

  const hasEn = Boolean(
    (dto.title?.us?.trim()) || (dto.description?.us?.trim())
  );

  let translationStatus = meta.translationStatus;
  if (!hasEn && translationStatus !== TRANSLATION_STATUS.NO_TRANSLATION) {
    // keep stored status
  } else if (!hasEn) {
    translationStatus = TRANSLATION_STATUS.NO_TRANSLATION;
  }

  return {
    id: dto.id,
    titleEs: dto.title?.es || '',
    titleEn: dto.title?.us || '',
    descriptionEs: dto.description?.es || '',
    descriptionEn: dto.description?.us || '',
    mainImageUrl: dto.image || '',
    mainImageAltEs: meta.mainImageAltEs,
    mainImageAltEn: meta.mainImageAltEn,
    categories,
    author: dto.username || '',
    publicationStatus: mapEstadoToPublicationStatus(dto.estado),
    translationStatus,
    eventDate: dto.eventDate || null,
    startTime: dto.startTime || '',
    endTime: dto.endTime || meta.endTime || '',
    location: dto.location || '',
    eventMode: mapEventMode(dto.eventMode, meta.eventMode),
    onlineUrl: dto.onlineUrl || meta.onlineUrl || '',
    slug: meta.slug,
    metaTitle: meta.metaTitle,
    metaDescription: meta.metaDescription,
    ogTitle: meta.ogTitle,
    ogDescription: meta.ogDescription,
    ogImageUrl: meta.ogImageUrl || dto.image || '',
    publicUrl: meta.publicUrl,
    featured: mapFeatureToBoolean(dto.feature) ?? meta.featured ?? false,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
    publishedAt: dto.eventDate || null,
    translationValidatedAt: meta.translationValidatedAt,
    basal: dto.basal || 'N'
  };
}

export function mapAgendaEventToApiPayload(item: AgendaEvent): AgendaApiDTO {
  return {
    title: { es: item.titleEs, us: item.titleEn },
    description: { es: item.descriptionEs, us: item.descriptionEn },
    image: item.mainImageUrl,
    eventDate: item.eventDate || undefined,
    startTime: item.startTime || undefined,
    endTime: item.endTime || undefined,
    location: item.location || undefined,
    eventMode: item.eventMode || undefined,
    onlineUrl: item.onlineUrl || undefined,
    estado: {
      id: mapPublicationStatusToEstadoId(item.publicationStatus ?? PUBLICATION_STATUS.DRAFT)
    },
    categories: (item.categories || []).map(c => ({ id: c.id, label: c.label, slug: c.slug })),
    feature: item.featured ? 'S' : 'N',
    basal: (item.basal?.trim() || 'N')
  };
}

export function extractMetadataFromAgendaEvent(item: AgendaEvent): AgendaEditorialMetadata {
  return {
    translationStatus: item.translationStatus,
    mainImageAltEs: item.mainImageAltEs,
    mainImageAltEn: item.mainImageAltEn,
    slug: item.slug,
    metaTitle: item.metaTitle,
    metaDescription: item.metaDescription,
    ogTitle: item.ogTitle,
    ogDescription: item.ogDescription,
    ogImageUrl: item.ogImageUrl,
    publicUrl: item.publicUrl,
    translationValidatedAt: item.translationValidatedAt || null,
    validatedSpanishSnapshot:
      item.translationStatus === TRANSLATION_STATUS.VALIDATED || item.translationValidatedAt
        ? buildSpanishSnapshot(item)
        : undefined,
    eventMode: item.eventMode || undefined,
    onlineUrl: item.onlineUrl,
    endTime: item.endTime
  };
}
