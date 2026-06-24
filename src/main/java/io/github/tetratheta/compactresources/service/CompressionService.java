package io.github.tetratheta.compactresources.service;

import io.github.tetratheta.compactresources.compression.CompressedMaterial;
import io.github.tetratheta.compactresources.compression.CompressedResource;
import io.github.tetratheta.compactresources.compression.CompressionLevel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/// Creates, identifies, recipes, and inventory compaction behavior for compressed resource items.
public class CompressionService {
  private static final String KEY_MATERIAL = "compressed_material";
  private static final String KEY_LEVEL = "compression_level";
  private final JavaPlugin plugin;
  private final StackSizeService stackSizeService;
  private final NamespacedKey materialKey;
  private final NamespacedKey levelKey;
  private final List<NamespacedKey> recipeKeys;

  /// Creates a compression service bound to one plugin runtime.
  ///
  /// @param plugin           plugin used for keys and recipe registration
  /// @param stackSizeService service used when new items need custom stack-size metadata
  public CompressionService(JavaPlugin plugin, StackSizeService stackSizeService) {
    this.plugin = plugin;
    this.stackSizeService = stackSizeService;
    materialKey = new NamespacedKey(plugin, KEY_MATERIAL);
    levelKey = new NamespacedKey(plugin, KEY_LEVEL);
    recipeKeys = new ArrayList<>();
  }

  /// Registers broad Bukkit recipes and lets the listener validate exact item data.
  public void registerRecipes() {
    unregisterRecipes();
    for (CompressedMaterial material : CompressedMaterial.values()) registerBaseCompressionRecipe(material);
    registerCompressedResourceCompressionRecipe();
    registerCompressedResourceDecompressionRecipe();
    plugin.getLogger().info("Registered " + recipeKeys.size() + " compression recipes.");
  }

  /// Removes all recipes registered by this runtime.
  public void unregisterRecipes() {
    for (NamespacedKey key : recipeKeys) plugin.getServer().removeRecipe(key);
    recipeKeys.clear();
  }

  /// Returns whether the key belongs to a recipe registered by this runtime.
  ///
  /// @param key recipe key to test
  /// @return true when this service registered the recipe
  public boolean isRegisteredRecipe(NamespacedKey key) {
    return recipeKeys.contains(key);
  }

  /// Returns all recipe keys registered by this runtime.
  ///
  /// @return registered recipe keys
  public Collection<NamespacedKey> getRecipeKeys() {
    return List.copyOf(recipeKeys);
  }

  /// Creates one compressed resource item.
  ///
  /// @param material compressed base material
  /// @param level    compression level
  /// @param amount   stack amount
  /// @return compressed resource item stack
  public ItemStack createItem(CompressedMaterial material, CompressionLevel level, int amount) {
    ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA, amount);
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return item;
    meta.getPersistentDataContainer().set(materialKey, PersistentDataType.STRING, material.id());
    meta.getPersistentDataContainer().set(levelKey, PersistentDataType.STRING, level.id());
    meta.itemName(Component.translatable(material.translationKey()).append(Component.text(" " + level.id())));
    meta.setRarity(ItemRarity.COMMON);
    meta.setItemModel(getItemModelKey(material, level));
    item.setItemMeta(meta);
    return item;
  }

  /// Returns whether an item is one of this plugin's compressed resources.
  ///
  /// @param item item to inspect
  /// @return true when the item has valid compressed resource metadata
  public boolean isCompressedResource(ItemStack item) {
    return decode(item) != null;
  }

  /// Decodes a compressed resource item.
  ///
  /// @param item item to inspect
  /// @return decoded compressed resource, or null when the item is not valid
  public CompressedResource decode(ItemStack item) {
    if (item == null || item.getType() != Material.HEART_OF_THE_SEA) return null;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return null;
    String materialId = meta.getPersistentDataContainer().get(materialKey, PersistentDataType.STRING);
    String levelId = meta.getPersistentDataContainer().get(levelKey, PersistentDataType.STRING);
    if (materialId == null || levelId == null) return null;
    CompressedMaterial material = CompressedMaterial.fromId(materialId);
    CompressionLevel level = CompressionLevel.fromId(levelId);
    return material == null || level == null ? null : new CompressedResource(material, level);
  }

  /// Returns whether an item has exactly the compressed resource identity used by recipes.
  ///
  /// @param item     item to inspect
  /// @param material expected compressed material
  /// @param level    expected compression level
  /// @return true when material and PDC values match
  public boolean hasCompressedIdentity(ItemStack item, CompressedMaterial material, CompressionLevel level) {
    if (item == null || item.getType() != Material.HEART_OF_THE_SEA) return false;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return false;
    String materialId = meta.getPersistentDataContainer().get(materialKey, PersistentDataType.STRING);
    String levelId = meta.getPersistentDataContainer().get(levelKey, PersistentDataType.STRING);
    return material.id().equals(materialId) && level.id().equals(levelId);
  }

  /// Creates a trusted custom crafting result from a broad registered recipe and matrix.
  ///
  /// @param matrix current crafting matrix
  /// @return custom result, or null when the input is not a valid compression recipe
  public ItemStack createCraftingResult(ItemStack[] matrix) {
    ItemStack decompressionResult = createDecompressionResult(matrix);
    if (decompressionResult != null) {
      stackSizeService.applyCustomStackSize(decompressionResult);
      return decompressionResult;
    }
    ItemStack compressedResourceResult = createCompressedResourceCompressionResult(matrix);
    if (compressedResourceResult != null) {
      stackSizeService.applyCustomStackSize(compressedResourceResult);
      return compressedResourceResult;
    }
    ItemStack baseCompressionResult = createBaseCompressionResult(matrix);
    if (baseCompressionResult != null) stackSizeService.applyCustomStackSize(baseCompressionResult);
    return baseCompressionResult;
  }

  /// Compacts supported base materials and lower compressed tiers inside a player inventory.
  ///
  /// @param player player whose inventory should be compacted
  /// @return true when at least one conversion happened
  public boolean compactInventory(Player player) {
    boolean compacted = false;
    PlayerInventory inventory = player.getInventory();
    for (CompressedMaterial material : CompressedMaterial.values()) {
      compacted |= compactMaterial(player, material, null, CompressionLevel.X9);
      compacted |= compactMaterial(player, material, CompressionLevel.X9, CompressionLevel.X81);
      compacted |= compactMaterial(player, material, CompressionLevel.X81, CompressionLevel.X729);
    }
    return compacted;
  }

  /// Registers one 3x3 base material compression recipe.
  private void registerBaseCompressionRecipe(CompressedMaterial material) {
    NamespacedKey key = new NamespacedKey(plugin, "compress_" + material.id() + "_x9");
    ShapedRecipe recipe = new ShapedRecipe(key, createItem(material, CompressionLevel.X9, 1));
    recipe.shape("AAA", "AAA", "AAA");
    recipe.setIngredient('A', material.baseMaterial());
    addRecipe(key, recipe);
  }

  /// Registers the broad 3x3 Heart of the Sea recipe for compressed resource tier upgrades.
  private void registerCompressedResourceCompressionRecipe() {
    NamespacedKey key = new NamespacedKey(plugin, "compress_resource");
    ShapedRecipe recipe = new ShapedRecipe(key, createItem(CompressedMaterial.DIRT, CompressionLevel.X81, 1));
    recipe.shape("AAA", "AAA", "AAA");
    recipe.setIngredient('A', Material.HEART_OF_THE_SEA);
    addRecipe(key, recipe);
  }

  /// Registers the broad single Heart of the Sea recipe for compressed resource decompression.
  private void registerCompressedResourceDecompressionRecipe() {
    NamespacedKey key = new NamespacedKey(plugin, "decompress_resource");
    ShapelessRecipe recipe = new ShapelessRecipe(key, createItem(CompressedMaterial.DIRT, CompressionLevel.X9, 9));
    recipe.addIngredient(Material.HEART_OF_THE_SEA);
    addRecipe(key, recipe);
  }

  /// Removes any stale recipe with the same key and registers the new recipe.
  private void addRecipe(NamespacedKey key, Recipe recipe) {
    plugin.getServer().removeRecipe(key);
    if (plugin.getServer().addRecipe(recipe)) recipeKeys.add(key);
    else plugin.getLogger().warning("Failed to register compression recipe: " + key);
  }

  /// Applies one compression step as many times as possible.
  private boolean compactMaterial(Player player, CompressedMaterial material, CompressionLevel inputLevel, CompressionLevel resultLevel) {
    PlayerInventory inventory = player.getInventory();
    int amount = countItems(inventory, material, inputLevel);
    int consumed = amount - amount % 9;
    if (consumed <= 0) return false;
    removeItems(inventory, material, inputLevel, consumed);
    ItemStack result = createItem(material, resultLevel, consumed / 9);
    stackSizeService.applyCustomStackSize(result);
    Map<Integer, ItemStack> leftovers = inventory.addItem(result);
    leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    return true;
  }

  /// Counts all inventory items matching one compression input.
  private int countItems(PlayerInventory inventory, CompressedMaterial material, CompressionLevel level) {
    int amount = 0;
    for (ItemStack item : inventory.getStorageContents()) {
      if (matchesInput(item, material, level)) amount += item.getAmount();
    }
    return amount;
  }

  /// Removes a requested amount of matching input from storage slots.
  private void removeItems(PlayerInventory inventory, CompressedMaterial material, CompressionLevel level, int amount) {
    ItemStack[] contents = inventory.getStorageContents();
    int remaining = amount;
    for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
      ItemStack item = contents[slot];
      if (!matchesInput(item, material, level)) continue;
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

  /// Checks whether an item can be consumed by one compression step.
  private boolean matchesInput(ItemStack item, CompressedMaterial material, CompressionLevel level) {
    if (item == null || item.getType() == Material.AIR) return false;
    if (level == null) return item.getType() == material.baseMaterial() && !isCompressedResource(item);
    return hasCompressedIdentity(item, material, level);
  }

  /// Creates a compression result from nine identical base materials.
  private ItemStack createBaseCompressionResult(ItemStack[] matrix) {
    int filledSlots = 0;
    CompressedMaterial firstMaterial = null;
    for (ItemStack item : matrix) {
      if (item == null || item.getType() == Material.AIR) continue;
      filledSlots++;
      CompressedMaterial material = CompressedMaterial.fromMaterial(item.getType());
      if (material == null || isCompressedResource(item)) return null;
      if (firstMaterial == null) firstMaterial = material;
      else if (firstMaterial != material) return null;
    }
    return filledSlots == 9 && firstMaterial != null ? createItem(firstMaterial, CompressionLevel.X9, 1) : null;
  }

  /// Creates a compression result from nine identical compressed resource items.
  private ItemStack createCompressedResourceCompressionResult(ItemStack[] matrix) {
    int filledSlots = 0;
    CompressedResource firstResource = null;
    for (ItemStack item : matrix) {
      if (item == null || item.getType() == Material.AIR) continue;
      filledSlots++;
      CompressedResource resource = decode(item);
      if (resource == null || resource.level().next() == null) return null;
      if (!hasCompressedIdentity(item, resource.material(), resource.level())) return null;
      if (firstResource == null) {
        firstResource = resource;
      } else if (!firstResource.equals(resource)) {
        return null;
      }
    }
    return filledSlots == 9 && firstResource != null ? createItem(firstResource.material(), firstResource.level().next(), 1) : null;
  }

  /// Creates a decompression result from one compressed resource item in any crafting grid.
  private ItemStack createDecompressionResult(ItemStack[] matrix) {
    int filledSlots = 0;
    CompressedResource resource = null;
    for (ItemStack item : matrix) {
      if (item == null || item.getType() == Material.AIR) continue;
      filledSlots++;
      resource = decode(item);
      if (resource == null || !hasCompressedIdentity(item, resource.material(), resource.level())) return null;
    }
    return filledSlots == 1 && resource != null ? createDecompressionResult(resource) : null;
  }

  /// Creates the output stack for one compressed resource decompression.
  private ItemStack createDecompressionResult(CompressedResource resource) {
    CompressionLevel previous = resource.level().previous();
    return previous == null ? new ItemStack(resource.material().baseMaterial(), 9) : createItem(resource.material(), previous, 9);
  }

  /// Returns the resource-pack item model key for one compressed resource item.
  private NamespacedKey getItemModelKey(CompressedMaterial material, CompressionLevel level) {
    return new NamespacedKey(plugin, "item/compressed/" + material.id() + "_" + level.id());
  }
}
