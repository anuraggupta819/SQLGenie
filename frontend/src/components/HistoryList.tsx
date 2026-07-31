import type { QueryHistoryEntry } from '../types';

interface HistoryListProps {
  entries: QueryHistoryEntry[];
  onDelete: (id: string) => void;
}

const STATUS_STYLES: Record<string, string> = {
  SUCCESS: 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-400',
  REJECTED: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-400',
  FAILED: 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-400',
};

export default function HistoryList({ entries, onDelete }: HistoryListProps) {
  if (entries.length === 0) {
    return <p className="text-sm text-gray-400">No queries yet.</p>;
  }

  return (
    <ul className="space-y-2">
      {entries.map((entry) => (
        <li
          key={entry.id}
          className="rounded-md border border-gray-200 dark:border-gray-800 p-3"
        >
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm text-gray-800 dark:text-gray-200">
                {entry.naturalLanguageQuery}
              </p>
              <div className="mt-1 flex items-center gap-2">
                <span
                  className={`rounded px-1.5 py-0.5 text-xs font-medium ${STATUS_STYLES[entry.status]}`}
                >
                  {entry.status}
                </span>
                <span className="text-xs text-gray-400">
                  {new Date(entry.createdAt).toLocaleString()}
                </span>
              </div>
            </div>
            <button
              onClick={() => onDelete(entry.id)}
              className="shrink-0 text-xs text-gray-400 hover:text-red-600 dark:hover:text-red-400"
            >
              Delete
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
