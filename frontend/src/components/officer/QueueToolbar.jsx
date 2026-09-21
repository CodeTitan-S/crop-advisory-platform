import { QUEUE_SORTS } from '../../utils/queue';

const SELECT_CLASS =
  'border rounded px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-green-500';

/**
 * Status filter plus sort control, shared by the officer advisory and disease queues.
 *
 * @param statuses the queue's statuses in workflow order
 * @param shown how many items the current filter leaves visible
 * @param total how many items the queue holds in total
 */
export default function QueueToolbar({
  statuses,
  status,
  onStatusChange,
  sort,
  onSortChange,
  shown,
  total,
}) {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 mb-4">
      <label className="flex items-center gap-2 text-sm text-gray-600">
        Status
        <select
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label="Filter by status"
          className={SELECT_CLASS}
        >
          <option value="ALL">All</option>
          {statuses.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-2 text-sm text-gray-600">
        Sort by
        <select
          value={sort}
          onChange={(e) => onSortChange(e.target.value)}
          aria-label="Sort queue"
          className={SELECT_CLASS}
        >
          {QUEUE_SORTS.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>

      <span className="text-sm text-gray-500">
        {shown} of {total} shown
      </span>
    </div>
  );
}
