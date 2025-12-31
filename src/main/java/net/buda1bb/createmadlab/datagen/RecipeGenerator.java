package net.buda1bb.createmadlab.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.buda1bb.createmadlab.CreateMadLab;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RecipeGenerator {

    private static final String MOD_ID = CreateMadLab.MOD_ID;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        System.out.println("Generating lacing recipe files...");

        try {
            // Read the laceables tag to get all items
            List<String> laceableItems = readLaceablesTag();
            System.out.println("Found " + laceableItems.size() + " items in laceables tag");

            // Define the fluids and their content types with custom amounts
            String[][] fluidTypes = {
                    {"heroin_solution", "bliss", "100"},
                    {"morphine_solution", "morphine", "100"},
                    {"lsd_solution", "lsd", "50"}
            };

            int generated = 0;

            // Generate recipes for each item in the tag for each fluid type
            for (String item : laceableItems) {
                for (String[] fluid : fluidTypes) {
                    String fluidName = fluid[0];
                    String content = fluid[1];
                    String amount = fluid[2];

                    if (generateLacingRecipe(item, fluidName, content, amount)) {
                        generated++;
                    }
                }
            }

            System.out.println("Successfully generated " + generated + " recipe files!");

        } catch (Exception e) {
            System.err.println("Error generating recipes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> readLaceablesTag() {
        List<String> items = new ArrayList<>();
        try {
            Path tagPath = Paths.get("src/main/resources/data/forge/tags/items/laceables.json");
            if (!Files.exists(tagPath)) {
                System.err.println("Laceables tag not found at: " + tagPath);
                return items;
            }

            JsonElement tagJson = JsonParser.parseReader(new FileReader(tagPath.toFile()));
            JsonObject tagObj = tagJson.getAsJsonObject();

            if (tagObj.has("values")) {
                for (JsonElement value : tagObj.getAsJsonArray("values")) {
                    String item = value.getAsString();
                    items.add(item);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to read laceables tag: " + e.getMessage());
        }
        return items;
    }

    private static boolean generateLacingRecipe(String itemId, String fluidName, String content, String amount) {
        try {
            JsonObject recipeJson = createLacingRecipeJson(itemId, fluidName, content, amount);

            // Extract the base item name for the filename (remove namespace if it's minecraft:)
            String itemName = itemId;
            if (itemId.contains(":")) {
                itemName = itemId.split(":")[1];
            }

            // Create the filename: itemname_fluidname_lacing.json
            String filename = itemName + "_" + fluidName + "_lacing.json";
            Path outputPath = getRecipePath(filename);

            // Create directories if they don't exist
            Files.createDirectories(outputPath.getParent());

            // Write the file
            try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                GSON.toJson(recipeJson, writer);
            }

            System.out.println("Generated: " + outputPath);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to generate recipe for " + itemId + " with " + fluidName + ": " + e.getMessage());
            return false;
        }
    }

    private static JsonObject createLacingRecipeJson(String itemId, String fluidName, String content, String amount) {
        JsonObject json = new JsonObject();
        json.addProperty("type", MOD_ID + ":custom_filling");

        // Ingredients array
        JsonArray ingredients = new JsonArray();

        // First ingredient - the specific item (not the tag)
        JsonObject itemIngredient = new JsonObject();
        itemIngredient.addProperty("item", itemId);
        ingredients.add(itemIngredient);

        // Second ingredient - the fluid with custom amount
        JsonObject fluidIngredient = new JsonObject();
        fluidIngredient.addProperty("fluid", MOD_ID + ":" + fluidName);
        fluidIngredient.addProperty("amount", Integer.parseInt(amount));
        ingredients.add(fluidIngredient);

        json.add("ingredients", ingredients);

        // Results array
        JsonArray results = new JsonArray();
        JsonObject result = new JsonObject();

        // Result is the same item but with NBT
        result.addProperty("item", itemId);

        // NBT data
        JsonObject nbt = new JsonObject();
        nbt.addProperty("content", content);
        result.add("nbt", nbt);

        results.add(result);
        json.add("results", results);

        return json;
    }

    private static Path getRecipePath(String recipeName) {
        // Output to the correct directory: data/createmadlab/recipes/lacing/
        return Paths.get("src/main/resources/data/" + MOD_ID + "/recipes/lacing/" + recipeName);
    }
}