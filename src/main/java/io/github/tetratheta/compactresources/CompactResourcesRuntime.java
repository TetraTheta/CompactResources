package io.github.tetratheta.compactresources;

import io.github.tetratheta.compactresources.config.CRConfig;
import io.github.tetratheta.compactresources.listener.CREventCompressedBlockMigration;
import io.github.tetratheta.compactresources.listener.CREventCompression;
import io.github.tetratheta.compactresources.listener.CREventMaxStack;
import io.github.tetratheta.compactresources.listener.CREventRecipeDiscovery;
import io.github.tetratheta.compactresources.listener.CREventResourcePack;
import io.github.tetratheta.compactresources.service.CompactService;
import io.github.tetratheta.compactresources.service.CompressedBlockService;
import io.github.tetratheta.compactresources.service.ResourcePackService;
import io.github.tetratheta.compactresources.service.StackSizeService;
import io.github.tetratheta.mol.message.MessageService;
import io.github.tetratheta.mol.plugin.PluginRuntime;

/// Wires configuration-backed services and Bukkit resources for one plugin runtime.
public class CompactResourcesRuntime extends PluginRuntime {
  private final CompactService compactService;
  private final CompressedBlockService compressedBlockService;
  private final CRConfig config;
  private final MessageService messageService;

  /// Creates all services from the current disk configuration and registers runtime listeners.
  ///
  /// @param plugin plugin entry point that owns this runtime
  public CompactResourcesRuntime(CompactResources plugin) {
    super(plugin);
    config = new CRConfig(plugin);
    messageService = new MessageService(plugin, config.getLanguage());
    if (config.validateAndFix(messageService)) config.saveConfig();

    StackSizeService stackSizeService =
        new StackSizeService(config.loadStackSizeRules(), messageService);
    compressedBlockService =
        config.isCompressionEnabled() ? new CompressedBlockService(plugin, stackSizeService) : null;
    if (compressedBlockService != null) compressedBlockService.registerRecipes();

    ResourcePackService resourcePackService = new ResourcePackService(config, messageService);
    compactService = new CompactService(plugin, stackSizeService, compressedBlockService);
    registerListener(new CREventMaxStack(stackSizeService, this::runTask));
    registerListener(new CREventResourcePack(resourcePackService, this::runTask));
    if (compressedBlockService != null) {
      registerListener(new CREventCompressedBlockMigration(plugin, this::runTask));
      registerListener(new CREventCompression(compressedBlockService));
      registerListener(new CREventRecipeDiscovery(compressedBlockService, this::runTask));
    }
  }

  /// Returns the service used by the compact command.
  ///
  /// @return inventory compacting service
  public CompactService getCompactService() {
    return compactService;
  }

  /// Returns the active configuration facade.
  ///
  /// @return active configuration facade
  public CRConfig getConfig() {
    return config;
  }

  /// Returns the active localized message service.
  ///
  /// @return localized message service
  public MessageService getMessageService() {
    return messageService;
  }

  /// Unregisters runtime Bukkit resources.
  @Override
  public void terminate() {
    if (compressedBlockService != null) compressedBlockService.unregisterRecipes();
    super.terminate();
  }
}
