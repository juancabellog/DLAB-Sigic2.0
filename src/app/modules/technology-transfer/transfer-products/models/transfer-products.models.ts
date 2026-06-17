export interface TransferProduct {
  id?: string;
  name: string;
  description: string;
  segments: string[];
}

export interface SegmentOption {
  id: string;
  label: string;
  children?: SegmentOption[];
  exclusive?: boolean;
}

export const SEGMENT_NONE_ID = 'NONE_OF_THE_ABOVE' as const;

export const SEGMENT_OPTIONS: SegmentOption[] = [
  {
    id: 'PRIVATE_SECTOR',
    label: 'Private Sector',
    children: [
      { id: 'PRIVATE_ACADEMY', label: 'Academy' },
      { id: 'PRIVATE_BUSINESS', label: 'Business' },
      { id: 'PRIVATE_OWN_ENTREPRENEURSHIP', label: 'Own entrepreneurship' }
    ]
  },
  {
    id: 'PUBLIC_SECTOR',
    label: 'Public Sector',
    children: [
      { id: 'PUBLIC_GOVERNMENT', label: 'Government' },
      { id: 'PUBLIC_ACADEMY', label: 'Academy' }
    ]
  },
  { id: 'SOCIAL_ONG', label: 'Social - ONG' },
  { id: 'IN_THE_CENTER', label: 'In the Center' },
  { id: 'NONE_OF_THE_ABOVE', label: 'None of the above', exclusive: true }
];

const LEAF_LABELS = new Map<string, string>();

function collectLeafLabels(nodes: SegmentOption[]): void {
  for (const node of nodes) {
    if (node.children?.length) {
      collectLeafLabels(node.children);
    } else {
      LEAF_LABELS.set(node.id, node.label);
    }
  }
}

collectLeafLabels(SEGMENT_OPTIONS);

/** Count of selected leaf segments (same as `ids.length` for normalized selections). */
export function segmentSelectionCount(ids: readonly string[]): number {
  return ids.length;
}

/** e.g. `0 segment(s) selected`, `2 segment(s) selected` */
export function formatSegmentSelectionSummary(ids: readonly string[]): string {
  const n = segmentSelectionCount(ids);
  return `${n} segment(s) selected`;
}

/** Human-readable labels for current selection (only known leaf ids). */
export function selectedSegmentLabels(ids: readonly string[]): string[] {
  return ids.map(id => LEAF_LABELS.get(id) ?? id);
}
