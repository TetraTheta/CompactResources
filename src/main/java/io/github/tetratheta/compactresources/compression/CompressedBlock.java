package io.github.tetratheta.compactresources.compression;

/// Stores decoded compressed block identity.
///
/// @param material compressed base material
/// @param level compression level
public record CompressedBlock(CompressedMaterial material, CompressionLevel level) {}
