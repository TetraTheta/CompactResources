package io.github.tetratheta.compactresources.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.tetratheta.compactresources.CompactResources;
import io.github.tetratheta.compactresources.command.common.CRSubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

/// Builds and executes the /cr reload subcommand.
public class ReloadCommand implements CRSubCommand {
  private static final String PERMISSION_RELOAD = "compactresources.reload";

  private final CompactResources plugin;

  /// Creates the reload subcommand.
  ///
  /// @param plugin plugin entry point used to reload runtime services
  public ReloadCommand(CompactResources plugin) {
    this.plugin = plugin;
  }

  /// Builds the reload command tree.
  ///
  /// @return reload subcommand builder
  @Override
  public LiteralArgumentBuilder<CommandSourceStack> getCommand() {
    return Commands.literal("reload")
        .requires(ctx -> ctx.getSender().hasPermission(PERMISSION_RELOAD))
        .executes(
            ctx -> {
              CommandSender sender = ctx.getSource().getSender();
              plugin.reloadRuntime();
              plugin.getRuntime().getMessageService().send(sender, "command.reload.success");
              return Command.SINGLE_SUCCESS;
            });
  }
}
