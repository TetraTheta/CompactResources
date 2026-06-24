package io.github.tetratheta.mol.plugin;

import org.bukkit.plugin.java.JavaPlugin;

/// Base class for Paper plugins that rebuild their services as a single runtime object.
///
/// Subclasses provide the concrete runtime through `createRuntime()` and put non-runtime startup work, such as command lifecycle registration, in
/// `onPluginEnabled()`.
///
/// Example:
/// ```java
/// public final class ExamplePlugin extends BasePlugin<ExampleRuntime> {
///   &#64;Override
///   protected ExampleRuntime createRuntime() {
///     return new ExampleRuntime(this);
///   }
///   ///   @Override
///   protected void onPluginEnabled() {
///     getLogger().info("ExamplePlugin is enabled.");
///   }
/// }
/// ```
///
public abstract class BasePlugin<R extends PluginRuntime> extends JavaPlugin {
  private R runtime;

  /// Creates the runtime and then runs subclass startup hooks.
  ///
  /// Bukkit calls this method when the plugin is enabled. Subclasses should use `createRuntime()` and `onPluginEnabled()` instead of overriding this
  /// method.
  @Override
  public final void onEnable() {
    runtime = createRuntime();
    onPluginEnabled();
  }

  /// Runs subclass shutdown hooks and releases the active runtime.
  ///
  /// Bukkit calls this method when the plugin is disabled. Subclasses should use `onPluginDisabled()` and runtime `terminate()` overrides instead of
  /// overriding this method.
  @Override
  public final void onDisable() {
    onPluginDisabled();
    if (runtime != null) {
      runtime.terminate();
      runtime = null;
    }
  }

  /// Creates a new runtime from the plugin's current configuration state.
  ///
  /// `BasePlugin` calls this during enable and runtime reloads.
  ///
  /// @return new runtime
  protected abstract R createRuntime();

  /// Runs after the initial runtime is created.
  ///
  /// Use this hook for startup work that needs an active runtime, such as Paper lifecycle command registration.
  protected void onPluginEnabled() {}

  /// Runs before the active runtime is terminated.
  ///
  /// Use this hook for plugin-level shutdown work that is separate from runtime resource cleanup.
  protected void onPluginDisabled() {}

  /// Returns the active runtime, creating one only when the plugin has no live runtime.
  ///
  /// @return active runtime
  public R getRuntime() {
    if (runtime == null) runtime = createRuntime();
    return runtime;
  }

  /// Rebuilds the active runtime from the latest disk configuration.
  public void reloadRuntime() {
    if (runtime != null) runtime.terminate();
    reloadConfig();
    runtime = createRuntime();
  }
}
