package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.service.ResourcePackService;
import java.util.function.Consumer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/// Sends configured server resource packs after players join the world.
public class CREventResourcePack implements Listener {
  private final Consumer<Runnable> nextTickScheduler;
  private final ResourcePackService resourcePackService;

  /// Creates a resource pack listener.
  ///
  /// @param resourcePackService service used to send resource pack requests
  /// @param nextTickScheduler   runtime-owned scheduler for delayed resource pack delivery
  public CREventResourcePack(ResourcePackService resourcePackService, Consumer<Runnable> nextTickScheduler) {
    this.nextTickScheduler = nextTickScheduler;
    this.resourcePackService = resourcePackService;
  }

  /// Sends the configured resource pack shortly after the player joins.
  ///
  /// @param e player join event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent e) {
    nextTickScheduler.accept(() -> resourcePackService.sendTo(e.getPlayer()));
  }
}
