package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.compression.CompressedMaterial;
import io.github.tetratheta.compactresources.compression.CompressionLevel;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/// Migrates legacy compressed block item metadata to the current item_model format.
public class CREventCompressedBlockMigration implements Listener {
  private static final String KEY_MATERIAL = "compressed_material";
  private static final String KEY_LEVEL = "compression_level";

  private final JavaPlugin plugin;
  private final NamespacedKey materialKey;
  private final NamespacedKey levelKey;
  private final Consumer<Runnable> nextTickScheduler;

  /// Creates a temporary metadata migration listener.
  ///
  /// @param plugin plugin used for namespaced keys
  /// @param nextTickScheduler runtime-owned scheduler for delayed inventory updates
  public CREventCompressedBlockMigration(JavaPlugin plugin, Consumer<Runnable> nextTickScheduler) {
    this.plugin = plugin;
    this.nextTickScheduler = nextTickScheduler;
    materialKey = new NamespacedKey(plugin, KEY_MATERIAL);
    levelKey = new NamespacedKey(plugin, KEY_LEVEL);
  }

  /// Migrates compressed blocks in a player's inventory after join-time item loading.
  ///
  /// @param e player join event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent e) {
    nextTickScheduler.accept(() -> migratePlayerInventory(e.getPlayer()));
  }

  /// Migrates compressed blocks after inventory clicks expose old items.
  ///
  /// @param e inventory click event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClick(InventoryClickEvent e) {
    nextTickScheduler.accept(
        () -> {
          migrateItem(e.getCurrentItem());
          migrateItem(e.getCursor());
          migrateInventory(e.getWhoClicked().getInventory());
        });
  }

  /// Migrates compressed blocks after creative inventory edits expose old items.
  ///
  /// @param e creative inventory event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryCreative(InventoryCreativeEvent e) {
    nextTickScheduler.accept(
        () -> {
          migrateItem(e.getCurrentItem());
          migrateItem(e.getCursor());
          migrateInventory(e.getWhoClicked().getInventory());
        });
  }

  /// Migrates compressed block drops after player pickup.
  ///
  /// @param e entity pickup event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityPickup(EntityPickupItemEvent e) {
    nextTickScheduler.accept(() -> migrateItem(e.getItem().getItemStack()));
  }

  /// Migrates compressed block drops after hopper-like inventory pickup.
  ///
  /// @param e inventory pickup event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryPickupItem(InventoryPickupItemEvent e) {
    nextTickScheduler.accept(() -> migrateItem(e.getItem().getItemStack()));
  }

  /// Migrates all reachable item slots in one player inventory.
  private void migratePlayerInventory(Player player) {
    migrateInventory(player.getInventory());
  }

  /// Migrates all item stacks in one inventory.
  private void migrateInventory(Inventory inventory) {
    if (inventory == null) return;
    for (ItemStack item : inventory.getContents()) {
      migrateItem(item);
    }
  }

  /// Rewrites old compressed block metadata into the current client-facing shape.
  private void migrateItem(ItemStack item) {
    if (item == null || item.getType() != Material.HEART_OF_THE_SEA) return;

    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;

    String materialId =
        meta.getPersistentDataContainer().get(materialKey, PersistentDataType.STRING);
    String levelId = meta.getPersistentDataContainer().get(levelKey, PersistentDataType.STRING);
    if (materialId == null || levelId == null) return;

    CompressedMaterial material = CompressedMaterial.fromId(materialId);
    CompressionLevel level = CompressionLevel.fromId(levelId);
    if (material == null || level == null) return;

    meta.setRarity(ItemRarity.COMMON);
    meta.setItemModel(getItemModelKey(material, level));
    clearLegacyCustomModelData(meta);
    item.setItemMeta(meta);
  }

  /// Removes the obsolete custom_model_data component once item_model is present.
  @SuppressWarnings("UnstableApiUsage")
  private void clearLegacyCustomModelData(ItemMeta meta) {
    meta.setCustomModelDataComponent(null);
  }

  /// Returns the resource-pack item_model key for one compressed block.
  private NamespacedKey getItemModelKey(CompressedMaterial material, CompressionLevel level) {
    return new NamespacedKey(plugin, "item/compressed/" + material.id() + "_" + level.id());
  }
}
