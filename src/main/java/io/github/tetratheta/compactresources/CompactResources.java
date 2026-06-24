package io.github.tetratheta.compactresources;

import io.github.tetratheta.compactresources.command.CRCommand;
import io.github.tetratheta.mol.plugin.BasePlugin;

/// Bootstraps the CompactResources plugin and owns the active runtime.
public final class CompactResources extends BasePlugin<CompactResourcesRuntime> {
  /// Creates the services and Bukkit resources for the current plugin configuration.
  ///
  /// @return new CompactResources runtime
  @Override
  protected CompactResourcesRuntime createRuntime() {
    return new CompactResourcesRuntime(this);
  }

  /// Registers commands after the initial runtime is available.
  @Override
  protected void onPluginEnabled() {
    registerCommand(() -> new CRCommand(this).getCommand());
  }
}
