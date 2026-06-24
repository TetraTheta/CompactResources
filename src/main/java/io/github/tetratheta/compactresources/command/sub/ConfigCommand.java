package io.github.tetratheta.compactresources.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.tetratheta.compactresources.CompactResources;
import io.github.tetratheta.compactresources.command.common.CRSubCommand;
import io.github.tetratheta.compactresources.config.CRConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemType;

import static org.bukkit.Registry.MATERIAL;

/// Builds and executes the '/cr config' subcommand tree.
public class ConfigCommand implements CRSubCommand {
  private static final String PERMISSION_CONFIG = "compactresources.config";
  private final CompactResources plugin;

  /// Creates the 'config' subcommand.
  ///
  /// @param plugin plugin entry point used to access and reload active configuration
  public ConfigCommand(CompactResources plugin) {
    this.plugin = plugin;
  }

  /// Builds the live configuration command tree.
  ///
  /// @return config subcommand builder
  @Override
  public LiteralArgumentBuilder<CommandSourceStack> getCommand() {
    return Commands.literal("config").requires(ctx -> ctx.getSender().hasPermission(PERMISSION_CONFIG)).then(getLanguageConfigCommand())
                   .then(getMaxStackSizeConfigCommand());
  }

  /// Builds the language configuration command tree.
  ///
  /// @return language config subcommand tree
  private LiteralArgumentBuilder<CommandSourceStack> getLanguageConfigCommand() {
    return Commands.literal("language").executes(ctx -> viewConfig(ctx, "language", getConfig().getLanguage())).then(
      Commands.argument("value", StringArgumentType.word()).executes(ctx -> setConfig(
        ctx, "language", StringArgumentType.getString(ctx, "value"), config -> config.setLanguage(StringArgumentType.getString(ctx, "value")))));
  }

  /// Builds the live item configuration command tree.
  ///
  /// @return item config subcommand tree
  private LiteralArgumentBuilder<CommandSourceStack> getMaxStackSizeConfigCommand() {
    LiteralArgumentBuilder<CommandSourceStack> enable = Commands.literal("enabled").executes(
                                                                  ctx -> viewConfig(ctx, "module.max-stack-size.default.enabled",
                                                                    getConfig().isDefaultRuleEnabled()))
                                                                .then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> setConfig(
                                                                  ctx, "module.max-stack-size.default.enabled",
                                                                  BoolArgumentType.getBool(ctx, "value"),
                                                                  config -> config.setDefaultRuleEnabled(BoolArgumentType.getBool(ctx, "value"))
                                                                )));
    LiteralArgumentBuilder<CommandSourceStack> maxStackSize = Commands.literal("size").executes(
      ctx -> viewConfig(ctx, "module.max-stack-size.default.size", getConfig().getDefaultMaxStackSize())).then(
      Commands.argument("value", IntegerArgumentType.integer(1, 99)).executes(ctx -> setConfig(
        ctx, "module.max-stack-size.default.size", IntegerArgumentType.getInteger(ctx, "value"),
        config -> config.setDefaultMaxStackSize(IntegerArgumentType.getInteger(ctx, "value"))
      )));
    LiteralArgumentBuilder<CommandSourceStack> ids = Commands.literal("id").then(
      Commands.argument("minecraft_item_id", ArgumentTypes.resourceKey(RegistryKey.ITEM)).executes(this::viewItemRule)
              .then(Commands.argument("value", IntegerArgumentType.integer(1, 99)).executes(this::setItemRule)));
    LiteralArgumentBuilder<CommandSourceStack> tags = Commands.literal("tag").then(
      Commands.argument("minecraft_tag_id", ArgumentTypes.namespacedKey()).executes(this::viewTagRule)
              .then(Commands.argument("value", IntegerArgumentType.integer(1, 99)).executes(this::setTagRule)));
    LiteralArgumentBuilder<CommandSourceStack> defaultConfig = Commands.literal("default").then(enable).then(maxStackSize);
    return Commands.literal("max-stack-size").then(defaultConfig).then(ids).then(tags);
  }

  /// Returns the active configuration facade for the command execution.
  ///
  /// @return active configuration facade
  private CRConfig getConfig() {
    return plugin.getRuntime().getConfig();
  }

  /// Displays a live configuration value.
  ///
  /// @param ctx   command context
  /// @param path  displayed configuration path
  /// @param value configuration value, or null when unset
  /// @return command result
  @SuppressWarnings("SameReturnValue")
  private int viewConfig(CommandContext<CommandSourceStack> ctx, String path, Object value) {
    CommandSender sender = ctx.getSource().getSender();
    if (!sender.hasPermission(PERMISSION_CONFIG)) {
      plugin.getRuntime().getMessageService().send(sender, "command.config.no-permission");
      return Command.SINGLE_SUCCESS;
    }
    String displayValue = value == null ? plugin.getRuntime().getMessageService().get("command.config.unset") : String.valueOf(value);
    plugin.getRuntime().getMessageService().send(sender, "command.config.view", path, displayValue);
    return Command.SINGLE_SUCCESS;
  }

  /// Updates, saves, and reloads a live configuration value.
  ///
  /// @param ctx    command context
  /// @param path   displayed configuration path
  /// @param value  stored configuration value
  /// @param writer configuration writer
  /// @return command result
  @SuppressWarnings("SameReturnValue")
  private int setConfig(CommandContext<CommandSourceStack> ctx, String path, Object value, ConfigWriter writer) {
    CommandSender sender = ctx.getSource().getSender();
    if (!sender.hasPermission(PERMISSION_CONFIG)) {
      plugin.getRuntime().getMessageService().send(sender, "command.config.no-permission");
      return Command.SINGLE_SUCCESS;
    }
    CRConfig config = plugin.getRuntime().getConfig();
    writer.write(config);
    config.saveConfig();
    plugin.reloadRuntime();
    plugin.getRuntime().getMessageService().send(sender, "command.config.set", path, value);
    return Command.SINGLE_SUCCESS;
  }

  /// Displays an item ID stack-size rule.
  ///
  /// @param ctx command context
  /// @return command result
  private int viewItemRule(CommandContext<CommandSourceStack> ctx) {
    String itemId = getItemId(ctx);
    CRConfig config = getConfig();
    return viewConfig(ctx, "module.max-stack-size.id." + config.normalizeConfiguredId(itemId), config.getItemStackSize(itemId));
  }

  /// Updates an item ID stack-size rule.
  ///
  /// @param ctx command context
  /// @return command result
  private int setItemRule(CommandContext<CommandSourceStack> ctx) {
    String itemId = getItemId(ctx);
    if (!isKnownItem(itemId)) {
      plugin.getRuntime().getMessageService().send(ctx.getSource().getSender(), "command.config.invalid-item-id", itemId);
      return Command.SINGLE_SUCCESS;
    }
    int value = IntegerArgumentType.getInteger(ctx, "value");
    String path = "module.max-stack-size.id." + getConfig().normalizeConfiguredId(itemId);
    return setConfig(ctx, path, value, config -> config.setItemStackSize(itemId, value));
  }

  /// Displays an item tag stack-size rule.
  ///
  /// @param ctx command context
  /// @return command result
  private int viewTagRule(CommandContext<CommandSourceStack> ctx) {
    String tagId = getTagId(ctx);
    CRConfig config = getConfig();
    return viewConfig(ctx, "module.max-stack-size.tag." + config.normalizeConfiguredId(tagId), config.getTagStackSize(tagId));
  }

  /// Updates an item tag stack-size rule.
  ///
  /// @param ctx command context
  /// @return command result
  private int setTagRule(CommandContext<CommandSourceStack> ctx) {
    String tagId = getTagId(ctx);
    if (!isKnownItemTag(tagId)) {
      plugin.getRuntime().getMessageService().send(ctx.getSource().getSender(), "command.config.invalid-tag-id", tagId);
      return Command.SINGLE_SUCCESS;
    }
    int value = IntegerArgumentType.getInteger(ctx, "value");
    String path = "module.max-stack-size.tag." + getConfig().normalizeConfiguredId(tagId);
    return setConfig(ctx, path, value, config -> config.setTagStackSize(tagId, value));
  }

  /// Returns the parsed item registry key as a namespaced ID.
  ///
  /// @param ctx command context
  /// @return namespaced item ID
  private String getItemId(CommandContext<CommandSourceStack> ctx) {
    @SuppressWarnings("unchecked") TypedKey<ItemType> itemKey = (TypedKey<ItemType>) ctx.getArgument("minecraft_item_id", TypedKey.class);
    return itemKey.key().asString();
  }

  /// Returns the parsed tag key as a namespaced ID.
  ///
  /// @param ctx command context
  /// @return namespaced tag ID
  private String getTagId(CommandContext<CommandSourceStack> ctx) {
    return ctx.getArgument("minecraft_tag_id", NamespacedKey.class).asString();
  }

  /// Returns whether the provided key resolves to a known item material.
  ///
  /// @param itemId namespaced item ID
  /// @return true when the item exists
  private boolean isKnownItem(String itemId) {
    NamespacedKey key = NamespacedKey.fromString(itemId);
    Material material = key == null ? null : MATERIAL.get(key);
    return material != null && material.isItem();
  }

  /// Returns whether the provided key resolves to a known item tag.
  ///
  /// @param tagId namespaced tag ID
  /// @return true when the item tag exists
  private boolean isKnownItemTag(String tagId) {
    NamespacedKey key = NamespacedKey.fromString(tagId);
    return key != null && Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class) != null;
  }

  /// Writes a value into the live configuration facade.
  private interface ConfigWriter {
    /// Writes the value into the provided configuration.
    ///
    /// @param config active configuration facade
    void write(CRConfig config);
  }
}
