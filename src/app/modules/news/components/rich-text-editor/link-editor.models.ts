export type LinkType = 'web' | 'email' | 'phone' | 'anchor';

export interface LinkEditorDialogData {
  linkType: LinkType;
  value: string;
  openInNewTab: boolean;
  noreferrer: boolean;
  nofollow: boolean;
  sponsored: boolean;
  isEditing: boolean;
  selectedText: string;
}

export interface LinkEditorDialogResult {
  action: 'save' | 'cancel' | 'remove';
  href?: string;
  target?: string;
  rel?: string;
}

export const LINK_TYPE_OPTIONS: { value: LinkType; label: string }[] = [
  { value: 'web', label: 'Web address' },
  { value: 'email', label: 'Email' },
  { value: 'phone', label: 'Phone' },
  { value: 'anchor', label: 'Anchor on page' }
];
