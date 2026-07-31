import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import QueryInput from '../components/QueryInput';
import ResultsTable from '../components/ResultsTable';
import HistoryList from '../components/HistoryList';
import FavoritesList from '../components/FavoritesList';
import { submitQuery, getHistory, deleteHistoryEntry } from '../api/queries';
import { getFavorites, deleteFavorite } from '../api/favorites';
import { getErrorMessage } from '../api/errors';
import type { FavoriteQuery, QueryHistoryEntry, QueryResultResponse } from '../types';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [question, setQuestion] = useState('');
  const [result, setResult] = useState<QueryResultResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<QueryHistoryEntry[]>([]);
  const [favorites, setFavorites] = useState<FavoriteQuery[]>([]);

  const loadHistory = useCallback(async () => {
    try {
      const page = await getHistory();
      setHistory(page.content);
    } catch {
      // History failing to load shouldn't block the rest of the dashboard.
    }
  }, []);

  const loadFavorites = useCallback(async () => {
    try {
      setFavorites(await getFavorites());
    } catch {
      // Same as history - non-fatal for the rest of the page.
    }
  }, []);

  useEffect(() => {
    loadHistory();
    loadFavorites();
  }, [loadHistory, loadFavorites]);

  async function handleAsk(newQuestion: string) {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const response = await submitQuery(newQuestion);
      setQuestion(newQuestion);
      setResult(response);
      loadHistory();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteHistory(id: string) {
    await deleteHistoryEntry(id);
    setHistory((prev) => prev.filter((h) => h.id !== id));
  }

  async function handleDeleteFavorite(id: string) {
    await deleteFavorite(id);
    setFavorites((prev) => prev.filter((f) => f.id !== id));
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <header className="border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <h1 className="text-lg font-semibold text-gray-900 dark:text-gray-100">SQLGenie</h1>
          <div className="flex items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
            <span>{user?.email}</span>
            <button onClick={logout} className="hover:text-gray-900 dark:hover:text-gray-100">
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="space-y-4 lg:col-span-2">
            <QueryInput onSubmit={handleAsk} loading={loading} />

            {error && (
              <div className="rounded-md border border-red-300 dark:border-red-800 bg-red-50 dark:bg-red-950 p-4">
                <p className="text-sm text-red-700 dark:text-red-400">{error}</p>
              </div>
            )}

            {result && (
              <ResultsTable question={question} result={result} onFavoriteSaved={loadFavorites} />
            )}
          </div>

          <div className="space-y-6">
            <section>
              <h2 className="mb-2 text-sm font-semibold text-gray-700 dark:text-gray-300">
                Favorites
              </h2>
              <FavoritesList favorites={favorites} onDelete={handleDeleteFavorite} />
            </section>

            <section>
              <h2 className="mb-2 text-sm font-semibold text-gray-700 dark:text-gray-300">
                History
              </h2>
              <HistoryList entries={history} onDelete={handleDeleteHistory} />
            </section>
          </div>
        </div>
      </main>
    </div>
  );
}
