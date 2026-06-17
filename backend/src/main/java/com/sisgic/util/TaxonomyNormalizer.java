package com.sisgic.util;

import java.text.Normalizer;

/**
 * Accent-insensitive, case-insensitive term comparison for taxonomy labels and slugs.
 */
public final class TaxonomyNormalizer {

    private TaxonomyNormalizer() {}

    public static String normalizeTerm(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase();
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return withoutAccents.replaceAll("\\s+", " ");
    }

    public static boolean termsMatch(String a, String b) {
        return normalizeTerm(a).equals(normalizeTerm(b));
    }

    public static String slugify(String value) {
        String normalized = normalizeTerm(value);
        String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "category" : slug;
    }
}
