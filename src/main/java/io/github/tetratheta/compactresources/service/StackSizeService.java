package io.github.tetratheta.compactresources.service;

import io.github.tetratheta.compactresources.config.StackSizeRules;
import io.github.tetratheta.mol.message.MessageService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/// Applies configured max stack sizes to item stacks and strips that metadata for comparisons.
public class StackSizeService {
  private final Map<Material, Integer> configuredStackSizes;

  /// Creates a service from normalized stack-size rules.
  ///
  /// @param rules normalized stack-size rules
  /// @param messageService message service used while resolving configuration warnings
  public StackSizeService(StackSizeRules rules, MessageService messageService) {
    configuredStackSizes = buildConfiguredStackSizes(rules, messageService);
  }

  /// Applies a configured max stack size to an item stack when a rule matches its material.
  ///
  /// @param item item stack to update
  public void applyCustomStackSize(ItemStack item) {
    if (item == null || item.getType() == Material.AIR) return;

    Integer maxStackSize = configuredStackSizes.get(item.getType());
    if (maxStackSize == null) return;

    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;

    meta.setMaxStackSize(maxStackSize);
    item.setItemMeta(meta);
  }

  /// Applies configured max stack sizes to every item stack in an array.
  ///
  /// @param items item stacks to update
  public void applyCustomStackSize(ItemStack[] items) {
    if (items == null) return;

    for (ItemStack item : items) {
      applyCustomStackSize(item);
    }
  }

  /// Returns a clone of the item with custom max stack-size metadata removed.
  ///
  /// @param item item stack to clone
  /// @return clone without custom stack-size metadata
  public ItemStack withoutCustomStackSize(ItemStack item) {
    ItemStack clone = item.clone();
    ItemMeta meta = clone.getItemMeta();
    if (meta == null) return clone;

    meta.setMaxStackSize(null);
    clone.setItemMeta(meta);
    return clone;
  }

  /// Precomputes the effective stack-size rule for every item material.
  ///
  /// @param rules normalized stack-size rules
  /// @param messageService message service used for invalid tag warnings
  /// @return item material to max stack-size map
  private Map<Material, Integer> buildConfiguredStackSizes(
      StackSizeRules rules, MessageService messageService) {
    Map<Material, Integer> stackSizes = new EnumMap<>(Material.class);
    List<TagStackSizeRule> tagRules = resolveTagRules(rules, messageService);

    for (Material material : Material.values()) {
      if (!isModernItemMaterial(material)) continue;

      Integer maxStackSize = getConfiguredMaxStackSize(material, rules, tagRules);
      if (maxStackSize != null) stackSizes.put(material, maxStackSize);
    }
    return Map.copyOf(stackSizes);
  }

  /// Resolves the first matching stack-size rule for a material in configuration priority order.
  ///
  /// @param material material to resolve
  /// @param rules normalized stack-size rules
  /// @param tagRules resolved tag rules
  /// @return configured max stack size, or null when no rule matches
  private Integer getConfiguredMaxStackSize(
      Material material, StackSizeRules rules, List<TagStackSizeRule> tagRules) {
    String itemId = getItemId(material);
    StackSizeRules.DefaultRule defaultRule = rules.defaultRule();
    Integer defaultStackSize = defaultRule.enabled() ? defaultRule.maxStackSize() : null;
    if (itemId == null) return defaultStackSize;

    Integer idStackSize = rules.ids().get(itemId);
    if (idStackSize != null) return idStackSize;

    Integer tagStackSize = getTagStackSize(material, tagRules);
    if (tagStackSize != null) return tagStackSize;

    for (StackSizeRules.RegexRule rule : rules.regexRules()) {
      if (rule.pattern().matcher(itemId).matches()) return rule.maxStackSize();
    }
    return defaultStackSize;
  }

  /// Resolves configured tag IDs to Bukkit tags once before per-material matching starts.
  ///
  /// @param rules normalized stack-size rules
  /// @param messageService message service used for invalid tag warnings
  /// @return resolved tag rules
  private List<TagStackSizeRule> resolveTagRules(
      StackSizeRules rules, MessageService messageService) {
    List<TagStackSizeRule> tagRules = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : rules.tags().entrySet()) {
      NamespacedKey tagKey = NamespacedKey.fromString(entry.getKey());
      if (tagKey == null) {
        messageService.logWarning("log.stack-size.invalid-tag-id", entry.getKey());
        continue;
      }

      Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
      if (tag != null) tagRules.add(new TagStackSizeRule(tag, entry.getValue()));
    }
    return List.copyOf(tagRules);
  }

  /// Returns the max stack size from the first tag rule that contains the material.
  ///
  /// @param material material to test
  /// @param tagRules resolved tag rules
  /// @return configured max stack size, or null when no tag matches
  private Integer getTagStackSize(Material material, List<TagStackSizeRule> tagRules) {
    for (TagStackSizeRule rule : tagRules) {
      if (rule.tag().isTagged(material)) return rule.maxStackSize();
    }
    return null;
  }

  /// Returns whether a Bukkit material is a runtime item with a modern namespaced key.
  ///
  /// @param material material to test
  /// @return true when the material can safely be used with item registries
  private boolean isModernItemMaterial(Material material) {
    return !isLegacyMaterial(material) && material.isItem();
  }

  /// Returns whether a material is part of Bukkit's legacy compatibility enum values.
  ///
  /// @param material material to test
  /// @return true when the material must not be passed to modern registry APIs
  private boolean isLegacyMaterial(Material material) {
    return material.name().startsWith("LEGACY_");
  }

  /// Converts a material key into the normalized item ID used by configuration rules.
  ///
  /// @param material item material
  /// @return lower-case namespaced item ID, or null when the material has no key
  private String getItemId(Material material) {
    NamespacedKey key = material.getKey();
    return key == null ? null : key.asString().toLowerCase(Locale.ROOT);
  }

  /// Stores a resolved Bukkit item tag and the stack size applied by that tag.
  ///
  /// @param tag resolved Bukkit item tag
  /// @param maxStackSize maximum stack size to apply
  private record TagStackSizeRule(Tag<Material> tag, Integer maxStackSize) {}
}
