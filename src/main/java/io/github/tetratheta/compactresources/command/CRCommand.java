package io.github.tetratheta.compactresources.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.tetratheta.compactresources.CompactResources;
import io.github.tetratheta.compactresources.command.common.CRSubCommand;
import io.github.tetratheta.compactresources.command.sub.CompactCommand;
import io.github.tetratheta.compactresources.command.sub.ConfigCommand;
import io.github.tetratheta.compactresources.command.sub.IgnoreCommand;
import io.github.tetratheta.compactresources.command.sub.ReloadCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;

/// Builds the Brigadier command tree for CompactResources commands.
public class CRCommand {
  private final CompactResources plugin;

  /// Creates a command builder bound to the plugin entry point.
  ///
  /// @param plugin plugin entry point used to access active services
  public CRCommand(CompactResources plugin) {
    this.plugin = plugin;
  }

  /// Builds the root /cr command with all subcommands.
  ///
  /// @return Brigadier command node registered during the command lifecycle event
  public LiteralCommandNode<CommandSourceStack> getCommand() {
    LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cr");
    for (CRSubCommand subCommand : getSubCommands()) root.then(subCommand.getCommand());
    return root.build();
  }

  /// Creates root-level subcommands in registration order.
  ///
  /// @return root-level subcommands
  private List<CRSubCommand> getSubCommands() {
    return List.of(new CompactCommand(plugin), new IgnoreCommand(plugin), new ReloadCommand(plugin), new ConfigCommand(plugin));
  }
}
