package io.hulsbo.util.model;

public enum MeasurementUnit {
    // Name(standardGrams, isVolume, description)
    PCS(null, false, "grams per piece (custom)"),
    GRAM(1.0, false, "Metric weight (1g)"),
    KILOGRAM(1000.0, false, "Metric weight (1000g)"),
    TEASPOON(5.0, true, "US volume (5ml)"),
    TABLESPOON(15.0, true, "US volume (15ml)"),
    CUP(240.0, true, "US volume (240ml)"),
    MILLILITER(1.0, true, "Metric volume (1ml)"),
    LITER(1000.0, true, "Metric volume (1000ml)");

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