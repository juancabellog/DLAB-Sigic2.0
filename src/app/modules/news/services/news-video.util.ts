const UNSAFE_PROTOCOLS = ['javascript:', 'data:', 'vbscript:'];

const ALLOWED_EMBED_HOSTS = new Set([
  'www.youtube.com',
  'youtube.com',
  'player.vimeo.com',
  'www.facebook.com',
  'facebook.com'
]);

export const NEWS_VIDEO_MAX_UPLOAD_BYTES = 100 * 1024 * 1024; // matches backend multipart limit
export const NEWS_VIDEO_UPLOAD_MIME_TYPES = ['video/mp4', 'video/webm'];
export const NEWS_VIDEO_UNSUPPORTED_URL_ERROR =
  'This video URL is not supported. Please use YouTube, Vimeo or Facebook.';

export interface VideoEmbedNormalizeResult {
  embedUrl?: string;
  originalUrl?: string;
  error?: string;
}

function ensureHttpsUrl(input: string): string {
  const trimmed = input.trim();
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  return `https://${trimmed.replace(/^\/+/, '')}`;
}

function isSafeHttpUrl(url: string): boolean {
  const lower = url.trim().toLowerCase();
  for (const protocol of UNSAFE_PROTOCOLS) {
    if (lower.startsWith(protocol)) {
      return false;
    }
  }
  return lower.startsWith('http://') || lower.startsWith('https://');
}

function isAllowedEmbedUrl(embedUrl: string): boolean {
  try {
    const host = new URL(embedUrl).hostname.toLowerCase();
    return ALLOWED_EMBED_HOSTS.has(host);
  } catch {
    return false;
  }
}

function extractYouTubeId(url: URL): string | null {
  const host = url.hostname.replace(/^www\./, '');
  if (host === 'youtu.be') {
    const id = url.pathname.split('/').filter(Boolean)[0];
    return id && /^[a-zA-Z0-9_-]{11}$/.test(id) ? id : null;
  }
  if (host === 'youtube.com' || host === 'm.youtube.com') {
    if (url.pathname.startsWith('/embed/')) {
      const id = url.pathname.split('/')[2];
      return id && /^[a-zA-Z0-9_-]{11}$/.test(id) ? id : null;
    }
    if (url.pathname.startsWith('/shorts/')) {
      const id = url.pathname.split('/')[2];
      return id && /^[a-zA-Z0-9_-]{11}$/.test(id) ? id : null;
    }
    const v = url.searchParams.get('v');
    return v && /^[a-zA-Z0-9_-]{11}$/.test(v) ? v : null;
  }
  return null;
}

function extractVimeoId(url: URL): string | null {
  const host = url.hostname.replace(/^www\./, '');
  if (host === 'player.vimeo.com') {
    const match = url.pathname.match(/\/video\/(\d+)/);
    return match?.[1] ?? null;
  }
  if (host === 'vimeo.com') {
    const match = url.pathname.match(/\/(\d+)/);
    return match?.[1] ?? null;
  }
  return null;
}

function isFacebookVideoUrl(url: URL): boolean {
  const host = url.hostname.replace(/^www\./, '');
  if (host === 'fb.watch') {
    return url.pathname.length > 1;
  }
  if (host === 'facebook.com') {
    return (
      /\/videos\//i.test(url.pathname) ||
      (url.pathname.includes('/watch') && url.searchParams.has('v'))
    );
  }
  return false;
}

/** Normalizes supported video page URLs into safe provider embed URLs. */
export function normalizeVideoEmbedUrl(input: string): VideoEmbedNormalizeResult {
  const trimmed = input.trim();
  if (!trimmed) {
    return { error: 'Video URL is required.' };
  }

  const pageUrl = ensureHttpsUrl(trimmed);
  if (!isSafeHttpUrl(pageUrl)) {
    return { error: NEWS_VIDEO_UNSUPPORTED_URL_ERROR };
  }

  let parsed: URL;
  try {
    parsed = new URL(pageUrl);
  } catch {
    return { error: NEWS_VIDEO_UNSUPPORTED_URL_ERROR };
  }

  const youtubeId = extractYouTubeId(parsed);
  if (youtubeId) {
    const embedUrl = `https://www.youtube.com/embed/${youtubeId}`;
    return { embedUrl, originalUrl: pageUrl };
  }

  const vimeoId = extractVimeoId(parsed);
  if (vimeoId) {
    const embedUrl = `https://player.vimeo.com/video/${vimeoId}`;
    return { embedUrl, originalUrl: pageUrl };
  }

  if (isFacebookVideoUrl(parsed)) {
    const embedUrl = `https://www.facebook.com/plugins/video.php?href=${encodeURIComponent(pageUrl)}&show_text=false`;
    return { embedUrl, originalUrl: pageUrl };
  }

  return { error: NEWS_VIDEO_UNSUPPORTED_URL_ERROR };
}

export function buildVideoEmbedHtml(embedUrl: string, originalUrl: string): string {
  if (!isAllowedEmbedUrl(embedUrl)) {
    throw new Error(NEWS_VIDEO_UNSUPPORTED_URL_ERROR);
  }
  const safeEmbed = embedUrl.replace(/"/g, '&quot;');
  const safeOriginal = originalUrl.replace(/"/g, '&quot;');
  return (
    `<div class="news-video-embed" data-video-url="${safeOriginal}" contenteditable="false">` +
    `<iframe src="${safeEmbed}" title="Embedded video" loading="lazy" ` +
    `allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" ` +
    `allowfullscreen></iframe></div>`
  );
}

export function buildUploadedVideoHtml(relativePath: string, mimeType: string): string {
  const safePath = relativePath.replace(/"/g, '&quot;');
  const safeType = mimeType.replace(/"/g, '&quot;');
  return (
    `<div class="news-video-file" data-video-src="${safePath}" contenteditable="false">` +
    `<video controls preload="metadata">` +
    `<source src="${safePath}" type="${safeType}" />` +
    `</video></div>`
  );
}

export function isNewsVideoBlock(element: Element | null): element is HTMLElement {
  return !!element?.classList.contains('news-video-embed') || !!element?.classList.contains('news-video-file');
}

export function getVideoBlockType(element: HTMLElement): 'embed' | 'file' | null {
  if (element.classList.contains('news-video-embed')) return 'embed';
  if (element.classList.contains('news-video-file')) return 'file';
  return null;
}
