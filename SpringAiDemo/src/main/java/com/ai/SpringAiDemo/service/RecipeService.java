package com.ai.SpringAiDemo.service;

import com.ai.SpringAiDemo.Model.Recipe;
import com.ai.SpringAiDemo.domain.Allergy;
import com.ai.SpringAiDemo.persistence.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class RecipeService {
    private final ChatClient chatClient;
    private final UserRepository userRepository;

    public RecipeService(ChatClient.Builder chatClientBuilder, UserRepository userRepository) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
    }

    // Method 1: Basic text recipe
    public String createRecipe(String ingredients,
                               String cuisine,
                               String dietaryRestrictions) {
        var template = """
                I want to create a recipe using the following requirements ingredients: {ingredients}.
                The cuisine type I prefer is {cuisine}. 
                Please consider the following dietary restrictions: {dietaryRestrictions}.
                Please provide me with a detailed recipe including title, list of ingredients, and cooking instructions 
                """;

        return chatClient.prompt()
                .user(u -> u.text(template)
                        .param("ingredients", ingredients)
                        .param("cuisine", cuisine)
                        .param("dietaryRestrictions", dietaryRestrictions))
                .call()
                .content();
    }

    // Method 2: Structured recipe (returns Recipe object)
    public Recipe createStructuredRecipe(String ingredients,
                                         String cuisine,
                                         String dietaryRestrictions) {
        var template = """
                Create a recipe using these requirements:
                - Ingredients: {ingredients}
                - Cuisine: {cuisine}
                - Dietary restrictions: {dietaryRestrictions}
                
                Respond with ONLY valid JSON in this exact format (no markdown, no explanation):
                {{
                  "title": "recipe name here",
                  "ingredients": ["ingredient 1 with measurements", "ingredient 2"],
                  "instructions": ["step 1", "step 2", "step 3"],
                  "prepTime": "X minutes",
                  "servings": 4,
                  "difficulty": "easy"
                }}
                """;

        return chatClient.prompt()
                .user(u -> u.text(template)
                        .param("ingredients", ingredients)
                        .param("cuisine", cuisine)
                        .param("dietaryRestrictions", dietaryRestrictions))
                .call()
                .entity(Recipe.class);
    }

    // Method 3: Safe recipe with function calling
    // The @Bean functions below will be automatically available to ChatClient
    public String createSafeRecipe(String userId,
                                   String ingredients,
                                   String cuisine) {
        var template = """
                Create a {cuisine} recipe for user {userId} using these ingredients: {ingredients}.
                
                IMPORTANT: 
                1. First, call the getUserAllergies function to check what user {userId} is allergic to
                2. Then, call checkIngredientAvailability to verify each ingredient is in stock
                3. Only use ingredients that are both safe and available
                4. Create a complete recipe avoiding all allergens
                5. Explain which ingredients were excluded and why
                """;

        return chatClient.prompt()
                .user(u -> u.text(template)
                        .param("userId", userId)
                        .param("ingredients", ingredients)
                        .param("cuisine", cuisine))
                .call()
                .content();
    }


    @Bean
    @Description("Get the list of food allergies for a specific user from the database")
    public Function<UserAllergyRequest, UserAllergyResponse> getUserAllergies() {
        return request -> {
            try {
                Long userId = Long.parseLong(request.userId());

                // Query the ACTUAL database!
                return userRepository.findById(userId)
                        .map(user -> new UserAllergyResponse(
                                user.getAllergies().stream()
                                        .map(Allergy::Allergen)
                                        .toList()
                        ))
                        .orElse(new UserAllergyResponse(List.of()));

            } catch (NumberFormatException e) {
                // Fallback for non-numeric IDs (backward compatibility)
                return switch (request.userId()) {
                    case "user123" -> new UserAllergyResponse(List.of("peanuts", "shellfish", "dairy"));
                    case "user456" -> new UserAllergyResponse(List.of("gluten", "soy"));
                    default -> new UserAllergyResponse(List.of());
                };
            }
        };
    }

    @Bean
    @Description("Check if a specific ingredient is currently available in inventory. Returns true if available, false if out of stock.")
    public Function<IngredientCheckRequest, IngredientCheckResponse> checkIngredientAvailability() {
        return request -> {
            // Simulate inventory check
            String ingredient = request.ingredient().toLowerCase();
            boolean available = !ingredient.contains("truffle") &&
                    !ingredient.contains("caviar") &&
                    !ingredient.contains("lobster");
            return new IngredientCheckResponse(request.ingredient(), available);
        };
    }

    // Records for function calling
    public record UserAllergyRequest(String userId) {}
    public record UserAllergyResponse(List<String> allergies) {}
    public record IngredientCheckRequest(String ingredient) {}
    public record IngredientCheckResponse(String ingredient, boolean available) {}
}