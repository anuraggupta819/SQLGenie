package com.anuraggupta.sqlgenie.controller;

import com.anuraggupta.sqlgenie.dto.request.NlQueryRequest;
import com.anuraggupta.sqlgenie.dto.response.QueryHistoryResponse;
import com.anuraggupta.sqlgenie.dto.response.QueryResultResponse;
import com.anuraggupta.sqlgenie.security.UserPrincipal;
import com.anuraggupta.sqlgenie.service.AssistantService;
import com.anuraggupta.sqlgenie.service.QueryHistoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queries")
@RequiredArgsConstructor
@Tag(name = "Queries", description = "Natural language to SQL and query history")
@SecurityRequirement(name = "bearerAuth")
public class QueryController {

    private final AssistantService assistantService;
    private final QueryHistoryService queryHistoryService;

    @PostMapping
    public ResponseEntity<QueryResultResponse> submitQuery(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NlQueryRequest request) {
        QueryResultResponse response = assistantService.handle(
                principal.getUser().getId(), request.question());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<QueryHistoryResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<QueryHistoryResponse> page = queryHistoryService
                .getHistory(principal.getUser().getId(), pageable)
                .map(QueryHistoryResponse::from);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        queryHistoryService.deleteHistory(principal.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
