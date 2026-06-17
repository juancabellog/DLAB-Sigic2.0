package com.sisgic.service;

import com.sisgic.entity.Noticia;
import com.sisgic.entity.RelatedNews;
import com.sisgic.repository.NoticiaRepository;
import com.sisgic.repository.RelatedNewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RelatedNewsService {

    public static final int MAX_RELATED_POSTS = 3;

    @Autowired
    private RelatedNewsRepository relatedNewsRepository;

    @Autowired
    private NoticiaRepository noticiaRepository;

    /**
     * Replaces all related posts for a news item in related_news.
     * An empty list clears existing relations.
     */
    public void syncRelatedPosts(Long noticiaId, List<Long> relatedIds) {
        if (noticiaId == null) {
            throw new IllegalArgumentException("News id is required to sync related posts");
        }
        if (relatedIds == null) {
            return;
        }

        List<Long> normalized = normalizeRelatedIds(noticiaId, relatedIds);
        relatedNewsRepository.deleteByIdNoticia(noticiaId);

        if (normalized.isEmpty()) {
            return;
        }

        List<RelatedNews> rows = normalized.stream()
            .map(refId -> new RelatedNews(noticiaId, refId))
            .collect(Collectors.toList());
        relatedNewsRepository.saveAll(rows);
    }

    public List<Long> getRelatedPostIds(Long noticiaId) {
        if (noticiaId == null) {
            return List.of();
        }
        return relatedNewsRepository.findRelatedIdsByIdNoticia(noticiaId);
    }

    public List<Noticia> getRelatedNoticias(Long noticiaId) {
        if (noticiaId == null) {
            return List.of();
        }
        return relatedNewsRepository.findByIdNoticiaWithRelated(noticiaId).stream()
            .map(RelatedNews::getRelatedNoticia)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Removes all rows where the news item appears as source or target.
     */
    public void deleteAllReferences(Long noticiaId) {
        if (noticiaId == null) {
            return;
        }
        relatedNewsRepository.deleteByIdNoticia(noticiaId);
        relatedNewsRepository.deleteByIdNoticiaRef(noticiaId);
    }

    private List<Long> normalizeRelatedIds(Long noticiaId, List<Long> relatedIds) {
        List<Long> distinct = relatedIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

        if (distinct.size() > MAX_RELATED_POSTS) {
            throw new IllegalArgumentException("A news item can have at most " + MAX_RELATED_POSTS + " related posts");
        }
        if (distinct.contains(noticiaId)) {
            throw new IllegalArgumentException("A news item cannot be related to itself");
        }

        if (distinct.isEmpty()) {
            return List.of();
        }

        List<Noticia> found = noticiaRepository.findAllById(distinct);
        Set<Long> foundIds = found.stream()
            .map(Noticia::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));

        if (foundIds.size() != distinct.size()) {
            throw new IllegalArgumentException("One or more related posts were not found");
        }

        return distinct;
    }
}
