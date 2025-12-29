package com.ai.SpringAiDemo.service;

import com.ai.SpringAiDemo.Model.Recipe;
import com.ai.SpringAiDemo.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestConfig.class)  // Use mocked ChatClient
class RecipeServiceTest {

    @Autowired
    private RecipeService recipeService;

    // ========== BUSINESS LOGIC TESTS (Free, Fast) ==========

    @Test
    void testGetUserAllergies_User123_ReturnsPeanutShellfishDairy() {
        // Given
        var request = new RecipeService.UserAllergyRequest("user123");

        // When
        var response = recipeService.getUserAllergies().apply(request);

        // Then
        assertNotNull(response);
        assertEquals(3, response.allergies().size());
        assertTrue(response.allergies().contains("peanuts"));
        assertTrue(response.allergies().contains("shellfish"));
        assertTrue(response.allergies().contains("dairy"));
    }

    @Test
    void testGetUserAllergies_User456_ReturnsGlutenSoy() {
        // Given
        var request = new RecipeService.UserAllergyRequest("user456");

        // When
        var response = recipeService.getUserAllergies().apply(request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.allergies().size());
        assertTrue(response.allergies().contains("gluten"));
        assertTrue(response.allergies().contains("soy"));
    }

    @Test
    void testGetUserAllergies_UnknownUser_ReturnsEmptyList() {
        // Given
        var request = new RecipeService.UserAllergyRequest("unknown");

        // When
        var response = recipeService.getUserAllergies().apply(request);

        // Then
        assertNotNull(response);
        assertTrue(response.allergies().isEmpty());
    }

    @Test
    void testCheckIngredientAvailability_Truffle_NotAvailable() {
        // Given
        var request = new RecipeService.IngredientCheckRequest("truffle");

        // When
        var response = recipeService.checkIngredientAvailability().apply(request);

        // Then
        assertNotNull(response);
        assertEquals("truffle", response.ingredient());
        assertFalse(response.available(), "Luxury ingredients should be unavailable");
    }

    @Test
    void testCheckIngredientAvailability_Caviar_NotAvailable() {
        // Given
        var request = new RecipeService.IngredientCheckRequest("caviar");

        // When
        var response = recipeService.checkIngredientAvailability().apply(request);

        // Then
        assertFalse(response.available());
    }

    @Test
    void testCheckIngredientAvailability_Lobster_NotAvailable() {
        // Given
        var request = new RecipeService.IngredientCheckRequest("lobster");

        // When
        var response = recipeService.checkIngredientAvailability().apply(request);

        // Then
        assertFalse(response.available());
    }

    @Test
    void testCheckIngredientAvailability_CommonIngredient_Available() {
        // Given
        var request = new RecipeService.IngredientCheckRequest("chicken");

        // When
        var response = recipeService.checkIngredientAvailability().apply(request);

        // Then
        assertTrue(response.available(), "Common ingredients should be available");
    }

    @Test
    void testCheckIngredientAvailability_CaseInsensitive() {
        // Given - test with different cases
        var upperCase = new RecipeService.IngredientCheckRequest("TRUFFLE");
        var mixedCase = new RecipeService.IngredientCheckRequest("TrUfFlE");

        // When
        var upperResponse = recipeService.checkIngredientAvailability().apply(upperCase);
        var mixedResponse = recipeService.checkIngredientAvailability().apply(mixedCase);

        // Then - all should be unavailable regardless of case
        assertFalse(upperResponse.available());
        assertFalse(mixedResponse.available());
    }
    @Test
    void testRecipeContract_AllFieldsRequired() {
        // Given valid data
        Recipe recipe = new Recipe(
                "Pasta Carbonara",
                List.of("pasta", "eggs", "bacon"),
                List.of("Cook pasta", "Mix eggs", "Combine"),
                "20 minutes",
                4,
                "medium"
        );

        // Then all fields should be accessible
        assertNotNull(recipe.title(), "Title must not be null");
        assertNotNull(recipe.ingredients(), "Ingredients must not be null");
        assertNotNull(recipe.instructions(), "Instructions must not be null");
        assertNotNull(recipe.prepTime(), "Prep time must not be null");
        assertTrue(recipe.servings() > 0, "Servings must be positive");
        assertNotNull(recipe.difficulty(), "Difficulty must not be null");
    }

    @Test
    void testRecipeContract_IngredientsNotEmpty() {
        Recipe recipe = new Recipe(
                "Pasta Carbonara",
                List.of("pasta", "eggs", "bacon"),
                List.of("Cook pasta", "Mix eggs"),
                "20 minutes",
                4,
                "medium"
        );

        assertFalse(recipe.ingredients().isEmpty(),
                "Recipe must have at least one ingredient");
        assertTrue(recipe.ingredients().size() >= 1,
                "Recipe should contain ingredients");
    }

    @Test
    void testRecipeContract_InstructionsNotEmpty() {
        Recipe recipe = new Recipe(
                "Simple Pasta",
                List.of("pasta"),
                List.of("Boil water", "Cook pasta", "Serve"),
                "15 minutes",
                2,
                "easy"
        );

        assertFalse(recipe.instructions().isEmpty(),
                "Recipe must have instructions");
        assertTrue(recipe.instructions().size() >= 1,
                "Recipe should contain cooking steps");
    }

    @Test
    void testRecipeContract_DifficultyLevelsValid() {
        List<String> validDifficulties = List.of("easy", "medium", "hard");

        Recipe easyRecipe = new Recipe("Toast", List.of("bread"),
                List.of("Toast bread"), "2 minutes", 1, "easy");
        Recipe mediumRecipe = new Recipe("Pasta", List.of("pasta"),
                List.of("Cook pasta"), "20 minutes", 2, "medium");
        Recipe hardRecipe = new Recipe("Soufflé", List.of("eggs"),
                List.of("Make soufflé"), "60 minutes", 4, "hard");

        assertTrue(validDifficulties.contains(easyRecipe.difficulty()),
                "Easy difficulty should be valid");
        assertTrue(validDifficulties.contains(mediumRecipe.difficulty()),
                "Medium difficulty should be valid");
        assertTrue(validDifficulties.contains(hardRecipe.difficulty()),
                "Hard difficulty should be valid");
    }

    @Test
    void testRecipeContract_ServingsPositive() {
        Recipe recipe = new Recipe(
                "Single Serving Pasta",
                List.of("pasta"),
                List.of("Cook"),
                "10 minutes",
                1,
                "easy"
        );

        assertTrue(recipe.servings() > 0,
                "Servings must be a positive number");
    }

    // ========== INTEGRATION TESTS (Manual Only) ==========

    // These are commented out but show what COULD be tested with real API
    // Uncomment temporarily to validate actual AI behavior before releases

    /*
    @Test
    @Tag("integration")
    void integrationTest_CreateRecipe_WithRealAI() {
        // WARNING: This makes a real API call and costs money!
        // Only run manually before major releases

        String recipe = recipeService.createRecipe(
            "chicken, rice",
            "Asian",
            "none"
        );

        assertNotNull(recipe);
        assertTrue(recipe.length() > 100, "Recipe should be detailed");
        assertTrue(recipe.toLowerCase().contains("chicken"));
    }

    @Test
    @Tag("integration")
    void integrationTest_CreateStructuredRecipe_ValidatesRealJSON() {
        // WARNING: This makes a real API call!

        Recipe recipe = recipeService.createStructuredRecipe(
            "pasta, tomatoes",
            "Italian",
            "vegetarian"
        );

        assertNotNull(recipe.title());
        assertNotNull(recipe.ingredients());
        assertFalse(recipe.ingredients().isEmpty());
        assertTrue(recipe.servings() > 0);
        assertNotNull(recipe.difficulty());
    }
    */
}