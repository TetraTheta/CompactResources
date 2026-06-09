package io.github.tetratheta.compactresources.listener;

import io.github.tetratheta.compactresources.service.CompressedBlockService;
import java.util.function.Consumer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/// Makes Java-registered compression recipes visible in the player's recipe book.
public class CREventRecipeDiscovery implements Listener {
  private final CompressedBlockService compressedBlockService;
  private final Consumer<Runnable> nextTickScheduler;

  /// Creates a recipe discovery listener.
  ///
  /// @param compressedBlockService service that owns compression recipe keys
  /// @param nextTickScheduler runtime-owned scheduler for delayed recipe discovery
  public CREventRecipeDiscovery(
      CompressedBlockService compressedBlockService, Consumer<Runnable> nextTickScheduler) {
    this.compressedBlockService = compressedBlockService;
    this.nextTickScheduler = nextTickScheduler;
  }

  /// Discovers compression recipes for joining players.
  ///
  /// @param e player join event
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent e) {
    nextTickScheduler.accept(
        () -> e.getPlayer().discoverRecipes(compressedBlockService.getRecipeKeys()));
  }
}
