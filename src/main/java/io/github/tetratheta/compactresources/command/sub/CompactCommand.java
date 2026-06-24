package io.github.tetratheta.compactresources.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.tetratheta.compactresources.CompactResources;
import io.github.tetratheta.compactresources.command.common.CRSubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/// Builds and executes the /cr compact subcommand.
public class CompactCommand implements CRSubCommand {
  private static final String PERMISSION_COMPACT = "compactresources.compact";
  private final CompactResources plugin;

  /// Creates the compact subcommand.
  ///
  /// @param plugin plugin entry point used to access active services
  public CompactCommand(CompactResources plugin) {
    this.plugin = plugin;
  }

  /// Builds the compact command tree.
  ///
  /// @return compact subcommand builder
  @Override
  public LiteralArgumentBuilder<CommandSourceStack> getCommand() {
    return Commands.literal("compact").requires(ctx -> ctx.getSender().hasPermission(PERMISSION_COMPACT)).executes(ctx -> {
      CommandSender sender = ctx.getSource().getSender();
      Entity executor = ctx.getSource().getExecutor();
      if (!(executor instanceof Player player)) {
        plugin.getRuntime().getMessageService().send(sender, "command.compact.only-player");
        return Command.SINGLE_SUCCESS;
      }
      boolean compacted = plugin.getRuntime().getCompactService().compactInventory(player);
      if (compacted) plugin.getRuntime().getMessageService().send(sender, "command.compact.success");
      else plugin.getRuntime().getMessageService().send(sender, "command.compact.empty");
      return Command.SINGLE_SUCCESS;
    });
  }
}
