package io.github.tetratheta.compactresources.config;

import io.github.tetratheta.mol.config.BaseConfig;
import io.github.tetratheta.mol.message.MessageService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/// Loads, normalizes, and persists CompactResources configuration values.
public class CRConfig extends BaseConfig {
  private static final String PATH_COMPRESSION_ENABLED = "module.compression.enabled";
  private static final String PATH_DEFAULT_ENABLED = "module.max-stack-size.default.enabled";
  private static final String PATH_DEFAULT_SIZE = "module.max-stack-size.default.size";
  private static final String PATH_ID_RULES = "module.max-stack-size.id";
  private static final String PATH_TAG_RULES = "module.max-stack-size.tag";
  private static final String PATH_REGEX_RULES = "module.max-stack-size.regex";
  private static final String PATH_RESOURCE_PACK_ENABLED = "resource-pack.enabled";
  private static final String PATH_RESOURCE_PACK_FORCE = "resource-pack.force";
  private static final String PATH_RESOURCE_PACK_URL = "resource-pack.url";
  private static final String PATH_RESOURCE_PACK_SHA1 = "resource-pack.sha1";
  private static final String PATH_RESOURCE_PACK_UUID = "resource-pack.uuid";
  private static final UUID DEFAULT_RESOURCE_PACK_UUID =
      UUID.fromString("9d54b89a-1738-4307-abd8-3f7f9d8613f5");

  /// Creates a configuration facade bound to the provided plugin instance.
  ///
  /// @param provided plugin instance that owns the Bukkit configuration
  public CRConfig(JavaPlugin provided) {
    super(provided);
  }

  /// Returns the configured language code.
  ///
  /// @return configured language code
  public String getLanguage() {
    return getString("language", MessageService.defaultLanguage());
  }

  /// Updates the configured language code.
  ///
  /// @param language language code to store
  public void setLanguage(String language) {
    getConfig().set("language", language.strip());
  }

  /// Returns whether the compression module is enabled.
  ///
  /// @return true when custom compressed blocks should be registered
  public boolean isCompressionEnabled() {
    return getConfig().getBoolean(PATH_COMPRESSION_ENABLED, true);
  }

  /// Returns whether server resource pack delivery is enabled.
  ///
  /// @return true when players should receive the configured resource pack
  public boolean isResourcePackEnabled() {
    return getConfig().getBoolean(PATH_RESOURCE_PACK_ENABLED, true);
  }

  /// Returns whether the configured resource pack should be required.
  ///
  /// @return true when clients must accept the resource pack
  public boolean isResourcePackForced() {
    return getConfig().getBoolean(PATH_RESOURCE_PACK_FORCE, true);
  }

  /// Returns the configured resource pack download URL.
  ///
  /// @return resource pack URL, or an empty string when delivery is not configured
  public String getResourcePackUrl() {
    return getConfig().getString(PATH_RESOURCE_PACK_URL, "").strip();
  }

  /// Returns the configured resource pack SHA-1 hash.
  ///
  /// @return hex-encoded SHA-1 hash, or an empty string when delivery is not configured
  public String getResourcePackSha1() {
    return getConfig().getString(PATH_RESOURCE_PACK_SHA1, "").strip().toLowerCase(Locale.ROOT);
  }

  /// Returns the configured resource pack UUID, falling back to a stable default.
  ///
  /// @return resource pack UUID
  public UUID getResourcePackUuid() {
    String configured = getConfig().getString(PATH_RESOURCE_PACK_UUID, "").strip();
    try {
      return configured.isBlank() ? DEFAULT_RESOURCE_PACK_UUID : UUID.fromString(configured);
    } catch (IllegalArgumentException e) {
      return DEFAULT_RESOURCE_PACK_UUID;
    }
  }

  /// Returns whether the default stack-size rule is enabled.
  ///
  /// @return true when the default stack-size rule is enabled
  public boolean isDefaultRuleEnabled() {
    return getConfig().getBoolean(PATH_DEFAULT_ENABLED, false);
  }

  /// Updates whether the default stack-size rule is enabled.
  ///
  /// @param enabled whether the default stack-size rule should apply
  public void setDefaultRuleEnabled(boolean enabled) {
    getConfig().set(PATH_DEFAULT_ENABLED, enabled);
  }

  /// Returns the default max stack size after clamping it to the supported range.
  ///
  /// @return default max stack size
  public int getDefaultMaxStackSize() {
    return normalizeStackSize(getConfig().getInt(PATH_DEFAULT_SIZE, 64));
  }

  /// Updates the default max stack size after clamping it to the supported range.
  ///
  /// @param maxStackSize configured max stack size
  public void setDefaultMaxStackSize(int maxStackSize) {
    getConfig().set(PATH_DEFAULT_SIZE, normalizeStackSize(maxStackSize));
  }

  /// Returns a max stack-size rule configured for an item ID.
  ///
  /// @param itemId item ID with optional namespace
  /// @return configured max stack size, or null when no rule exists
  public Integer getItemStackSize(String itemId) {
    return getMapStackSize(PATH_ID_RULES, itemId);
  }

  /// Updates a max stack-size rule configured for an item ID.
  ///
  /// @param itemId item ID with optional namespace
  /// @param maxStackSize configured max stack size
  public void setItemStackSize(String itemId, int maxStackSize) {
    setMapStackSize(PATH_ID_RULES, itemId, maxStackSize);
  }

  /// Returns a max stack-size rule configured for an item tag ID.
  ///
  /// @param tagId item tag ID with optional namespace
  /// @return configured max stack size, or null when no rule exists
  public Integer getTagStackSize(String tagId) {
    return getMapStackSize(PATH_TAG_RULES, tagId);
  }

  /// Updates a max stack-size rule configured for an item tag ID.
  ///
  /// @param tagId item tag ID with optional namespace
  /// @param maxStackSize configured max stack size
  public void setTagStackSize(String tagId, int maxStackSize) {
    setMapStackSize(PATH_TAG_RULES, tagId, maxStackSize);
  }

  /// Normalizes a configured Minecraft ID into the form used by map-based rules.
  ///
  /// @param id configured ID
  /// @return normalized namespaced ID
  public String normalizeConfiguredId(String id) {
    return normalizeId(id);
  }

  /// Normalizes user configuration and removes invalid regex rules before services are created.
  ///
  /// @param messageService message service used to report recoverable configuration issues
  /// @return true when configuration values were changed and should be saved
  public boolean validateAndFix(MessageService messageService) {
    boolean changed = validateDefaultRule();
    changed |= validateResourcePackSettings(messageService);
    changed |= validateMapSection(PATH_ID_RULES, true, messageService);
    changed |= validateMapSection(PATH_TAG_RULES, false, messageService);
    changed |= validateRegexRules(messageService);
    return changed;
  }

  /// Converts normalized configuration sections into immutable rule input for stack-size services.
  ///
  /// @return loaded stack-size rules
  public StackSizeRules loadStackSizeRules() {
    StackSizeRules.DefaultRule defaultRule = loadDefaultRule();
    Map<String, Integer> ids = loadMapRules(PATH_ID_RULES);
    Map<String, Integer> tags = loadMapRules(PATH_TAG_RULES);
    List<StackSizeRules.RegexRule> regexRules = loadRegexRules();
    return new StackSizeRules(defaultRule, ids, tags, regexRules);
  }

  /// Ensures the default stack-size rule exists and stores a clamped stack size.
  private boolean validateDefaultRule() {
    boolean changed = false;
    ConfigurationSection section =
        getConfig().contains("module.max-stack-size.default", true)
            ? getConfig().getConfigurationSection("module.max-stack-size.default")
            : null;
    if (section == null) {
      section = getConfig().createSection("module.max-stack-size.default");
      changed = true;
    }

    if (!getConfig().contains(PATH_DEFAULT_ENABLED, true)) {
      section.set("enabled", false);
      changed = true;
    }

    int maxStackSize = normalizeStackSize(section.getInt("size", 64));
    Object rawMaxStackSize = section.get("size");
    if (!getConfig().contains(PATH_DEFAULT_SIZE, true)
        || !(rawMaxStackSize instanceof Number number)
        || number.intValue() != maxStackSize) {
      section.set("size", maxStackSize);
      changed = true;
    }

    return changed;
  }

  /// Ensures resource-pack settings have defaults and a valid UUID string.
  ///
  /// @param messageService message service used to report recoverable configuration issues
  /// @return true when configuration values were changed and should be saved
  private boolean validateResourcePackSettings(MessageService messageService) {
    boolean changed = false;
    if (!getConfig().contains(PATH_RESOURCE_PACK_ENABLED, true)) {
      getConfig().set(PATH_RESOURCE_PACK_ENABLED, true);
      changed = true;
    }
    if (!getConfig().contains(PATH_RESOURCE_PACK_FORCE, true)) {
      getConfig().set(PATH_RESOURCE_PACK_FORCE, true);
      changed = true;
    }
    if (!getConfig().contains(PATH_RESOURCE_PACK_URL, true)) {
      getConfig().set(PATH_RESOURCE_PACK_URL, "");
      changed = true;
    }
    if (!getConfig().contains(PATH_RESOURCE_PACK_SHA1, true)) {
      getConfig().set(PATH_RESOURCE_PACK_SHA1, "");
      changed = true;
    }

    String configuredUuid = getConfig().getString(PATH_RESOURCE_PACK_UUID, "").strip();
    try {
      if (configuredUuid.isBlank()) throw new IllegalArgumentException();
      UUID.fromString(configuredUuid);
    } catch (IllegalArgumentException e) {
      messageService.logWarning("log.config.invalid-resource-pack-uuid", configuredUuid);
      getConfig().set(PATH_RESOURCE_PACK_UUID, DEFAULT_RESOURCE_PACK_UUID.toString());
      changed = true;
    }
    return changed;
  }

  /// Loads the fallback rule used when no ID, tag, or regex rule matches an item.
  ///
  /// @return normalized default rule
  private StackSizeRules.DefaultRule loadDefaultRule() {
    boolean enabled = getConfig().getBoolean(PATH_DEFAULT_ENABLED, false);
    int maxStackSize = normalizeStackSize(getConfig().getInt(PATH_DEFAULT_SIZE, 64));
    return new StackSizeRules.DefaultRule(enabled, maxStackSize);
  }

  /// Validates regex rules and writes back only rules that can be safely loaded later.
  ///
  /// @param messageService message service used to report recoverable configuration issues
  private boolean validateRegexRules(MessageService messageService) {
    if (!getConfig().contains(PATH_REGEX_RULES, true) || !getConfig().isList(PATH_REGEX_RULES))
      return false;

    List<Map<?, ?>> regexRules = getConfig().getMapList(PATH_REGEX_RULES);
    List<Map<String, Object>> fixedRules = new ArrayList<>();
    boolean changed = false;

    for (Map<?, ?> rule : regexRules) {
      Object patternValue = rule.get("pattern");
      if (!(patternValue instanceof String pattern) || pattern.isBlank()) {
        messageService.logWarning("log.config.blank-regex-pattern");
        changed = true;
        continue;
      }

      try {
        Pattern.compile(pattern);
      } catch (PatternSyntaxException e) {
        messageService.logWarning("log.config.invalid-regex-pattern", pattern);
        changed = true;
        continue;
      }

      int maxStackSize = 1;
      Object sizeValue = rule.get("size");
      if (sizeValue instanceof Number number) maxStackSize = number.intValue();
      int normalizedMaxStackSize = normalizeStackSize(maxStackSize);
      if (!(sizeValue instanceof Number number) || number.intValue() != normalizedMaxStackSize)
        changed = true;

      Map<String, Object> fixedRule = new LinkedHashMap<>();
      fixedRule.put("pattern", pattern);
      fixedRule.put("size", normalizedMaxStackSize);
      fixedRules.add(fixedRule);
    }

    if (changed) getConfig().set(PATH_REGEX_RULES, fixedRules);
    return changed;
  }

  /// Ensures a map-based configuration section exists and clamps all stack-size values.
  ///
  /// @param path section path to validate
  /// @param itemIds whether section keys should be validated as item IDs
  /// @param messageService message service used to report recoverable configuration issues
  private boolean validateMapSection(String path, boolean itemIds, MessageService messageService) {
    if (!getConfig().contains(path, true)) return false;

    ConfigurationSection section = getConfig().getConfigurationSection(path);
    if (section == null) return false;

    boolean changed = false;
    for (String key : section.getKeys(false)) {
      int normalized = normalizeStackSize(section.getInt(key, 1));
      Object rawValue = section.get(key);
      if (!(rawValue instanceof Number number) || number.intValue() != normalized) {
        section.set(key, normalized);
        changed = true;
      }

      if (itemIds && resolveMaterial(key) == null)
        messageService.logWarning("log.config.unknown-item-id", key);
    }
    return changed;
  }

  /// Loads a map-based rule section and normalizes item or tag IDs into namespaced IDs.
  ///
  /// @param path section path to load
  /// @return normalized map rules
  private Map<String, Integer> loadMapRules(String path) {
    Map<String, Integer> rules = new LinkedHashMap<>();
    if (!getConfig().contains(path, true)) return rules;

    ConfigurationSection section = getConfig().getConfigurationSection(path);
    if (section == null) return rules;

    for (String key : section.getKeys(false))
      rules.put(normalizeId(key), normalizeStackSize(section.getInt(key)));
    return rules;
  }

  /// Reads a stack-size value from a map-based rule section.
  ///
  /// @param path section path to read
  /// @param id configured item or tag ID
  /// @return configured max stack size, or null when no rule exists
  private Integer getMapStackSize(String path, String id) {
    String normalizedId = normalizeId(id);
    ConfigurationSection section = getConfig().getConfigurationSection(path);
    if (section == null) return null;

    for (String key : section.getKeys(false)) {
      if (normalizeId(key).equals(normalizedId)) return normalizeStackSize(section.getInt(key));
    }
    return null;
  }

  /// Writes a stack-size value into a map-based rule section.
  ///
  /// @param path section path to write
  /// @param id configured item or tag ID
  /// @param maxStackSize configured max stack size
  private void setMapStackSize(String path, String id, int maxStackSize) {
    String normalizedId = normalizeId(id);
    ConfigurationSection section = getConfig().getConfigurationSection(path);
    if (section == null) {
      getConfig().set(path, null);
      section = getConfig().createSection(path);
    }

    for (String key : section.getKeys(false)) {
      if (normalizeId(key).equals(normalizedId) && !key.equals(normalizedId))
        section.set(key, null);
    }
    section.set(normalizedId, normalizeStackSize(maxStackSize));
  }

  /// Loads valid regex rules from the already-normalized configuration.
  ///
  /// @return compiled regex rules
  private List<StackSizeRules.RegexRule> loadRegexRules() {
    List<StackSizeRules.RegexRule> rules = new ArrayList<>();
    if (!getConfig().contains(PATH_REGEX_RULES, true) || !getConfig().isList(PATH_REGEX_RULES))
      return rules;

    for (Map<?, ?> rule : getConfig().getMapList(PATH_REGEX_RULES)) {
      Object patternValue = rule.get("pattern");
      Object sizeValue = rule.get("size");
      if (!(patternValue instanceof String pattern) || !(sizeValue instanceof Number number))
        continue;
      rules.add(new StackSizeRules.RegexRule(Pattern.compile(pattern), number.intValue()));
    }
    return rules;
  }

  /// Clamps a configured stack size to the range accepted by item metadata.
  ///
  /// @param value configured stack size
  /// @return clamped stack size
  private int normalizeStackSize(int value) {
    return Math.clamp(value, 1, 99);
  }

  /// Converts bare Minecraft IDs into namespaced IDs and normalizes case.
  ///
  /// @param id configured ID
  /// @return normalized namespaced ID
  private String normalizeId(String id) {
    if (id.contains(":")) return id.toLowerCase(Locale.ROOT);
    return "minecraft:" + id.toLowerCase(Locale.ROOT);
  }

  /// Resolves a configured item ID to a Bukkit material only when it represents an item.
  ///
  /// @param itemId configured item ID
  /// @return matching item material, or null when the ID is invalid or not an item
  private Material resolveMaterial(String itemId) {
    NamespacedKey key = NamespacedKey.fromString(normalizeId(itemId));
    if (key == null) return null;

    Material material = Registry.MATERIAL.get(key);
    if (material == null || !material.isItem()) return null;
    return material;
  }
}
