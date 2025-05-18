package io.hulsbo.util.model;

public enum MeasurementUnit {
    // Name(standardGrams, isVolume, description)
    PCS(null, false, "grams per piece (custom)"),
    GRAM(1.0, false, "grams"),
    KILOGRAM(1000.0, false, "kilograms"),
    TEASPOON(5.0, true, "5ml"),
    TABLESPOON(15.0, true, "15ml"),
    CUP(240.0, true, "240ml"),
    MILLILITER(1.0, true, "milliliter"),
    LITER(1000.0, true, "liter");

    private final Double standardGrams;
    private final boolean isVolume;
    private final String description;

    MeasurementUnit(Double standardGrams, boolean isVolume, String description) {
        this.standardGrams = standardGrams;
        this.isVolume = isVolume;
        this.description = description;
    }

    public Double getStandardGrams() {
        return standardGrams;
    }

    public boolean isVolume() {
        return isVolume;
    }

    public String getDescription() {
        return description;
    }
}