package io.hulsbo.dto;

import java.util.UUID;

/**
 * Data Transfer Object for returning ingredient search results.
 */
public class IngredientSearchResultDTO {

    public UUID id;
    public String name;

    // Default constructor (required by Jackson/Panache projection)
    public IngredientSearchResultDTO() {
    }

    // Constructor for Panache projection
    public IngredientSearchResultDTO(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
} 