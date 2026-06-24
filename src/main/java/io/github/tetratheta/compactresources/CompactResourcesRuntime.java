package io.github.tetratheta.compactresources;

import io.github.tetratheta.compactresources.config.CRConfig;
import io.github.tetratheta.compactresources.listener.CREventCompression;
import io.github.tetratheta.compactresources.listener.CREventCompressionMigration;
import io.github.tetratheta.compactresources.listener.CREventMaxStack;
import io.github.tetratheta.compactresources.listener.CREventRecipeDiscovery;
import io.github.tetratheta.compactresources.listener.CREventResourcePack;
import io.github.tetratheta.compactresources.service.CompactService;
import io.github.tetratheta.compactresources.service.CompressionService;
import io.github.tetratheta.compactresources.service.ResourcePackService;
import io.github.tetratheta.compactresources.service.StackSizeService;
import io.github.tetratheta.mol.message.MessageService;
import io.github.tetratheta.mol.plugin.PluginRuntime;

/// Wires configuration-backed services and Bukkit resources for one plugin runtime.
public class CompactResourcesRuntime extends PluginRuntime {
  private final CompactService compactService;
  private final CompressionService compressionService;
  private final CRConfig config;
  private final MessageService messageService;
  private final StackSizeService stackSizeService;

  /// Creates all services from the current disk configuration and registers runtime listeners.
  ///
  /// @param plugin plugin entry point that owns this runtime
  public CompactResourcesRuntime(CompactResources plugin) {
    super(plugin);
    config = new CRConfig(plugin);
    messageService = new MessageService(plugin, config.getLanguage());
    if (config.validateAndFix(messageService)) config.saveConfig();
    stackSizeService = new StackSizeService(config.loadStackSizeRules(), messageService);
    compressionService = config.isCompressionEnabled() ? new CompressionService(plugin, stackSizeService) : null;
    if (compressionService != null) compressionService.registerRecipes();
    ResourcePackService resourcePackService = new ResourcePackService(config, messageService);
    compactService = new CompactService(plugin, stackSizeService, compressionService);
    registerListener(new CREventMaxStack(stackSizeService, this::runTask));
    registerListener(new CREventResourcePack(resourcePackService, this::runTask));
    if (compressionService != null) {
      registerListener(new CREventCompressionMigration(plugin, this::runTask));
      registerListener(new CREventCompression(compressionService));
      registerListener(new CREventRecipeDiscovery(compressionService, this::runTask));
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

  /// Returns the active stack-size service.
  ///
  /// @return stack-size service
  public StackSizeService getStackSizeService() {
    return stackSizeService;
  }

  /// Unregisters runtime Bukkit resources.
  @Override
  public void terminate() {
    if (compressionService != null) compressionService.unregisterRecipes();
    super.terminate();
  }
}
