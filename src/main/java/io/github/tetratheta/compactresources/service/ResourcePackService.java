package io.github.tetratheta.compactresources.service;

import io.github.tetratheta.compactresources.config.CRConfig;
import io.github.tetratheta.mol.message.MessageService;
import java.net.URI;
import java.net.URISyntaxException;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/// Sends the configured resource pack request to joining players.
public class ResourcePackService {
  private final boolean enabled;
  private final boolean forced;
  private final ResourcePackInfo packInfo;
  private final MessageService messageService;

  /// Creates a resource pack delivery service from current configuration values.
  ///
  /// @param config         active configuration facade
  /// @param messageService message service used for logs and prompts
  public ResourcePackService(CRConfig config, MessageService messageService) {
    this.messageService = messageService;
    enabled = config.isResourcePackEnabled();
    forced = config.isResourcePackForced();
    packInfo = buildPackInfo(config);
  }

  /// Sends the configured resource pack request to a player when delivery is configured.
  ///
  /// @param player player that should receive the resource pack
  public void sendTo(Player player) {
    if (!enabled || packInfo == null) return;
    ResourcePackRequest request = ResourcePackRequest.resourcePackRequest().packs(packInfo).replace(false).required(forced)
                                                     .prompt(Component.text(messageService.get("resource-pack.prompt"))).build();
    player.sendResourcePacks(request);
  }

  /// Builds Adventure resource pack metadata, returning null for incomplete settings.
  private ResourcePackInfo buildPackInfo(CRConfig config) {
    String url = config.getResourcePackUrl();
    String sha1 = config.getResourcePackSha1();
    if (url.isBlank() || sha1.isBlank()) return null;
    try {
      return ResourcePackInfo.resourcePackInfo().id(config.getResourcePackUuid()).uri(new URI(url)).hash(sha1).build();
    } catch (IllegalArgumentException | URISyntaxException e) {
      messageService.logWarning("log.resource-pack.invalid-config", e.getMessage());
      return null;
    }
  }
}
