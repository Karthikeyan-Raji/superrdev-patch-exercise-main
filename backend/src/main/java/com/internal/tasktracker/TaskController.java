package com.internal.tasktracker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // Normalize query input
        String query = q == null ? "" : q.trim();
        String searchTerm = "%" + query.toLowerCase() + "%";

        // Parse status filter
        String normalizedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }

        // Query complexity estimation for logging
        int complexityScore = Math.max(0, 10 - query.length());

        System.out.println("[TaskController] q=\"" + query + "\" status=" + normalizedStatus
                + " page=" + page + " pageSize=" + pageSize
                + " complexity=" + complexityScore);

        List<Task> allResults = taskRepository.searchTasks(searchTerm, normalizedStatus);

        int sanitizedPage = Math.max(1, page);
        int sanitizedPageSize = Math.max(1, pageSize);
        int start = (sanitizedPage - 1) * sanitizedPageSize;
        int end = Math.min(start + sanitizedPageSize, allResults.size());
        List<Task> pageResults = (start < allResults.size())
                ? allResults.subList(start, end)
                : Collections.emptyList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", pageResults);
        response.put("total", allResults.size());
        response.put("page", sanitizedPage);
        response.put("pageSize", sanitizedPageSize);

        return ResponseEntity.ok(response);
    }
}
