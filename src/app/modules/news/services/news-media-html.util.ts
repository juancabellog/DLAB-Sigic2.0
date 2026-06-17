/** Relative paths stored in DB/HTML, e.g. media/IMAGEN/abc.jpg */
const MEDIA_PREFIX = 'media/';

/**
 * Converts absolute or API media URLs back to a relative path (media/...).
 */
export function toRelativeMediaPath(src: string | null | undefined, apiBase?: string): string | null {
  if (!src?.trim()) return null;
  let value = src.trim();

  if (value.startsWith(MEDIA_PREFIX)) {
    return value;
  }

  if (apiBase) {
    const publicBase = apiBase.replace(/\/api\/?$/, '');
    const marker = `${publicBase}/media/`;
    if (value.startsWith(marker)) {
      return MEDIA_PREFIX + value.slice(marker.length);
    }
  }

  const idx = value.indexOf('/media/');
  if (idx >= 0) {
    return MEDIA_PREFIX + value.slice(idx + '/media/'.length);
  }

  return null;
}

/**
 * Normalizes img/video src in HTML to relative media/ paths for persistence.
 */
export function normalizeMediaPathsInHtml(html: string, apiBase?: string): string {
  if (!html?.trim() || typeof DOMParser === 'undefined') {
    return html || '';
  }

  const doc = new DOMParser().parseFromString(html, 'text/html');
  doc.querySelectorAll('img[src], video[src], source[src]').forEach(el => {
    const src = el.getAttribute('src');
    const relative = toRelativeMediaPath(src, apiBase);
    if (relative) {
      el.setAttribute('src', relative);
    }
  });
  return doc.body.innerHTML;
}

/**
 * Resolves relative media/ paths to public URLs for display in editor/preview.
 */
export function resolveMediaPathsInHtml(
  html: string,
  resolve: (relativePath: string) => string | null
): string {
  if (!html?.trim() || typeof DOMParser === 'undefined') {
    return html || '';
  }

  const doc = new DOMParser().parseFromString(html, 'text/html');
  doc.querySelectorAll('img[src], video[src], source[src]').forEach(el => {
    const src = el.getAttribute('src');
    if (!src) return;
    const relative = toRelativeMediaPath(src) || (src.startsWith(MEDIA_PREFIX) ? src : null);
    if (relative) {
      const resolved = resolve(relative);
      if (resolved) {
        el.setAttribute('src', resolved);
      }
    }
  });
  return doc.body.innerHTML;
}

export function isVideoPath(path: string): boolean {
  return /\.(mp4|webm|ogg|mov)(\?|$)/i.test(path);
}
