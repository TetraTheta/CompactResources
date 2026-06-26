# v2.2.0 (Unreleased)

- Added max stack size exceptions.
  - Items named `cr_ignore` are skipped case-insensitively, so players can create exceptions through anvils.
  - Added `/cr ignore get|set|unset|toggle` for command-managed exceptions.
  - CompactResources now removes only max stack size metadata that it owns, preserving values from other plugins, datapacks, or commands.

# v2.1.0

- CompactResources now supports compacting items as well as blocks.
- Added these supported compressed resources:
  - `arrow`
  - `beetroot`
  - `bone_block`
  - `carrot`
  - `hay_block`
  - `magma_cream`
  - `potato`
  - `redstone_block`
  - `sugar_cane`

# v2.0.0

- Added more blocks to `Compressed Block`.
- [BREAKING CHANGE] Changed how `Compressed Block` is defined internally; automatic migration from v1 is enabled.

# v1.1.0

- Added the `Compressed Block` feature.

# v1.0.0

- Initial release.
