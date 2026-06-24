package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.service.StackSizeService;
import java.util.function.Consumer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;

/// Reapplies configured stack-size metadata after Bukkit inventory and item events mutate items.
public class CREventMaxStack implements Listener {
  private final Consumer<Runnable> nextTickScheduler;
  private final StackSizeService stackSizeService;

  /// Creates an event listener backed by the active stack-size service.
  ///
  /// @param stackSizeService  service used to apply configured stack sizes
  /// @param nextTickScheduler runtime-owned scheduler for delayed metadata updates
  public CREventMaxStack(StackSizeService stackSizeService, Consumer<Runnable> nextTickScheduler) {
    this.nextTickScheduler = nextTickScheduler;
    this.stackSizeService = stackSizeService;
  }

  /// Reapplies stack-size metadata after a dispenser exposes or moves an item stack.
  ///
  /// @param e block dispense event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockDispense(BlockDispenseEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getItem()));
  }

  /// Reapplies stack-size metadata after an entity picks up an item.
  ///
  /// @param e entity pickup event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onEntityPickup(EntityPickupItemEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getItem().getItemStack()));
  }

  /// Reapplies stack-size metadata after a furnace creates a smelt result.
  ///
  /// @param e furnace smelt event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onFurnaceSmelt(FurnaceSmeltEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getResult()));
  }

  /// Reapplies stack-size metadata after a player inventory click updates cursor and slot state.
  ///
  /// @param e inventory click event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(InventoryClickEvent e) {
    nextTickScheduler.accept(() -> {
      applyCustomStackSize(e.getCurrentItem());
      applyCustomStackSize(e.getCursor());
      fixInventory(e.getWhoClicked().getInventory());
    });
  }

  /// Reapplies stack-size metadata after creative inventory edits change player-held items.
  ///
  /// @param e creative inventory event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryCreative(InventoryCreativeEvent e) {
    nextTickScheduler.accept(() -> {
      applyCustomStackSize(e.getCurrentItem());
      applyCustomStackSize(e.getCursor());
      fixInventory(e.getWhoClicked().getInventory());
    });
  }

  /// Reapplies stack-size metadata after inventories transfer an item stack.
  ///
  /// @param e inventory move event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryMove(InventoryMoveItemEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getItem()));
  }

  /// Reapplies stack-size metadata after an inventory picks up a dropped item entity.
  ///
  /// @param e inventory pickup event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryPickupItem(InventoryPickupItemEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getItem().getItemStack()));
  }

  /// Reapplies stack-size metadata after a new dropped item entity spawns.
  ///
  /// @param e item spawn event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onItemSpawn(ItemSpawnEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getEntity().getItemStack()));
  }

  /// Reapplies stack-size metadata after a crafter produces a result.
  ///
  /// @param e crafter craft event
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPrepareCrafter(CrafterCraftEvent e) {
    nextTickScheduler.accept(() -> applyCustomStackSize(e.getResult()));
  }

  /// Reapplies stack-size metadata to every item in an inventory.
  ///
  /// @param inventory inventory to fix
  private void fixInventory(Inventory inventory) {
    if (inventory == null) return;
    for (var item : inventory.getContents()) applyCustomStackSize(item);
  }

  /// Applies stack-size metadata unless the item is a compressed resource.
  ///
  /// @param item item to update
  private void applyCustomStackSize(org.bukkit.inventory.ItemStack item) {
    stackSizeService.applyCustomStackSize(item);
  }
}
