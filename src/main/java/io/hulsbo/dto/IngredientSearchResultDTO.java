package io.hulsbo.dto;

import java.util.UUID;

/**
 * Data Transfer Object for returning ingredient search results.
 */
public class IngredientSearchResultDTO {

    public UUID id;
    public String name;
    public UUID created_by_user_id;

    // Default constructor (required by Jackson/Panache projection)
    public IngredientSearchResultDTO() {
    }

    // Constructor for Panache projection
    public IngredientSearchResultDTO(UUID id, String name, UUID created_by_user_id) {
        this.id = id;
        this.name = name;
        this.created_by_user_id = created_by_user_id;
    }
} 