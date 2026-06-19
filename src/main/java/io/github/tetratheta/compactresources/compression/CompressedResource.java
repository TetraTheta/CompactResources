package io.github.tetratheta.compactresources.compression;

/// Stores decoded compressed resource identity.
///
/// @param material compressed base material
/// @param level compression level
public record CompressedResource(CompressedMaterial material, CompressionLevel level) {}
