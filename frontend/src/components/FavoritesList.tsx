import type { FavoriteQuery } from '../types';

interface FavoritesListProps {
  favorites: FavoriteQuery[];
  onDelete: (id: string) => void;
}

export default function FavoritesList({ favorites, onDelete }: FavoritesListProps) {
  if (favorites.length === 0) {
    return <p className="text-sm text-gray-400">No favorites saved yet.</p>;
  }

  return (
    <ul className="space-y-2">
      {favorites.map((favorite) => (
        <li
          key={favorite.id}
          className="rounded-md border border-gray-200 dark:border-gray-800 p-3"
        >
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-gray-800 dark:text-gray-200">
                {favorite.name}
              </p>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {favorite.explanation}
              </p>
            </div>
            <button
              onClick={() => onDelete(favorite.id)}
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
