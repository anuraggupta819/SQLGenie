package com.anuraggupta.sqlgenie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FavoriteRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Natural language query is required")
        String naturalLanguageQuery,

        @NotBlank(message = "Generated SQL is required")
        String generatedSql
) {
}
