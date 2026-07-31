package com.anuraggupta.sqlgenie.controller;

import com.anuraggupta.sqlgenie.dto.request.FavoriteRequest;
import com.anuraggupta.sqlgenie.dto.response.FavoriteResponse;
import com.anuraggupta.sqlgenie.security.UserPrincipal;
import com.anuraggupta.sqlgenie.service.FavoriteQueryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Saved favorite queries")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteQueryService favoriteQueryService;

    @PostMapping
    public ResponseEntity<FavoriteResponse> saveFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FavoriteRequest request) {
        FavoriteResponse response = favoriteQueryService.save(principal.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getFavorites(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(favoriteQueryService.getFavorites(principal.getUser().getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        favoriteQueryService.deleteFavorite(principal.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
