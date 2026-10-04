# Handwritten Explanations

Please find the photographed handwritten notes for each bug in this directory (`bug1.jpg`, `bug2.jpg`, etc.).

Below is the transcript of the handwritten notes covering the required four points for each bug:

---

### Bug 1: SQL Operator Precedence in Task Search Query
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskRepository.java`, lines 14–17 (Repository / Data Layer)
  - `db/queries/search_tasks.sql`, lines 10–13 (SQL Layer)
  - `db/oracle/task_search_package.sql`, lines 52–55, 66–69 (Oracle PL/SQL Reference)
- **How Discovered**:
  - Tested search queries against the API (e.g., `/api/tasks?q=api&status=OPEN` and `/api/tasks?q=api`). Observed that tasks marked `archived: true` (IDs 20 and 21) were returned, and task ID 2 with `status: IN_PROGRESS` was returned even when filtering specifically for `status=OPEN`.
- **Root Cause**:
  - In SQL standard precedence rules, `AND` binds tighter than `OR`. The clause:
    `WHERE archived = FALSE AND LOWER(title) LIKE :term OR LOWER(description) LIKE :term AND (:status IS NULL OR status = :status)`
    was evaluated as:
    `(archived = FALSE AND LOWER(title) LIKE :term) OR (LOWER(description) LIKE :term AND (:status IS NULL OR status = :status))`
    This caused any task whose title matched `:term` to be returned regardless of `:status`, and any task whose description matched `:term` to be returned even if `archived = TRUE`.
- **How Fixed & Why**:
  - Added explicit parentheses around the search conditions:
    `WHERE archived = FALSE AND (LOWER(title) LIKE :term OR LOWER(description) LIKE :term) AND (:status IS NULL OR status = :status)`
  - Applied the fix consistently to `TaskRepository.java`, `search_tasks.sql`, and `task_search_package.sql`. This is the cleanest, lowest-risk fix that correctly enforces both archiving and status filters without altering database schema.

---

### Bug 2: Artificial Sleep / Blocking Delay in Controller
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskController.java`, lines 36–42 (Backend / Controller Layer)
- **How Discovered**:
  - Inspected `TaskController.java` and observed `Thread.sleep(queryWeight)` where `queryWeight = Math.max(0, 10 - query.length()) * 100L`. Benchmarked API response times: empty query took >1,080 ms while 10-character queries took ~20 ms.
- **Root Cause**:
  - The controller artificially blocked Tomcat worker threads. Crucially, shorter queries slept longer than longer queries. When typing incrementally (e.g. "a" -> "ap" -> "api"), responses arrived in reverse order ("api" resolved first, then "ap", then "a"), causing stale search results to overwrite current results on the client.
- **How Fixed & Why**:
  - Removed `Thread.sleep(queryWeight)` entirely. This eliminates the server-side bottleneck, frees HTTP worker threads, and prevents out-of-order response latency anomalies.

---

### Bug 3: Unhandled Exception on Invalid Status (HTTP 500)
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskController.java`, lines 30–33 (Backend / Controller Layer)
- **How Discovered**:
  - Tested API request with invalid status `/api/tasks?status=INVALID`. Server crashed with HTTP 500 `IllegalArgumentException`.
- **Root Cause**:
  - `TaskStatus.valueOf(status.toUpperCase())` throws an unchecked `IllegalArgumentException` when the string does not match `OPEN`, `IN_PROGRESS`, or `DONE`. Uncaught exceptions in Spring controllers bubble up as 500 Internal Server Errors.
- **How Fixed & Why**:
  - Wrapped `TaskStatus.valueOf` in a `try-catch(IllegalArgumentException)` block and returned a structured `ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status))`. Returning HTTP 400 with a descriptive error is standard REST practice and prevents server failure.

---

### Bug 4: Unsanitized Pagination Parameters Causing IndexOutOfBoundsException (HTTP 500)
- **Location**:
  - `backend/src/main/java/com/internal/tasktracker/TaskController.java`, lines 50–54 (Backend / Controller Layer)
- **How Discovered**:
  - Tested `/api/tasks?page=0`. Server threw `IndexOutOfBoundsException: fromIndex = -10` and returned HTTP 500.
- **Root Cause**:
  - When `page < 1`, `(page - 1) * pageSize` yields a negative `start` index. If `start < allResults.size()`, `allResults.subList(start, end)` throws `IndexOutOfBoundsException`.
- **How Fixed & Why**:
  - Clamped pagination parameters using `Math.max(1, page)` and `Math.max(1, pageSize)`. This guarantees non-negative sublist indices and protects the API against invalid inputs without crashing.

---

### Bug 5: Frontend Race Conditions, Broken Error Recovery & Lack of Request Cancellation
- **Location**:
  - `frontend/src/hooks/useTasks.js`, lines 10–22 (Frontend / Hook Layer)
  - `frontend/src/api.js`, lines 3–20 (Frontend / API Client Layer)
- **How Discovered**:
  - Code audit of asynchronous state handling. When requests failed, `setLoading(false)` was never called in the `.catch()` block, leaving the UI permanently stuck on "Loading tasks...". Rapid filter changes also led to stale responses overriding newer responses.
- **Root Cause**:
  - Lack of `AbortController` cancellation allowed outdated in-flight fetch promises to resolve and overwrite newer state. In `useTasks.js`, missing `setLoading(false)` in error cases permanently wedged the loading spinner, and errors were never reset upon new requests.
- **How Fixed & Why**:
  - Added `AbortController` support to `api.js` and `useTasks.js`. Abort signals cancel prior requests on unmount or parameter changes. Ignored `AbortError`, cleared `error` on each new request, and moved `setLoading(false)` to a `finally` block checked against `signal.aborted`.

---

### Bug 6: Pagination Page Not Resetting on Query/Status Changes
- **Location**:
  - `frontend/src/App.jsx`, lines 24–25 (Frontend / Component Layer)
- **How Discovered**:
  - Navigated to page 3, then typed a search term ("api") that has only 8 items (1 page). The UI displayed "No tasks found" because `page` remained at 3.
- **Root Cause**:
  - `query` and `status` updates updated state but left `page` untouched, querying out-of-bounds pages on smaller result sets.
- **How Fixed & Why**:
  - Added a `useEffect` hook in `App.jsx` watching `[debouncedQuery, status]` that resets `page` to 1 whenever filters change.

---

### Bug 7: Rapid Input Server Hammering (Missing Debounce)
- **Location**:
  - `frontend/src/App.jsx` and new `frontend/src/hooks/useDebounce.js` (Frontend Layer)
- **How Discovered**:
  - Monitored network activity while typing in `SearchBar`. Every keystroke dispatched an immediate HTTP request.
- **Root Cause**:
  - Direct binding between search input value and query parameter in `useTasks`.
- **How Fixed & Why**:
  - Implemented a standard, lightweight `useDebounce` hook with a 300ms delay. The text input remains instantaneous and responsive for the user, while network calls and backend database load are drastically reduced.
