package com.ai.SpringAiDemo.controller;

import com.ai.SpringAiDemo.Model.Recipe;
import com.ai.SpringAiDemo.domain.Allergy;
import com.ai.SpringAiDemo.domain.User;
import com.ai.SpringAiDemo.persistence.UserRepository;
import com.ai.SpringAiDemo.service.RecipeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {
    private final RecipeService recipeService;
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/generate")
    public ResponseEntity<String> generateRecipe(
            @RequestParam String ingredients,
            @RequestParam(defaultValue = "any") String cuisine,
            @RequestParam(defaultValue = "none") String dietary) {

        String recipe = recipeService.createRecipe(ingredients, cuisine, dietary);
        return ResponseEntity.ok(recipe);
    }

    @GetMapping("/generate-structured")
    public ResponseEntity<Recipe> generateStructuredRecipe(
            @RequestParam String ingredients,
            @RequestParam(defaultValue = "any") String cuisine,
            @RequestParam(defaultValue = "none") String dietary) {

        Recipe recipe = recipeService.createStructuredRecipe(ingredients, cuisine, dietary);
        return ResponseEntity.ok(recipe);
    }

    @GetMapping(value = "/generate-safe", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> generateSafeRecipe(
            @RequestParam String userId,
            @RequestParam String ingredients,
            @RequestParam(defaultValue = "any") String cuisine) {
        String recipe = recipeService.createSafeRecipe(userId, ingredients, cuisine);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(recipe);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Recipe service is running!");
    }

}