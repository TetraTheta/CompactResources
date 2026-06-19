package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.service.CompressionService;
import java.util.function.Consumer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/// Makes Java-registered compression recipes visible in the player's recipe book.
public class CREventRecipeDiscovery implements Listener {
  private final CompressionService compressionService;
  private final Consumer<Runnable> nextTickScheduler;

  /// Creates a recipe discovery listener.
  ///
  /// @param compressionService service that owns compression recipe keys
  /// @param nextTickScheduler runtime-owned scheduler for delayed recipe discovery
  public CREventRecipeDiscovery(
      CompressionService compressionService, Consumer<Runnable> nextTickScheduler) {
    this.compressionService = compressionService;
    this.nextTickScheduler = nextTickScheduler;
  }

  /// Discovers compression recipes for joining players.
  ///
  /// @param e player join event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent e) {
    nextTickScheduler.accept(
        () -> e.getPlayer().discoverRecipes(compressionService.getRecipeKeys()));
  }
}
