package io.github.tetratheta.compactresources;

import io.github.tetratheta.compactresources.config.CRConfig;
import io.github.tetratheta.compactresources.listener.CREventListener;
import io.github.tetratheta.compactresources.service.CompactService;
import io.github.tetratheta.compactresources.service.StackSizeService;
import io.github.tetratheta.mol.message.MessageService;
import io.github.tetratheta.mol.plugin.PluginRuntime;

/// Wires configuration-backed services and Bukkit resources for one plugin runtime.
public class CompactResourcesRuntime extends PluginRuntime {
  private final CompactService compactService;
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
    compactService = new CompactService(plugin, stackSizeService);
    registerListener(new CREventListener(stackSizeService, this::runTask));
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
    super.terminate();
  }
}
