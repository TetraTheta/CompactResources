package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.service.CompressionService;
import org.bukkit.Keyed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/// Protects compressed resource items from vanilla behavior that would break their domain rules.
public class CREventCompression implements Listener {
  private final CompressionService compressionService;

  /// Creates a compression event listener.
  ///
  /// @param compressionService service used to identify compressed resource items
  public CREventCompression(CompressionService compressionService) {
    this.compressionService = compressionService;
  }

  /// Prevents Heart of the Sea based compressed resources from being placed in the world.
  ///
  /// Heart of the Sea isn't placeable but I want to ensure this.
  ///
  /// @param e block place event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockPlace(BlockPlaceEvent e) {
    if (compressionService.isCompressedResource(e.getItemInHand())) e.setCancelled(true);
  }

  /// Prevents compressed resources from being consumed by unrelated Heart of the Sea recipes.
  ///
  /// @param e crafting preparation event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPrepareItemCraft(PrepareItemCraftEvent e) {
    Recipe recipe = e.getRecipe();
    if (isCompressionRecipe(recipe)) {
      ItemStack result = compressionService.createCraftingResult(e.getInventory().getMatrix());
      e.getInventory().setResult(result);
      return;
    }
    for (ItemStack item : e.getInventory().getMatrix()) {
      if (!compressionService.isCompressedResource(item)) continue;
      e.getInventory().setResult(null);
      return;
    }
  }

  /// Returns whether a recipe belongs to the compression service.
  private boolean isCompressionRecipe(Recipe recipe) {
    if (!(recipe instanceof CraftingRecipe) || !(recipe instanceof Keyed keyed)) return false;
    return compressionService.isRegisteredRecipe(keyed.getKey());
  }
}
