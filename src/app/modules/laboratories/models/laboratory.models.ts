export const LAB_STATUS = {
  ACTIVE: 'active',
  INACTIVE: 'inactive'
} as const;

export type LabStatus = typeof LAB_STATUS[keyof typeof LAB_STATUS];

export const TRANSLATION_STATUS = {
  NO_TRANSLATION: 'no_translation',
  AUTO_GENERATED: 'auto_generated',
  MANUALLY_EDITED: 'manually_edited',
  REQUIRES_REVIEW: 'requires_review',
  VALIDATED: 'validated'
} as const;

export type TranslationStatus = typeof TRANSLATION_STATUS[keyof typeof TRANSLATION_STATUS];

export const MEMBERSHIP_TYPE = {
  DIRECTOR: 'director',
  LAB_MANAGER: 'lab_manager',
  MEMBER: 'member'
} as const;

export type MembershipType = typeof MEMBERSHIP_TYPE[keyof typeof MEMBERSHIP_TYPE];

export const MEMBERSHIP_STATUS = {
  ACTIVE: 'active',
  ENDED: 'ended'
} as const;

export type MembershipStatus = typeof MEMBERSHIP_STATUS[keyof typeof MEMBERSHIP_STATUS];

export interface Laboratory {
  id?: number;
  nameEs: string;
  nameEn: string;
  descriptionEs: string;
  descriptionEn: string;
  imageUrl: string;
  imageAltEs: string;
  imageAltEn: string;
  clusterId: number | null;
  clusterLabel?: string;
  directorId: number | null;
  directorName?: string;
  directorEmail?: string;
  directorOrcid?: string;
  directorMobilePhone?: string;
  directorIniciales?: string;
  directorProfileImageUrl?: string;
  directorResourceType?: string;
  directorResourceTypeLabel?: string;
  status: LabStatus;
  translationStatus: TranslationStatus;
  translationValidatedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
  slug?: string;
  metaTitle?: string;
  metaDescription?: string;
  ogTitle?: string;
  ogDescription?: string;
  ogImageUrl?: string;
  publicUrl?: string;
  activeMemberCount?: number;
  labManagerName?: string;
  memberships?: LaboratoryMembership[];
}

export interface LaboratoryMembership {
  id?: string;
  laboratoryId?: number;
  personId: number | null;
  personName?: string;
  personEmail?: string;
  orcid?: string;
  email?: string;
  mobilePhone?: string;
  personIniciales?: string;
  profileImageUrl?: string | null;
  membershipType: MembershipType;
  resourceType: string;
  resourceTypeLabel?: string;
  startDate: string;
  endDate: string | null;
  status: MembershipStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface LaboratoryListFilters {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  status?: LabStatus | 'all' | '';
  clusterId?: number | null;
  directorId?: number | null;
  search?: string;
  hasActiveMembers?: boolean | null;
}

export const LAB_STATUS_LABELS: Record<LabStatus, string> = {
  active: 'Active',
  inactive: 'Inactive'
};

export const TRANSLATION_STATUS_LABELS: Record<TranslationStatus, string> = {
  no_translation: 'No translation',
  auto_generated: 'Auto-generated',
  manually_edited: 'Manually edited',
  requires_review: 'Requires review',
  validated: 'Validated'
};

export const MEMBERSHIP_STATUS_LABELS: Record<MembershipStatus, string> = {
  active: 'Active',
  ended: 'Ended'
};

export const CLUSTER_OPTIONS: { id: number; label: string }[] = [
  { id: 1, label: 'Cluster I' },
  { id: 2, label: 'Cluster II' },
  { id: 3, label: 'Cluster III' },
  { id: 4, label: 'Cluster IV' },
  { id: 5, label: 'Cluster V' }
];

export function createEmptyLaboratory(): Laboratory {
  return {
    nameEs: '',
    nameEn: '',
    descriptionEs: '',
    descriptionEn: '',
    imageUrl: '',
    imageAltEs: '',
    imageAltEn: '',
    clusterId: null,
    directorId: null,
    status: LAB_STATUS.ACTIVE,
    translationStatus: TRANSLATION_STATUS.NO_TRANSLATION,
    slug: '',
    metaTitle: '',
    metaDescription: '',
    ogTitle: '',
    ogDescription: '',
    ogImageUrl: '',
    publicUrl: '',
    memberships: []
  };
}

export function hasRichTextContent(html: string | null | undefined): boolean {
  if (!html) return false;
  const text = html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
  return text.length > 0;
}

export function plainTextFromHtml(html: string): string {
  if (!html) return '';
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}

export function buildSpanishSnapshot(lab: Laboratory): string {
  return JSON.stringify({
    nameEs: lab.nameEs,
    descriptionEs: lab.descriptionEs
  });
}

export function isMembershipActive(m: LaboratoryMembership): boolean {
  return m.status === MEMBERSHIP_STATUS.ACTIVE && !m.endDate;
}

export function getPersonInitials(name?: string | null, iniciales?: string | null): string {
  if (iniciales?.trim()) {
    return iniciales.trim().toUpperCase().slice(0, 3);
  }
  if (!name?.trim()) {
    return '?';
  }
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function matchesPersonSearch(m: LaboratoryMembership, query: string): boolean {
  const q = query.trim().toLowerCase();
  if (!q) return true;
  return [
    m.personName,
    m.email,
    m.personEmail,
    m.orcid,
    m.mobilePhone
  ].some(v => (v || '').toLowerCase().includes(q));
}
