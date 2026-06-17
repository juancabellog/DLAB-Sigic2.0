import { LinkType } from './link-editor.models';

const UNSAFE_PROTOCOLS = ['javascript:', 'data:', 'vbscript:'];

export function parseHrefToLinkForm(href: string): { linkType: LinkType; value: string } {
  const raw = (href || '').trim();
  if (!raw) {
    return { linkType: 'web', value: '' };
  }
  if (raw.toLowerCase().startsWith('mailto:')) {
    return { linkType: 'email', value: raw.slice(7) };
  }
  if (raw.toLowerCase().startsWith('tel:')) {
    return { linkType: 'phone', value: raw.slice(4) };
  }
  if (raw.startsWith('#')) {
    return { linkType: 'anchor', value: raw };
  }
  return { linkType: 'web', value: raw };
}

export function parseRelAttribute(rel: string | null | undefined): {
  noreferrer: boolean;
  nofollow: boolean;
  sponsored: boolean;
} {
  const parts = (rel || '').toLowerCase().split(/\s+/).filter(Boolean);
  return {
    noreferrer: parts.includes('noreferrer'),
    nofollow: parts.includes('nofollow'),
    sponsored: parts.includes('sponsored')
  };
}

export function buildRelAttribute(opts: {
  noreferrer: boolean;
  nofollow: boolean;
  sponsored: boolean;
}): string | undefined {
  const parts: string[] = [];
  if (opts.noreferrer) parts.push('noreferrer');
  if (opts.nofollow) parts.push('nofollow');
  if (opts.sponsored) parts.push('sponsored');
  return parts.length ? parts.join(' ') : undefined;
}

export function isSafeHref(href: string): boolean {
  const trimmed = href.trim();
  const lower = trimmed.toLowerCase();

  for (const protocol of UNSAFE_PROTOCOLS) {
    if (lower.startsWith(protocol)) {
      return false;
    }
  }

  if (
    lower.startsWith('http:') ||
    lower.startsWith('https:') ||
    lower.startsWith('mailto:') ||
    lower.startsWith('tel:') ||
    lower.startsWith('#') ||
    lower.startsWith('/') ||
    lower.startsWith('media/')
  ) {
    return true;
  }

  // Relative paths without explicit protocol
  if (!/^[a-z][a-z0-9+.-]*:/i.test(trimmed)) {
    return true;
  }

  return false;
}

export function normalizeAndBuildHref(
  linkType: LinkType,
  value: string
): { href?: string; error?: string } {
  const trimmed = value.trim();
  if (!trimmed) {
    return { error: 'This field is required.' };
  }

  if (linkType === 'email') {
    const email = trimmed.replace(/^mailto:/i, '');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return { error: 'Enter a valid email address.' };
    }
    const href = `mailto:${email}`;
    return isSafeHref(href) ? { href } : { error: 'This link protocol is not allowed.' };
  }

  if (linkType === 'phone') {
    const raw = trimmed.replace(/^tel:/i, '');
    const digits = raw.replace(/[\s().-]/g, '');
    if (!/^\+?[0-9]{6,15}$/.test(digits)) {
      return { error: 'Enter a valid phone number.' };
    }
    const href = `tel:${digits}`;
    return isSafeHref(href) ? { href } : { error: 'This link protocol is not allowed.' };
  }

  if (linkType === 'anchor') {
    const anchor = trimmed.startsWith('#') ? trimmed : `#${trimmed}`;
    if (!/^#[\w-]+$/.test(anchor)) {
      return { error: 'Anchor must start with # and contain only letters, numbers, or hyphens.' };
    }
    return { href: anchor };
  }

  let url = trimmed;
  if (!/^https?:\/\//i.test(url) && !url.startsWith('/') && !url.startsWith('#') && !url.startsWith('media/')) {
    url = `https://${url}`;
  }

  if (!isSafeHref(url)) {
    return { error: 'This link protocol is not allowed.' };
  }

  if (/^https?:\/\//i.test(url)) {
    try {
      new URL(url);
    } catch {
      return { error: 'Enter a valid URL.' };
    }
  }

  return { href: url };
}

export function defaultTogglesForLinkType(linkType: LinkType): {
  openInNewTab: boolean;
  noreferrer: boolean;
} {
  if (linkType === 'web') {
    return { openInNewTab: true, noreferrer: true };
  }
  return { openInNewTab: false, noreferrer: false };
}

export function supportsNewTab(linkType: LinkType): boolean {
  return linkType === 'web';
}
