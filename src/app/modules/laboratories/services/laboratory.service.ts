import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { BaseHttpService } from '../../../core/services/base-http.service';
import { environment } from '../../../../environments/environment';
import {
  Laboratory,
  LaboratoryListFilters,
  LaboratoryMembership,
  MembershipType,
  TRANSLATION_STATUS,
  TranslationStatus
} from '../models/laboratory.models';

export interface LaboratoryListResponse {
  content: Laboratory[];
  totalElements: number;
}

interface PaginatedLaboratoryApi {
  content: Laboratory[];
  totalElements: number;
}

export type LaboratoryTranslationDirection = 'es_to_en' | 'en_to_es';

export interface TranslateLaboratoryResponse {
  nameEn?: string;
  descriptionEn?: string;
  nameEs?: string;
  descriptionEs?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LaboratoryService {
  constructor(private baseHttp: BaseHttpService) {}

  getLaboratories(filters: LaboratoryListFilters = {}): Observable<LaboratoryListResponse> {
    const params: Record<string, string | number | boolean> = {
      page: filters.page ?? 0,
      size: filters.size ?? 10000,
      sortBy: filters.sort ?? 'nameEs',
      sortDir: (filters.direction ?? 'ASC').toLowerCase()
    };
    if (filters.status && filters.status !== 'all') {
      params['status'] = filters.status;
    } else if (filters.status === 'all') {
      params['status'] = 'all';
    }
    if (filters.clusterId) params['clusterId'] = filters.clusterId;
    if (filters.directorId) params['directorId'] = filters.directorId;
    if (filters.search) params['search'] = filters.search;
    if (filters.hasActiveMembers != null) params['hasActiveMembers'] = filters.hasActiveMembers;

    return this.baseHttp.get<PaginatedLaboratoryApi>('/laboratories', params).pipe(
      map(res => ({
        content: res.content || [],
        totalElements: res.totalElements ?? 0
      })),
      catchError(err => throwError(() => err))
    );
  }

  getLaboratoryById(id: number, clusterId?: number | null): Observable<Laboratory> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.get<Laboratory>(`/laboratories/${id}`, params);
  }

  createLaboratory(payload: Laboratory): Observable<Laboratory> {
    return this.baseHttp.post<Laboratory>('/laboratories', payload);
  }

  updateLaboratory(id: number, payload: Laboratory): Observable<Laboratory> {
    const params = payload.clusterId != null ? { clusterId: payload.clusterId } : undefined;
    return this.baseHttp.put<Laboratory>(`/laboratories/${id}`, payload, params);
  }

  activateLaboratory(id: number, clusterId?: number | null): Observable<Laboratory> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.patch<Laboratory>(`/laboratories/${id}/activate`, {}, params);
  }

  deactivateLaboratory(id: number, clusterId?: number | null): Observable<Laboratory> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.patch<Laboratory>(`/laboratories/${id}/deactivate`, {}, params);
  }

  deleteLaboratory(id: number, clusterId?: number | null): Observable<void> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.delete<void>(`/laboratories/${id}`, params);
  }

  generateLaboratoryTranslation(
    direction: LaboratoryTranslationDirection,
    source: { name: string; description?: string | null }
  ): Observable<TranslateLaboratoryResponse> {
    if (direction === 'en_to_es') {
      return this.baseHttp.post<TranslateLaboratoryResponse>('/laboratories/translate', {
        direction,
        nameEn: source.name,
        descriptionEn: source.description ?? ''
      });
    }
    return this.baseHttp.post<TranslateLaboratoryResponse>('/laboratories/translate', {
      direction: 'es_to_en',
      nameEs: source.name,
      descriptionEs: source.description ?? ''
    });
  }

  validateLaboratoryTranslation(id: number, clusterId?: number | null): Observable<Laboratory> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.post<Laboratory>(`/laboratories/${id}/validate-translation`, {}, params);
  }

  getLaboratoryMembers(
    laboratoryId: number,
    clusterId?: number | null,
    membershipType?: MembershipType
  ): Observable<LaboratoryMembership[]> {
    const params: Record<string, string | number> = {};
    if (clusterId != null) params['clusterId'] = clusterId;
    if (membershipType) params['membershipType'] = membershipType;
    return this.baseHttp.get<LaboratoryMembership[]>(
      `/laboratories/${laboratoryId}/memberships`,
      Object.keys(params).length ? params : undefined
    );
  }

  addLaboratoryMember(
    laboratoryId: number,
    clusterId: number | null | undefined,
    payload: LaboratoryMembership
  ): Observable<LaboratoryMembership> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.post<LaboratoryMembership>(
      `/laboratories/${laboratoryId}/memberships`,
      payload,
      params
    );
  }

  updateLaboratoryMember(
    laboratoryId: number,
    clusterId: number | null | undefined,
    membershipKey: string,
    payload: LaboratoryMembership
  ): Observable<LaboratoryMembership> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.put<LaboratoryMembership>(
      `/laboratories/${laboratoryId}/memberships/${encodeURIComponent(membershipKey)}`,
      payload,
      params
    );
  }

  endLaboratoryMember(
    laboratoryId: number,
    clusterId: number | null | undefined,
    membershipKey: string,
    endDate: string
  ): Observable<LaboratoryMembership> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.patch<LaboratoryMembership>(
      `/laboratories/${laboratoryId}/memberships/${encodeURIComponent(membershipKey)}/end`,
      { endDate },
      params
    );
  }

  deleteMembershipPermanently(
    laboratoryId: number,
    clusterId: number | null | undefined,
    membershipKey: string
  ): Observable<void> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.delete<void>(
      `/laboratories/${laboratoryId}/memberships/${encodeURIComponent(membershipKey)}`,
      params
    );
  }

  restoreMembership(
    laboratoryId: number,
    clusterId: number | null | undefined,
    membershipKey: string
  ): Observable<LaboratoryMembership> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.patch<LaboratoryMembership>(
      `/laboratories/${laboratoryId}/memberships/${encodeURIComponent(membershipKey)}/restore`,
      {},
      params
    );
  }

  assignDirector(
    laboratoryId: number,
    clusterId: number | null | undefined,
    payload: {
      personId: number;
      email?: string;
      orcid?: string;
      mobilePhone?: string;
    }
  ): Observable<Laboratory> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.put<Laboratory>(`/laboratories/${laboratoryId}/director`, payload, params);
  }

  updateDirectorContact(
    laboratoryId: number,
    clusterId: number | null | undefined,
    payload: {
      email?: string;
      orcid?: string;
      mobilePhone?: string;
    }
  ): Observable<Laboratory> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.patch<Laboratory>(
      `/laboratories/${laboratoryId}/director/contact`,
      payload,
      params
    );
  }

  changeLabManager(
    laboratoryId: number,
    clusterId: number | null | undefined,
    payload: {
      personId: number;
      resourceType: string;
      startDate?: string;
      endDateForCurrent?: string;
      email?: string;
      orcid?: string;
      mobilePhone?: string;
    }
  ): Observable<LaboratoryMembership> {
    const params = clusterId != null ? { clusterId } : undefined;
    return this.baseHttp.post<LaboratoryMembership>(
      `/laboratories/${laboratoryId}/lab-manager/change`,
      payload,
      params
    );
  }

  buildMembershipPayload(
    result: {
      personId: number | null;
      resourceType: string;
      startDate: string;
      endDate: string | null;
      email: string;
      orcid: string;
      mobilePhone: string;
      profileImageUrl?: string | null;
    },
    membershipType: MembershipType,
    existing?: LaboratoryMembership
  ): LaboratoryMembership {
    return {
      id: existing?.id,
      personId: result.personId,
      membershipType,
      resourceType: result.resourceType as LaboratoryMembership['resourceType'],
      startDate: result.startDate,
      endDate: result.endDate,
      email: result.email,
      orcid: result.orcid,
      mobilePhone: result.mobilePhone,
      profileImageUrl: result.profileImageUrl ?? existing?.profileImageUrl,
      status: result.endDate ? 'ended' : 'active'
    };
  }

  uploadImage(file: File): Observable<string> {
    return this.baseHttp.uploadFile<{ image: string }>(
      '/files/upload-media',
      file,
      'file',
      { subdir: 'images' }
    ).pipe(
      map(res => res.image)
    );
  }

  resolveMediaUrl(path: string | null | undefined): string | null {
    if (!path) return null;
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    const base = environment.apiUrl.replace(/\/api\/?$/, '');
    const normalized = path.replace(/^\//, '');
    if (normalized.startsWith('media/')) {
      return `${base}/${normalized}`;
    }
    return `${base}/${normalized}`;
  }

  applyTranslationResult(
    lab: Laboratory,
    result: TranslateLaboratoryResponse,
    direction: LaboratoryTranslationDirection = 'es_to_en'
  ): Laboratory {
    if (direction === 'en_to_es') {
      return {
        ...lab,
        nameEs: result.nameEs || lab.nameEs,
        descriptionEs: result.descriptionEs || lab.descriptionEs,
        translationStatus: TRANSLATION_STATUS.AUTO_GENERATED as TranslationStatus
      };
    }
    return {
      ...lab,
      nameEn: result.nameEn || lab.nameEn,
      descriptionEn: result.descriptionEn || lab.descriptionEn,
      translationStatus: TRANSLATION_STATUS.AUTO_GENERATED as TranslationStatus
    };
  }
}
