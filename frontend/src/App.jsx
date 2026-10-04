import { useState, useEffect } from 'react';
import SearchBar from './components/SearchBar';
import StatusFilter from './components/StatusFilter';
import TaskTable from './components/TaskTable';
import { useTasks } from './hooks/useTasks';
import { useDebounce } from './hooks/useDebounce';

export default function App() {
  const [searchInput, setSearchInput] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);

  const debouncedQuery = useDebounce(searchInput, 300);

  // Reset to page 1 when search query or status filter changes
  useEffect(() => {
    setPage(1);
  }, [debouncedQuery, status]);

  const { tasks, total, loading, error } = useTasks(debouncedQuery, status, page, 10);

  const totalPages = Math.ceil(total / 10);

  return (
    <div className="app">
      <header className="app-header">
        <h1>Task Tracker</h1>
        <p className="subtitle">Internal task management</p>
      </header>

      <div className="controls">
        <SearchBar value={searchInput} onChange={setSearchInput} />
        <StatusFilter value={status} onChange={setStatus} />
      </div>

      <TaskTable tasks={tasks} loading={loading} error={error} />

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span>
            Page {page} of {totalPages}
          </span>
          <button disabled={page >= totalPages} onClick={() => setPage((p) => p + 1)}>
            Next
          </button>
        </div>
      )}
    </div>
  );
}
