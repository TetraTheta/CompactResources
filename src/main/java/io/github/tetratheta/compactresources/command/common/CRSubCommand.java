package io.github.tetratheta.compactresources.command.common;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/// Defines one CompactResources subcommand tree.
public interface CRSubCommand {
  /// Builds the Brigadier node for this subcommand.
  ///
  /// @return subcommand node builder
  LiteralArgumentBuilder<CommandSourceStack> getCommand();
}
