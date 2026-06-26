<p align="center">
<img src="https://raw.githubusercontent.com/TetraTheta/CompactResources/main/.etc/compactresources_icon.png" alt="CompactResources Logo">
</p>

# CompactResources
**CompactResources adds stack-size rules and compressed resource items to Paper servers**

![Version](https://img.shields.io/modrinth/v/compactresources?style=for-the-badge&label=Plugin%20Version) 
![Game Version](https://img.shields.io/modrinth/game-versions/compactresources?style=for-the-badge&label=Minecraft%20Version)<br>
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/compactresources?style=for-the-badge&label=Modrinth%20Downloads)](https://modrinth.com/plugin/compactresources)

[![Discord](https://img.shields.io/discord/1514516278226845726?style=for-the-badge)](https://discord.gg/eS8tCEkecp)

## Introduction

CompactResources helps players keep their inventories clean by increasing the maximum stack size of configured items, adding reversible compressed resource items, and letting specific items opt out when they must keep their vanilla or externally managed stack size.

Compressed resources are custom `minecraft:heart_of_the_sea` items that can visually appear as block-shaped models through the resource pack. For them to display correctly, clients need [CompactResourcesPack](https://modrinth.com/resourcepack/compactresourcespack), the companion resource pack developed primarily for this plugin. The plugin can send that pack automatically through Paper's server resource-pack system when `resource-pack.url` and `resource-pack.sha1` are configured.

By default, the plugin targets items that are commonly useful but awkward to store in bulk, such as stews, potions, saddles, boats, chest boats, and minecarts. Server owners can customize stack-size rules by item ID, Minecraft item tag, regular expression, or a disabled-by-default fallback rule for every item.

Players can craft supported materials into x9, x81, and x729 compressed tiers, decompress them back through crafting, or run `/cr compact` to convert eligible inventory contents automatically. Supported compressed resources include common storage-heavy blocks plus selected items such as arrows, beetroot, carrots, magma cream, potatoes, and sugar cane.

Use `/cr reload` after editing the configuration to apply changes without restarting the server.

See [CHANGELOG.md](CHANGELOG.md) for release notes.

Related projects:

- CompactResourcesPack resource pack: <https://modrinth.com/resourcepack/compactresourcespack>

## Configuration

```yml
language: 'ko'

module:
  compression:
    enabled: true
  max-stack-size:
    # The default max stack size applied to all items unless a more specific rule exists.
    # WARNING: Enabling this is risky as it applies to items with durability, causing the entire stack to share damage.
    default:
      enabled: false
      size: 64
    # Max stack sizes by Minecraft item ID.
    # The 'minecraft:' namespace can be omitted (e.g., 'suspicious_stew': 64).
    id:
      'beetroot_soup': 64
      'lingering_potion': 64
      'mushroom_stew': 64
      'potion': 64
      'rabbit_stew': 64
      'saddle': 64
      'splash_potion': 64
      'suspicious_stew': 64
    # Regular expression rules matched against Minecraft item IDs.
    # Patterns are matched against the full runtime item ID (e.g., 'minecraft:<id>').
    # Example: '^.*minecart$' matches all minecart items.
    regex:
      - pattern: '^.*minecart$'
        size: 64
    # Max stack sizes by Minecraft item tag ID.
    # The 'minecraft:' namespace can be omitted (e.g., 'boats': 64).
    tag:
      'boats': 64
      'chest_boats': 64

resource-pack:
  enabled: true
  force: true
  sha1: '34465d867fff230b649080315e466e1d2cfd72d8'
  url: 'https://cdn.modrinth.com/data/swhJ0PPM/versions/y5qpQDLi/compact-resources-pack.zip'
  uuid: '9d54b89a-1738-4307-abd8-3f7f9d8613f5'
```

`module.compression.enabled` controls compression recipes and item handling. If it is disabled, the stack-size module can still run, but compressed resource items are not registered.

`module.max-stack-size` rules are resolved in this order: exact item ID, item tag, regex, then the optional default rule. Values are clamped to the supported range `1..99`. Invalid regex rules are removed during validation, while unknown item IDs are kept with a warning so future Minecraft versions can still use them.

CompactResources skips max stack-size changes for items named `cr_ignore` case-insensitively, which lets non-op players create exceptions through anvils. Players with `compactresources.ignore` can also run `/cr ignore set` while holding an item; that permission is granted to all players by default. For command-generated items, use Paper's PDC-compatible custom data form:

```mcfunction
/give @s minecraft:stone[custom_data={PublicBukkitValues:{"compactresources:ignore":1b}}]
```

When CompactResources applies a max stack size, it also writes the internal `compactresources:managed_max_stack_size` marker. That marker is not user-facing; it only lets the plugin remove or update values it owns while preserving max stack sizes created by other plugins, datapacks, or commands.

`resource-pack.enabled` controls whether the plugin asks joining players to load the configured pack. `resource-pack.force` makes the request required. If `url` or `sha1` is blank, or if the URL/hash metadata is invalid, resource-pack delivery is skipped and compressed resources will still work mechanically but appear as Heart of the Sea items on clients without CompactResourcesPack.

## Commands

- `/cr compact`: Compacts the executing player's inventory.
- `/cr ignore get`: Shows whether the main-hand item is ignored and managed by CompactResources.
- `/cr ignore set`: Marks the main-hand item as a max stack-size exception.
- `/cr ignore unset`: Removes the command-set exception from the main-hand item.
- `/cr ignore toggle`: Toggles the command-set exception on the main-hand item.
- `/cr reload`: Reloads configuration from disk.
- `/cr config language [value]`: Views or updates the active language.
- `/cr config max-stack-size default enabled [true|false]`: Views or updates the fallback stack-size rule.
- `/cr config max-stack-size default size [1..99]`: Views or updates the fallback stack size.
- `/cr config max-stack-size id <minecraft item id> [1..99]`: Views or updates an item ID rule.
- `/cr config max-stack-size tag <minecraft tag id> [1..99]`: Views or updates an item tag rule.

## Technical Notes

Compressed resources are custom `minecraft:heart_of_the_sea` items. CompactResources stores the base material and compression tier in the item's Persistent Data Container, then assigns an item model such as `compactresources:item/compressed/cobblestone_x9`.

Item names are Adventure translatable components. A compressed cobblestone item is named as `block.minecraft.cobblestone` plus ` x9`, while a compressed arrow item uses `item.minecraft.arrow` plus ` x9`, so each client sees the base material name in its own language.

[CompactResourcesPack](https://modrinth.com/resourcepack/compactresourcespack) provides item model definitions for compressed resources. Server owners can use the URL of the resource pack on Modrinth CDN and configure `resource-pack.url` plus `resource-pack.sha1`; Paper then sends it to clients on join.

During the item model migration, CompactResources upgrades older compressed resource items that still carry legacy Custom Model Data metadata when players join or move those items.

Permissions:

- `compactresources.compact`: Allows `/cr compact`.
- `compactresources.config`: Allows viewing and editing live config values.
- `compactresources.ignore`: Allows managing max stack-size exceptions on the main-hand item.
- `compactresources.reload`: Allows `/cr reload`.
