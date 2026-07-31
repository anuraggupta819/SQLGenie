import { useState, type FormEvent } from 'react';

interface QueryInputProps {
  onSubmit: (question: string) => void | Promise<void>;
  loading: boolean;
}

export default function QueryInput({ onSubmit, loading }: QueryInputProps) {
  const [question, setQuestion] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!question.trim() || loading) return;
    await onSubmit(question.trim());
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <input
        type="text"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="Ask a question about the data, e.g. 'Show me the top 5 customers by order count'"
        className="flex-1 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
      />
      <button
        type="submit"
        disabled={loading || !question.trim()}
        className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        {loading ? 'Asking…' : 'Ask'}
      </button>
    </form>
  );
}
