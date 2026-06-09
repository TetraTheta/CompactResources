<div style="text-align: center">
<img src="https://raw.githubusercontent.com/TetraTheta/CompactResources/main/.etc/compactresources_icon.png" alt="CompactResources Logo">
</div>

# CompactResources
**CompactResources makes low-stack resources easier to carry on Paper servers**

![Version](https://img.shields.io/modrinth/v/compactresources?style=for-the-badge&label=Plugin%20Version) 
![Game Version](https://img.shields.io/modrinth/game-versions/compactresources?style=for-the-badge&label=Minecraft%20Version)  
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/compactresources?style=for-the-badge&label=Modrinth%20Downloads)](https://modrinth.com/plugin/compactresources)

## Introduction

CompactResources helps players keep their inventories clean by increasing the maximum stack size of configured items and compacting reversible resource recipes directly from their inventory.

By default, the plugin targets items that are commonly useful but awkward to store in bulk, such as stews, potions, saddles, boats, chest boats, and minecarts. Server owners can customize stack-size rules by item ID, Minecraft item tag, regular expression, or a disabled-by-default fallback rule for every item.

Players can run `/cr compact` to convert eligible items into their compacted form when the server has a matching 2x2 or 3x3 recipe and a recipe that can turn the result back into the original items. For example, compatible resources can be compacted without permanently losing their decompacting path.

Use `/cr reload` after editing the configuration to apply changes without restarting the server.

## Configuration

```yml
language: 'ko'

module:
  compression:
    enabled: true
  max-stack-size:
    default:
      enabled: false
      size: 64
    id:
      potion: 64
      saddle: 64
    tag:
      boats: 64
    regex:
      - pattern: '^.*minecart$'
        size: 64

resource-pack:
  enabled: true
  force: true
  url: ''
  sha1: ''
  uuid: '9d54b89a-1738-4307-abd8-3f7f9d8613f5'
```

## Commands

- `/cr compact`: Compacts the executing player's inventory.
- `/cr reload`: Reloads configuration from disk.
- `/cr config language [value]`: Views or updates the active language.
- `/cr config max-stack-size default enabled [true|false]`: Views or updates the fallback stack-size rule.
- `/cr config max-stack-size default size [1..99]`: Views or updates the fallback stack size.
- `/cr config max-stack-size id <minecraft item id> [1..99]`: Views or updates an item ID rule.
- `/cr config max-stack-size tag <minecraft tag id> [1..99]`: Views or updates an item tag rule.

## Technical Notes

Compressed blocks are custom `minecraft:heart_of_the_sea` items. CompactResources stores the base material and compression tier in the item's Persistent Data Container, then assigns a Custom Model Data string such as `compactresources:cobblestone/x9`.

Item names are Adventure translatable components. A compressed cobblestone item is named as `block.minecraft.cobblestone` plus ` x9`, so each client sees the block name in its own language.

The resource pack overrides the Heart of the Sea item definition and dispatches by Custom Model Data string. Server owners can host the ZIP on Modrinth CDN and configure `resource-pack.url` plus `resource-pack.sha1`; Paper then sends it to clients on join.

Permissions:

- `compactresources.compact`: Allows `/cr compact`.
- `compactresources.config`: Allows viewing and editing live config values.
- `compactresources.reload`: Allows `/cr reload`.
