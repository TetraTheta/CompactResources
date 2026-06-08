package io.github.tetratheta.compactresources.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/// Finds reversible compacting recipes and applies them to player inventories.
public class CompactService {
  private final JavaPlugin plugin;
  private final StackSizeService stackSizeService;

  /// Creates a compacting service backed by the server recipe registry.
  ///
  /// @param plugin plugin instance used to access the server
  /// @param stackSizeService service used to compare and update custom stack sizes
  public CompactService(JavaPlugin plugin, StackSizeService stackSizeService) {
    this.plugin = plugin;
    this.stackSizeService = stackSizeService;
  }

  /// Compacts every eligible item group in a player's storage inventory.
  ///
  /// @param player player whose inventory should be compacted
  /// @return true when at least one item group was compacted
  public boolean compactInventory(Player player) {
    boolean compacted = false;
    List<ItemStack> candidates = new ArrayList<>();
    List<Recipe> recipes = getRecipes();

    for (ItemStack item : player.getInventory().getStorageContents()) {
      if (item == null || item.getType() == Material.AIR || containsSimilarItem(candidates, item))
        continue;
      candidates.add(item.clone());
    }

    for (ItemStack candidate : candidates) {
      if (compactItem(player, candidate, recipes)) compacted = true;
    }
    return compacted;
  }

  /// Checks whether an equivalent item sample already exists in the candidate list.
  ///
  /// @param candidates existing unique item samples
  /// @param item item stack to compare
  /// @return true when an equivalent candidate already exists
  private boolean containsSimilarItem(List<ItemStack> candidates, ItemStack item) {
    return candidates.stream()
        .anyMatch(candidate -> isSimilarWithoutCustomStackSize(candidate, item));
  }

  /// Compacts all inventory stacks equivalent to a sample item when a reversible recipe exists.
  ///
  /// @param player player whose inventory should be modified
  /// @param sample item sample used to find matching inventory stacks
  /// @param recipes recipe snapshot for the current command execution
  /// @return true when items were consumed and compacted results were added
  private boolean compactItem(Player player, ItemStack sample, List<Recipe> recipes) {
    CompactRecipe recipe = getCompactRecipe(sample, recipes);
    if (recipe == null) return false;

    int amount = 0;
    for (ItemStack item : player.getInventory().getStorageContents()) {
      if (item != null && isSimilarWithoutCustomStackSize(item, sample)) amount += item.getAmount();
    }

    int inputAmount = recipe.input().getAmount();
    int consumedAmount = amount - amount % inputAmount;
    if (consumedAmount <= 0) return false;

    removeItems(player.getInventory(), sample, consumedAmount);

    ItemStack result = recipe.result().clone();
    result.setAmount((consumedAmount / inputAmount) * result.getAmount());
    stackSizeService.applyCustomStackSize(result);
    player
        .getInventory()
        .addItem(result)
        .values()
        .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    return true;
  }

  /// Takes a snapshot of the current server recipes for one compact command execution.
  ///
  /// @return current server recipes
  private List<Recipe> getRecipes() {
    List<Recipe> recipes = new ArrayList<>();
    Iterator<Recipe> iterator = plugin.getServer().recipeIterator();
    while (iterator.hasNext()) recipes.add(iterator.next());
    return recipes;
  }

  /// Removes a requested amount of items equivalent to a sample from a player inventory.
  ///
  /// @param inventory inventory to modify
  /// @param sample item sample used for matching
  /// @param amount total amount to remove
  private void removeItems(PlayerInventory inventory, ItemStack sample, int amount) {
    ItemStack[] contents = inventory.getStorageContents();
    int remaining = amount;

    for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
      ItemStack item = contents[slot];
      if (item == null || !isSimilarWithoutCustomStackSize(item, sample)) continue;

      int removed = Math.min(item.getAmount(), remaining);
      remaining -= removed;

      if (item.getAmount() == removed) {
        inventory.setItem(slot, null);
      } else {
        item.setAmount(item.getAmount() - removed);
        inventory.setItem(slot, item);
      }
    }
  }

  /// Finds the best reversible 2x2 or 3x3 compacting recipe for an item stack.
  ///
  /// @param stack item stack to compact
  /// @param recipes recipe snapshot for the current command execution
  /// @return compacting recipe, or null when no reversible recipe exists
  private CompactRecipe getCompactRecipe(ItemStack stack, List<Recipe> recipes) {
    CompactRecipe bestRecipe = null;

    for (Recipe recipe : recipes) {
      int ingredientAmount = getMatchingIngredientAmount(recipe, stack);
      ItemStack result = recipe.getResult();

      if ((ingredientAmount == 4 || ingredientAmount == 9)
          && ingredientAmount > result.getAmount()
          && hasDecompactRecipe(result, stack, ingredientAmount, recipes)) {
        ItemStack input = stack.clone();
        input.setAmount(ingredientAmount);
        CompactRecipe compactRecipe = new CompactRecipe(result.clone(), input);

        if (bestRecipe == null
            || compactRecipe.input().getAmount() > bestRecipe.input().getAmount()) {
          bestRecipe = compactRecipe;
        }
      }
    }

    return bestRecipe;
  }

  /// Checks whether a compacted result can be crafted back into the original ingredient amount.
  ///
  /// @param compacted compacted item produced by a candidate recipe
  /// @param ingredient original ingredient item
  /// @param ingredientAmount amount of original ingredients consumed by the candidate recipe
  /// @param recipes recipe snapshot for the current command execution
  /// @return true when a matching decompacting recipe exists
  private boolean hasDecompactRecipe(
      ItemStack compacted, ItemStack ingredient, int ingredientAmount, List<Recipe> recipes) {
    for (Recipe recipe : recipes) {
      if (recipe.getResult().getAmount() * compacted.getAmount() != ingredientAmount) continue;

      if (!isSimilarWithoutCustomStackSize(recipe.getResult(), ingredient)) continue;

      if (getMatchingIngredientAmount(recipe, compacted) == compacted.getAmount()) return true;
    }
    return false;
  }

  /// Counts matching recipe slots only when every non-empty ingredient matches a stack.
  ///
  /// @param recipe recipe to test
  /// @param stack item stack to match against ingredients
  /// @return matching ingredient count, or 0 when the recipe is unsupported or not uniform
  private int getMatchingIngredientAmount(Recipe recipe, ItemStack stack) {
    ItemStack comparableStack = stackSizeService.withoutCustomStackSize(stack);

    if (recipe instanceof ShapedRecipe shapedRecipe) {
      return getMatchingShapedIngredientAmount(shapedRecipe, comparableStack);
    } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
      return getMatchingIngredientAmount(shapelessRecipe.getChoiceList(), comparableStack);
    }
    return 0;
  }

  /// Counts matching shaped recipe slots from the shape grid, not from unique recipe symbols.
  ///
  /// @param recipe shaped recipe to test
  /// @param stack item stack to match against ingredients
  /// @return matching ingredient count, or 0 when the recipe is not square or not uniform
  private int getMatchingShapedIngredientAmount(ShapedRecipe recipe, ItemStack stack) {
    String[] shape = recipe.getShape();
    if (shape.length == 0) return 0;

    int width = shape[0].length();
    if (shape.length != width) return 0;

    int amount = 0;
    Map<Character, RecipeChoice> choices = recipe.getChoiceMap();
    for (String row : shape) {
      if (row.length() != width) return 0;

      for (int index = 0; index < row.length(); index++) {
        RecipeChoice ingredient = choices.get(row.charAt(index));
        if (ingredient == null) continue;
        if (!ingredient.test(stack)) return 0;
        amount++;
      }
    }
    return amount;
  }

  /// Counts matching ingredient choices only when every non-empty choice matches a stack.
  ///
  /// @param ingredients ingredient choices to test
  /// @param stack item stack to match against ingredients
  /// @return matching ingredient count, or 0 when any ingredient differs
  private int getMatchingIngredientAmount(Iterable<RecipeChoice> ingredients, ItemStack stack) {
    int amount = 0;
    for (RecipeChoice ingredient : ingredients) {
      if (ingredient == null) continue;
      if (!ingredient.test(stack)) return 0;
      amount++;
    }
    return amount;
  }

  /// Compares two item stacks while ignoring custom max stack-size metadata.
  ///
  /// @param first first stack
  /// @param second second stack
  /// @return true when both stacks are similar after removing custom stack-size metadata
  private boolean isSimilarWithoutCustomStackSize(ItemStack first, ItemStack second) {
    return stackSizeService
        .withoutCustomStackSize(first)
        .isSimilar(stackSizeService.withoutCustomStackSize(second));
  }

  /// Stores the compacted result and the ingredient stack required to produce it.
  ///
  /// @param result compacted recipe result
  /// @param input ingredient stack required by the compacting recipe
  private record CompactRecipe(ItemStack result, ItemStack input) {}
}
