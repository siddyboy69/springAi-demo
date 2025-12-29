package com.ai.SpringAiDemo.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public record Allergy(@NotNull String Allergen) {
}
