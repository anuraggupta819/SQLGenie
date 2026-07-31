import { useState } from 'react';
import type { QueryResultResponse } from '../types';
import { saveFavorite } from '../api/favorites';
import { getErrorMessage } from '../api/errors';

interface ResultsTableProps {
  question: string;
  result: QueryResultResponse;
  onFavoriteSaved: () => void;
}

export default function ResultsTable({ question, result, onFavoriteSaved }: ResultsTableProps) {
  const [favoriteName, setFavoriteName] = useState('');
  const [savingFavorite, setSavingFavorite] = useState(false);
  const [favoriteError, setFavoriteError] = useState<string | null>(null);
  const [favoriteSaved, setFavoriteSaved] = useState(false);

  async function handleSaveFavorite() {
    if (!favoriteName.trim() || !result.sql) return;
    setSavingFavorite(true);
    setFavoriteError(null);
    try {
      await saveFavorite(favoriteName.trim(), question, result.sql);
      setFavoriteSaved(true);
      onFavoriteSaved();
    } catch (err) {
      setFavoriteError(getErrorMessage(err));
    } finally {
      setSavingFavorite(false);
    }
  }

  if (result.status !== 'SUCCESS') {
    return (
      <div className="rounded-md border border-amber-300 dark:border-amber-800 bg-amber-50 dark:bg-amber-950 p-4">
        <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
          {result.status === 'REJECTED' ? "Couldn't answer that" : 'Query failed'}
        </p>
        <p className="mt-1 text-sm text-amber-700 dark:text-amber-400">{result.errorMessage}</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {result.explanation && (
        <p className="text-sm text-gray-600 dark:text-gray-400">{result.explanation}</p>
      )}

      {result.sql && (
        <pre className="overflow-x-auto rounded-md bg-gray-900 p-3 text-xs text-gray-100">
          <code>{result.sql}</code>
        </pre>
      )}

      {result.columns && result.rows && (
        <div className="overflow-x-auto rounded-md border border-gray-200 dark:border-gray-800">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-800 text-sm">
            <thead className="bg-gray-50 dark:bg-gray-900">
              <tr>
                {result.columns.map((col) => (
                  <th
                    key={col}
                    className="px-3 py-2 text-left font-medium text-gray-500 dark:text-gray-400"
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
              {result.rows.map((row, i) => (
                <tr key={i}>
                  {result.columns!.map((col) => (
                    <td key={col} className="px-3 py-2 text-gray-700 dark:text-gray-300">
                      {String(row[col] ?? '')}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
          {result.rows.length === 0 && (
            <p className="p-3 text-sm text-gray-400">No rows returned.</p>
          )}
        </div>
      )}

      <p className="text-xs text-gray-400">
        {result.rows?.length ?? 0} row(s) in {result.executionTimeMs}ms
      </p>

      {!favoriteSaved ? (
        <div className="flex gap-2">
          <input
            type="text"
            value={favoriteName}
            onChange={(e) => setFavoriteName(e.target.value)}
            placeholder="Name this favorite"
            className="flex-1 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-1.5 text-sm text-gray-900 dark:text-gray-100 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
          <button
            onClick={handleSaveFavorite}
            disabled={savingFavorite || !favoriteName.trim()}
            className="rounded-md border border-gray-300 dark:border-gray-700 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 disabled:opacity-50"
          >
            {savingFavorite ? 'Saving…' : 'Save as favorite'}
          </button>
        </div>
      ) : (
        <p className="text-sm text-green-600 dark:text-green-400">Saved to favorites.</p>
      )}
      {favoriteError && <p className="text-sm text-red-600 dark:text-red-400">{favoriteError}</p>}
    </div>
  );
}
