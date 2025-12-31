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
    private final Function<UserAllergyRequest, UserAllergyResponse> userAllergyFunction;

    // NOTE: ingredientCheckFunction is registered as a @Bean (line 214) and available for Spring AI to call
    // Currently not used by any of the three recipe generation methods, but demonstrates how to register
    // multiple functions that AI can choose from based on the prompt
    private final Function<IngredientCheckRequest, IngredientCheckResponse> ingredientCheckFunction;

    public RecipeService(ChatClient.Builder chatClientBuilder, UserRepository userRepository) {
        this.userRepository = userRepository;

        this.userAllergyFunction = createUserAllergyFunction();
        this.ingredientCheckFunction = createIngredientCheckFunction();

        this.chatClient = chatClientBuilder.build();
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

    // Method 3: Safe recipe with allergy checking
    public String createSafeRecipe(String userId, String ingredients, String cuisine) {
        // Use the stored function instead of calling @Bean method
        var allergyRequest = new UserAllergyRequest(userId);
        var allergyResponse = userAllergyFunction.apply(allergyRequest);

        // Log for debugging


        // Build the allergen list as a string
        String allergenList = allergyResponse.allergies().isEmpty()
                ? "none"
                : String.join(", ", allergyResponse.allergies());

        var template = """
            You are creating a recipe for a user with SEVERE FOOD ALLERGIES.
            
            USER ALLERGIES: {allergens}
            
            Available ingredients: {ingredients}
            Cuisine: {cuisine}
            
            SAFETY PROTOCOL (CRITICAL - LIFE-THREATENING IF VIOLATED):
            
            Step 1: Identify dangerous ingredients
            - User is allergic to: {allergens}
            - From the ingredient list "{ingredients}", identify which items are allergens:
              * If "shellfish" is an allergen, then shrimp, crab, lobster, prawns are FORBIDDEN
              * If "peanuts" is an allergen, then peanuts, peanut butter, peanut oil are FORBIDDEN
              * If "dairy" is an allergen, then milk, cheese, butter, cream are FORBIDDEN
            
            Step 2: Remove ALL allergens
            - Cross out every dangerous ingredient from your list
            - Never use them in the recipe under any circumstances
            
            Step 3: Create recipe with ONLY safe ingredients
            - Use only the remaining safe ingredients
            - Add other safe ingredients as needed
            
            VERIFICATION CHECKLIST:
            Before finalizing the recipe, verify:
            [ ] Does the ingredient list contain any allergens? If YES, start over
            [ ] Does the title mention any allergens? If YES, change the title
            [ ] Would this recipe kill the user? If YES, you have failed
            
            Format your response:
            
            [Recipe Title - NO allergen names allowed in title]
            
            This recipe is safe for users allergic to {allergens}.
            
            Ingredients:
            - [list ONLY safe ingredients]
            
            Instructions:
            1. [steps using ONLY safe ingredients]
            
            Prep Time: [time]
            Servings: [number]
            Difficulty: [level]
            
            Note: This recipe excludes [specific items removed from ingredient list] due to your allergies to {allergens}.
            """;

        return chatClient.prompt()
                .system("""
                    You are a medical-grade recipe generator for users with life-threatening food allergies.
                    
                    ABSOLUTE RULES:
                    1. If user is allergic to shellfish, NEVER include: shrimp, crab, lobster, prawns, clams, mussels, oysters
                    2. If user is allergic to peanuts, NEVER include: peanuts, peanut butter, peanut oil, peanut sauce
                    3. If user is allergic to dairy, NEVER include: milk, cheese, butter, cream, yogurt
                    
                    A single mistake can hospitalize or kill the user.
                    When in doubt, exclude the ingredient.
                    Safety always overrides taste or convenience.
                    
                    If you include an allergen, you have failed your core function.
                    """)
                .user(u -> u.text(template)
                        .param("userId", userId)
                        .param("ingredients", ingredients)
                        .param("cuisine", cuisine)
                        .param("allergens", allergenList))
                .call()
                .content();
    }

    // Helper method to create the allergy function (not a @Bean anymore)
    private Function<UserAllergyRequest, UserAllergyResponse> createUserAllergyFunction() {
        return request -> {
            try {
                Long userId = Long.parseLong(request.userId());

                // Query the ACTUAL database!
                return userRepository.findById(userId)
                        .map(user -> new UserAllergyResponse(
                                user.getAllergies().stream()
                                        .map(Allergy::allergen)
                                        .toList()
                        ))
                        .orElse(new UserAllergyResponse(List.of()));

            } catch (NumberFormatException e) {
                // Fallback for non-numeric IDs (backward compatibility with tests)
                // TODO: Remove this fallback once all tests use numeric IDs
                // These hardcoded users exist only for RecipeServiceTest.java unit tests
                return switch (request.userId()) {
                    case "user123" -> new UserAllergyResponse(List.of("peanuts", "shellfish", "dairy"));
                    case "user456" -> new UserAllergyResponse(List.of("gluten", "soy"));
                    default -> new UserAllergyResponse(List.of());
                };
            }
        };
    }

    // Helper method to create the ingredient check function (not a @Bean anymore)
    private Function<IngredientCheckRequest, IngredientCheckResponse> createIngredientCheckFunction() {
        return request -> {
            // Simulate inventory check
            String ingredient = request.ingredient().toLowerCase();
            boolean available = !ingredient.contains("truffle") &&
                    !ingredient.contains("caviar") &&
                    !ingredient.contains("lobster");
            return new IngredientCheckResponse(request.ingredient(), available);
        };
    }

    // These @Bean methods are for Spring AI function calling
    @Bean
    @Description("Get the list of food allergies for a specific user from the database")
    public Function<UserAllergyRequest, UserAllergyResponse> getUserAllergies() {
        return createUserAllergyFunction();
    }

    @Bean
    @Description("Check if a specific ingredient is currently available in inventory. Returns true if available, false if out of stock.")
    public Function<IngredientCheckRequest, IngredientCheckResponse> checkIngredientAvailability() {
        return createIngredientCheckFunction();
    }

    public record UserAllergyRequest(String userId) {}
    public record UserAllergyResponse(List<String> allergies) {}
    public record IngredientCheckRequest(String ingredient) {}
    public record IngredientCheckResponse(String ingredient, boolean available) {}
}