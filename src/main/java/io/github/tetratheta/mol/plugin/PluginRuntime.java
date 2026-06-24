package io.github.tetratheta.mol.plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/// Groups services and Bukkit resources that live for one enabled plugin runtime.
///
/// Subclasses should create domain services in their constructor, register listeners and scheduled tasks through this class, and override
/// `terminate()` when they need to save or close domain resources before Bukkit resources are released.
///
/// Example:
/// ```java
/// public final class ExampleRuntime extends PluginRuntime {
///   private final ExampleService service;
///
///   public ExampleRuntime(ExamplePlugin plugin) {
///     super(plugin);
///     service = new ExampleService(plugin.getConfig());
///     registerListener(new ExampleListener(service));
///   }
///
///   @Override
///   public void terminate() {
///     service.save();
///     super.terminate();
///   }
/// }
/// ```
///
public class PluginRuntime {
  private final JavaPlugin plugin;
  private final Set<BukkitTask> tasks;
  private final Set<Listener> listeners;

  /// Creates a runtime bound to one plugin instance.
  ///
  /// @param plugin plugin that owns this runtime
  public PluginRuntime(@NotNull JavaPlugin plugin) {
    this.plugin = plugin;
    tasks = new HashSet<>();
    listeners = new HashSet<>();
  }

  /// Registers a listener and releases it when this runtime terminates.
  ///
  /// @param listener listener to register
  protected void registerListener(@NotNull Listener listener) {
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    listeners.add(listener);
  }

  /// Tracks a scheduled task and cancels it when this runtime terminates.
  ///
  /// @param task task to track
  protected void registerTask(@NotNull BukkitTask task) {
    tasks.add(task);
  }

  /// Schedules a synchronous Bukkit task owned by this runtime.
  ///
  /// Completed tasks are removed from the runtime task set, while pending tasks are still cancelled if this runtime terminates before Bukkit runs
  /// them.
  ///
  /// @param runnable task to run on the next server tick
  protected void runTask(@NotNull Runnable runnable) {
    AtomicReference<BukkitTask> taskReference = new AtomicReference<>();
    BukkitTask task = plugin.getServer().getScheduler().runTask(
      plugin, () -> {
        try {
          runnable.run();
        } finally {
          tasks.remove(taskReference.get());
        }
      }
    );
    taskReference.set(task);
    registerTask(task);
  }

  /// Releases all Bukkit resources owned by this runtime.
  ///
  /// Subclasses that override this method should release domain resources first and then call `super.terminate()`.
  public void terminate() {
    for (Listener listener : listeners)
      HandlerList.unregisterAll(listener);
    listeners.clear();
    for (BukkitTask task : tasks)
      task.cancel();
    tasks.clear();
  }
}
