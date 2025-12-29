package com.ai.SpringAiDemo.Model;

import java.util.List;

public record Recipe(
        String title,
        List<String> ingredients,
        List<String> instructions,
        String prepTime,
        int servings,
        String difficulty
) {}