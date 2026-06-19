package io.github.tetratheta.compactresources.compression;

import java.util.Locale;
import org.bukkit.Material;

/// Defines every base material that can be represented as a compressed resource item.
public enum CompressedMaterial {
  AMETHYST_BLOCK(Material.AMETHYST_BLOCK),
  ANDESITE(Material.ANDESITE),
  ARROW(Material.ARROW),
  BASALT(Material.BASALT),
  BEETROOT(Material.BEETROOT),
  BLACKSTONE(Material.BLACKSTONE),
  BONE_BLOCK(Material.BONE_BLOCK),
  CALCITE(Material.CALCITE),
  CARROT(Material.CARROT),
  CLAY(Material.CLAY),
  COAL_BLOCK(Material.COAL_BLOCK),
  COBBLED_DEEPSLATE(Material.COBBLED_DEEPSLATE),
  COBBLESTONE(Material.COBBLESTONE),
  COPPER_BLOCK(Material.COPPER_BLOCK),
  DEEPSLATE(Material.DEEPSLATE),
  DIAMOND_BLOCK(Material.DIAMOND_BLOCK),
  DIORITE(Material.DIORITE),
  DIRT(Material.DIRT),
  END_STONE(Material.END_STONE),
  GOLD_BLOCK(Material.GOLD_BLOCK),
  GRANITE(Material.GRANITE),
  GRAVEL(Material.GRAVEL),
  HAY_BLOCK(Material.HAY_BLOCK),
  IRON_BLOCK(Material.IRON_BLOCK),
  LAPIS_BLOCK(Material.LAPIS_BLOCK),
  MAGMA_CREAM(Material.MAGMA_CREAM),
  NETHERITE_BLOCK(Material.NETHERITE_BLOCK),
  NETHERRACK(Material.NETHERRACK),
  POTATO(Material.POTATO),
  QUARTZ_BLOCK(Material.QUARTZ_BLOCK),
  RAW_COPPER_BLOCK(Material.RAW_COPPER_BLOCK),
  RAW_GOLD_BLOCK(Material.RAW_GOLD_BLOCK),
  RAW_IRON_BLOCK(Material.RAW_IRON_BLOCK),
  REDSTONE_BLOCK(Material.REDSTONE_BLOCK),
  SAND(Material.SAND),
  SOUL_SAND(Material.SOUL_SAND),
  SOUL_SOIL(Material.SOUL_SOIL),
  STONE(Material.STONE),
  SUGAR_CANE(Material.SUGAR_CANE),
  TUFF(Material.TUFF);

  private final Material baseMaterial;
  private final String id;
  private final String translationKey;

  CompressedMaterial(Material baseMaterial) {
    this.baseMaterial = baseMaterial;
    id = baseMaterial.getKey().getKey().toLowerCase(Locale.ROOT);
    translationKey = baseMaterial.translationKey();
  }

  /// Resolves a serialized ID into a compressed material.
  ///
  /// @param id serialized material ID
  /// @return matching material, or null when the ID is unsupported
  public static CompressedMaterial fromId(String id) {
    for (CompressedMaterial material : values()) {
      if (material.id.equals(id)) return material;
    }
    return null;
  }

  /// Resolves a Bukkit material into a supported compressed material.
  ///
  /// @param material Bukkit material
  /// @return matching compressed material, or null when unsupported
  public static CompressedMaterial fromMaterial(Material material) {
    for (CompressedMaterial compressedMaterial : values()) {
      if (compressedMaterial.baseMaterial == material) return compressedMaterial;
    }
    return null;
  }

  /// Returns the vanilla material used as the base recipe ingredient.
  ///
  /// @return base material
  public Material baseMaterial() {
    return baseMaterial;
  }

  /// Returns the stable serialized ID for PDC, recipe keys, and resource-pack files.
  ///
  /// @return serialized material ID
  public String id() {
    return id;
  }

  /// Returns the vanilla translation key used in item names.
  ///
  /// @return vanilla material translation key
  public String translationKey() {
    return translationKey;
  }
}
