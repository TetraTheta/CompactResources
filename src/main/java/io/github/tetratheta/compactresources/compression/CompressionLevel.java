package io.github.tetratheta.compactresources.compression;

/// Represents one supported compression tier.
public enum CompressionLevel {
  X9("x9", 9),
  X81("x81", 81),
  X729("x729", 729);

  private final String id;
  private final int multiplier;

  /// @param id stable configuration and PDC identifier
  /// @param multiplier amount of base materials represented by one item
  CompressionLevel(String id, int multiplier) {
    this.id = id;
    this.multiplier = multiplier;
  }

  /// Resolves a serialized ID into a compression level.
  ///
  /// @param id serialized level ID
  /// @return matching level, or null when the ID is unsupported
  public static CompressionLevel fromId(String id) {
    for (CompressionLevel level : values()) {
      if (level.id.equals(id)) return level;
    }
    return null;
  }

  /// Returns the stable serialized ID for this level.
  ///
  /// @return serialized level ID
  public String id() {
    return id;
  }

  /// Returns how many base materials one item represents.
  ///
  /// @return base material multiplier
  public int multiplier() {
    return multiplier;
  }

  /// Returns the next higher compression level.
  ///
  /// @return next level, or null when this is the highest level
  public CompressionLevel next() {
    return switch (this) {
      case X9 -> X81;
      case X81 -> X729;
      case X729 -> null;
    };
  }

  /// Returns the next lower compression level.
  ///
  /// @return previous level, or null when this level decompresses to the base material
  public CompressionLevel previous() {
    return switch (this) {
      case X9 -> null;
      case X81 -> X9;
      case X729 -> X81;
    };
  }
}
