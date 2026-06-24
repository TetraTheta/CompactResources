package io.github.tetratheta.compactresources.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.tetratheta.compactresources.CompactResources;
import io.github.tetratheta.compactresources.command.common.CRSubCommand;
import io.github.tetratheta.compactresources.service.StackSizeService;
import io.github.tetratheta.mol.message.MessageService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/// Builds and executes the /cr ignore subcommand for main-hand item stack-size exceptions.
public class IgnoreCommand implements CRSubCommand {
  private static final String PERMISSION_IGNORE = "compactresources.ignore";
  private final CompactResources plugin;

  /// Creates the ignore subcommand.
  ///
  /// @param plugin plugin entry point used to access active services
  public IgnoreCommand(CompactResources plugin) {
    this.plugin = plugin;
  }

  /// Builds the ignore command tree.
  ///
  /// @return ignore subcommand builder
  @Override
  public LiteralArgumentBuilder<CommandSourceStack> getCommand() {
    return Commands.literal("ignore").requires(ctx -> ctx.getSender().hasPermission(PERMISSION_IGNORE))
                   .then(Commands.literal("get").executes(ctx -> handleGet(ctx.getSource())))
                   .then(Commands.literal("set").executes(ctx -> handleSet(ctx.getSource(), true)))
                   .then(Commands.literal("unset").executes(ctx -> handleSet(ctx.getSource(), false)))
                   .then(Commands.literal("toggle").executes(ctx -> handleToggle(ctx.getSource())));
  }

  @SuppressWarnings("SameReturnValue")
  private int handleGet(CommandSourceStack source) {
    Player player = getPlayer(source);
    if (player == null) return Command.SINGLE_SUCCESS;
    ItemStack item = getMainHandItem(player);
    if (item == null) return Command.SINGLE_SUCCESS;
    StackSizeService stackSizeService = plugin.getRuntime().getStackSizeService();
    getMessages().send(source.getSender(), "command.ignore.status", stackSizeService.isIgnored(item), stackSizeService.isManaged(item));
    return Command.SINGLE_SUCCESS;
  }

  @SuppressWarnings("SameReturnValue")
  private int handleSet(CommandSourceStack source, boolean ignored) {
    Player player = getPlayer(source);
    if (player == null) return Command.SINGLE_SUCCESS;
    ItemStack item = getMainHandItem(player);
    if (item == null) return Command.SINGLE_SUCCESS;
    StackSizeService stackSizeService = plugin.getRuntime().getStackSizeService();
    stackSizeService.setIgnored(item, ignored);
    stackSizeService.applyCustomStackSize(item);
    getMessages().send(source.getSender(), ignored ? "command.ignore.set" : "command.ignore.unset");
    return Command.SINGLE_SUCCESS;
  }

  @SuppressWarnings("SameReturnValue")
  private int handleToggle(CommandSourceStack source) {
    Player player = getPlayer(source);
    if (player == null) return Command.SINGLE_SUCCESS;
    ItemStack item = getMainHandItem(player);
    if (item == null) return Command.SINGLE_SUCCESS;
    StackSizeService stackSizeService = plugin.getRuntime().getStackSizeService();
    boolean ignored = !stackSizeService.isIgnored(item);
    stackSizeService.setIgnored(item, ignored);
    stackSizeService.applyCustomStackSize(item);
    getMessages().send(source.getSender(), ignored ? "command.ignore.set" : "command.ignore.unset");
    return Command.SINGLE_SUCCESS;
  }

  private Player getPlayer(CommandSourceStack source) {
    Entity executor = source.getExecutor();
    if (executor instanceof Player player) return player;
    getMessages().send(source.getSender(), "command.ignore.only-player");
    return null;
  }

  private ItemStack getMainHandItem(Player player) {
    ItemStack item = player.getInventory().getItemInMainHand();
    if (item.getType() != Material.AIR) return item;
    getMessages().send(player, "command.ignore.empty-hand");
    return null;
  }

  private MessageService getMessages() {
    return plugin.getRuntime().getMessageService();
  }
}
