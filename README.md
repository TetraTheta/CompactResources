<div style="text-align: center">
<img src="https://raw.githubusercontent.com/TetraTheta/CompactResources/main/.etc/compactresources_info.png" style="width: 100%" alt="CompactResources Logo">
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
