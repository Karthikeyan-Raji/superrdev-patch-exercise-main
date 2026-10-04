# Notes

### Summary of Changes
- **SQL / Data Layer (`TaskRepository.java`, `search_tasks.sql`, `task_search_package.sql`)**: Fixed operator precedence between `AND` and `OR`. Parenthesized `(LOWER(title) LIKE :term OR LOWER(description) LIKE :term)` so archived tasks are never leaked and status filters are strictly enforced.
- **Backend (`TaskController.java`)**: Removed artificial `Thread.sleep` delay that caused reverse-order response races; safely handled invalid status parameters with HTTP 400 Bad Request; sanitized pagination bounds (`page >= 1`, `pageSize >= 1`) to eliminate 500 `IndexOutOfBoundsException`.
- **Frontend (`useTasks.js`, `api.js`, `App.jsx`, `useDebounce.js`)**: Added `AbortController` cancellation to eliminate out-of-order stale data overwrites; fixed loading/error lifecycle so UI doesn't hang; reset pagination to page 1 on filter/query changes; added 300ms search input debouncing to prevent server hammering.

### What Was Not Changed & Why
- **In-Memory Pagination**: Kept `subList` pagination in Java rather than rewriting repository methods to `Pageable` or SQL `LIMIT/OFFSET`. Given the in-memory H2 dataset, this avoided unnecessary architecture churn while maintaining a small, focused diff.
- **UI Architecture / Design**: Kept existing CSS and layout intact as requested by the exercise instructions ("a small, high-quality diff beats a large rewrite").
- **Oracle Artifact Structure**: Corrected the PL/SQL query logic to match the fix, but left package definitions intact since it is an offline reference artifact.

### Biggest Remaining Risk
- **Unbounded In-Memory Query Loading**: In production, `taskRepository.searchTasks` pulls every matching task row into JVM memory before slicing with `subList`. Under large datasets, this will cause high memory pressure, GC pauses, and potential OutOfMemoryErrors. It should be migrated to Spring Data `Pageable` with database-level pagination.

### Tools & AI Used
- Used AI to analyze cross-layer inconsistencies, locate the SQL operator precedence flaw across repository and PL/SQL artifacts, scaffold the `useDebounce` hook, and pinpoint edge cases (negative pagination indices, unhandled enum parsing).
