package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.service.CompressedBlockService;
import org.bukkit.Keyed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/// Protects compressed blocks from vanilla item behavior that would break their domain rules.
public class CREventCompression implements Listener {
  private final CompressedBlockService compressedBlockService;

  /// Creates a compression event listener.
  ///
  /// @param compressedBlockService service used to identify compressed block items
  public CREventCompression(CompressedBlockService compressedBlockService) {
    this.compressedBlockService = compressedBlockService;
  }

  /// Prevents Heart of the Sea based compressed blocks from being placed in the world.
  ///
  /// Heart of the Sea isn't placeable but I want to ensure this.
  ///
  /// @param e block place event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockPlace(BlockPlaceEvent e) {
    if (compressedBlockService.isCompressedBlock(e.getItemInHand())) e.setCancelled(true);
  }

  /// Prevents compressed blocks from being consumed by unrelated Heart of the Sea recipes.
  ///
  /// @param e crafting preparation event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPrepareItemCraft(PrepareItemCraftEvent e) {
    Recipe recipe = e.getRecipe();
    if (isCompressedBlockRecipe(recipe)) {
      ItemStack result = compressedBlockService.createCraftingResult(e.getInventory().getMatrix());
      e.getInventory().setResult(result);
      return;
    }

    for (ItemStack item : e.getInventory().getMatrix()) {
      if (!compressedBlockService.isCompressedBlock(item)) continue;
      e.getInventory().setResult(null);
      return;
    }
  }

  /// Returns whether a recipe belongs to the compressed block service.
  private boolean isCompressedBlockRecipe(Recipe recipe) {
    if (!(recipe instanceof CraftingRecipe) || !(recipe instanceof Keyed keyed)) return false;
    return compressedBlockService.isRegisteredRecipe(keyed.getKey());
  }
}
