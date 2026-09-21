/** Sorting and filtering shared by the officer advisory and disease queues. */

/** Sort choices offered by QueueToolbar; 'status' follows the queue's workflow order. */
export const QUEUE_SORTS = [
  { value: 'age-desc', label: 'Newest first' },
  { value: 'age-asc', label: 'Oldest first' },
  { value: 'status', label: 'Status' },
];

/** Epoch millis for an item's createdAt, or null when it is missing or unparseable. */
function createdTime(item) {
  const time = new Date(item?.createdAt ?? '').getTime();
  return Number.isNaN(time) ? null : time;
}

/** Orders by age, keeping undated items last in either direction. */
function byAge(a, b, newestFirst) {
  const left = createdTime(a);
  const right = createdTime(b);
  if (left === null || right === null) {
    if (left === right) return 0;
    return left === null ? 1 : -1;
  }
  return newestFirst ? right - left : left - right;
}

/** An unknown status ranks after every known one instead of jumping to the top. */
function statusRank(status, statusOrder) {
  const index = statusOrder.indexOf(status);
  return index === -1 ? statusOrder.length : index;
}

/**
 * Applies the queue's status filter and sort choice.
 *
 * @param items the queue as returned by the API (already newest-first)
 * @param controls `{ status, sort }` from the queue's state; status 'ALL' keeps every status
 * @param statusOrder the queue's workflow order, used as the sort key when sort === 'status'
 */
export function applyQueueControls(items, { status = 'ALL', sort = 'age-desc' }, statusOrder = []) {
  const filtered = status === 'ALL' ? items : items.filter((item) => item.status === status);

  return [...filtered].sort((a, b) => {
    if (sort === 'status') {
      // Inside one status the oldest item is the most overdue, so it leads.
      return (
        statusRank(a.status, statusOrder) - statusRank(b.status, statusOrder) ||
        byAge(a, b, false)
      );
    }
    return byAge(a, b, sort === 'age-desc');
  });
}

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/** How long ago an item was created, as a short label ("45m", "3h", "6d"). */
export function formatAge(createdAt) {
  const time = new Date(createdAt ?? '').getTime();
  if (Number.isNaN(time)) return 'unknown';

  const elapsed = Date.now() - time;
  if (elapsed < MINUTE) return 'just now';
  if (elapsed < HOUR) return `${Math.floor(elapsed / MINUTE)}m`;
  if (elapsed < DAY) return `${Math.floor(elapsed / HOUR)}h`;

  const days = Math.floor(elapsed / DAY);
  return days < 30 ? `${days}d` : `${Math.floor(days / 30)}mo`;
}
