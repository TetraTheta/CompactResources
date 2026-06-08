package io.github.tetratheta.mol.message;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/// Loads localized plugin messages and formats `{0}` style positional arguments.
public class MessageService {
  private static final String DEFAULT_LANGUAGE = "ko";
  private static final String FALLBACK_LANGUAGE = "en";

  private final String defaultLanguage;
  private final String fallbackLanguage;
  private final String activeLanguage;
  private final List<String> bundledLanguages;
  private final Map<String, FileConfiguration> bundledLanguageCache;
  private final Map<String, FileConfiguration> languageCache;
  private final Map<String, MessageFormat> messageFormatCache;
  private final Set<String> missingLanguages;
  private final JavaPlugin plugin;

  /// Creates a message service for a configured language with English fallback messages.
  ///
  /// @param plugin plugin instance used to load bundled language resources
  /// @param language requested language code
  public MessageService(JavaPlugin plugin, String language) {
    this(
        plugin,
        language,
        DEFAULT_LANGUAGE,
        FALLBACK_LANGUAGE,
        List.of("languages/en.yml", "languages/ko.yml"));
  }

  /// Creates a message service with caller-defined bundled language resources.
  ///
  /// @param plugin plugin instance used to load bundled language resources
  /// @param language requested language code
  /// @param defaultLanguage language used when the requested language is blank
  /// @param fallbackLanguage language used when a message path is missing
  /// @param bundledLanguages resource paths copied to the plugin data folder
  public MessageService(
      JavaPlugin plugin,
      String language,
      String defaultLanguage,
      String fallbackLanguage,
      List<String> bundledLanguages) {
    this.plugin = plugin;
    this.defaultLanguage = normalizeLanguage(defaultLanguage, DEFAULT_LANGUAGE);
    this.fallbackLanguage = normalizeLanguage(fallbackLanguage, FALLBACK_LANGUAGE);
    activeLanguage = normalizeLanguage(language);
    this.bundledLanguages = List.copyOf(bundledLanguages);
    bundledLanguageCache = new ConcurrentHashMap<>();
    languageCache = new ConcurrentHashMap<>();
    messageFormatCache = new ConcurrentHashMap<>();
    missingLanguages = ConcurrentHashMap.newKeySet();
    copyDefaultLanguages(plugin);
  }

  /// Returns the language used when configuration does not specify one.
  ///
  /// @return default language code
  public static String defaultLanguage() {
    return DEFAULT_LANGUAGE;
  }

  /// Returns a localized message, optionally formatted with `{0}` style positional arguments.
  ///
  /// @param path message path
  /// @param arguments positional values consumed by `MessageFormat`
  /// @return localized message, fallback message, or the path itself when no message exists
  public String get(String path, Object... arguments) {
    return formatMessage(activeLanguage, getMessage(activeLanguage, path), path, arguments);
  }

  /// Sends a localized message to a command sender.
  ///
  /// @param sender recipient
  /// @param path message path
  /// @param arguments positional values consumed by `MessageFormat`
  public void send(CommandSender sender, String path, Object... arguments) {
    sender.sendMessage(get(path, arguments));
  }

  /// Logs a localized warning message.
  ///
  /// @param path message path
  /// @param arguments positional values consumed by `MessageFormat`
  public void logWarning(String path, Object... arguments) {
    plugin.getLogger().warning(get(path, arguments));
  }

  /// Logs a localized info message.
  ///
  /// @param path message path
  /// @param arguments positional values consumed by `MessageFormat`
  public void logInfo(String path, Object... arguments) {
    plugin.getLogger().info(get(path, arguments));
  }

  /// Resolves a message from the requested language, its base language, then the fallback language.
  ///
  /// @param language normalized requested language code
  /// @param path message path
  /// @return localized message or path when no language contains it
  private String getMessage(String language, String path) {
    String message = getLanguageMessage(language, path);
    if (message != null) return message;

    String baseLanguage = getBaseLanguage(language);
    if (!baseLanguage.equals(language)) {
      message = getLanguageMessage(baseLanguage, path);
      if (message != null) return message;
    }

    message = getLanguageMessage(fallbackLanguage, path);
    if (message != null) return message;

    return path;
  }

  /// Resolves a message from the editable language file, then the bundled default resource.
  ///
  /// @param language normalized language code
  /// @param path message path
  /// @return localized message, or null when neither source contains it
  private String getLanguageMessage(String language, String path) {
    FileConfiguration languageMessages = loadLanguage(plugin, language);
    String message = languageMessages.getString(path);
    if (message != null) return message;

    FileConfiguration bundledMessages = loadBundledLanguage(language);
    return bundledMessages.getString(path);
  }

  /// Applies cached `MessageFormat` formatting while keeping broken format strings visible.
  ///
  /// @param language normalized language code used as part of the cache key
  /// @param message message pattern
  /// @param path message path used for diagnostics
  /// @param arguments positional values consumed by `MessageFormat`
  /// @return formatted message or the raw message when the pattern is invalid
  private String formatMessage(String language, String message, String path, Object... arguments) {
    if (arguments.length == 0) return message.replace("''", "'");

    String cacheKey = language + "\u0000" + message;
    MessageFormat messageFormat =
        messageFormatCache.computeIfAbsent(cacheKey, _ -> createMessageFormat(message, path));
    return messageFormat.format(arguments).replace('\u00a0', ' ');
  }

  /// Creates a `MessageFormat` for one translation pattern.
  ///
  /// @param message message pattern
  /// @param path message path used for diagnostics
  /// @return compiled message format
  private MessageFormat createMessageFormat(String message, String path) {
    try {
      return new MessageFormat(message);
    } catch (IllegalArgumentException e) {
      plugin
          .getLogger()
          .severe("Invalid message format for '" + path + "': " + e.getLocalizedMessage());
      return new MessageFormat("'" + message.replace("'", "''") + "'");
    }
  }

  /// Copies bundled language files to the plugin data folder when they do not exist yet.
  ///
  /// @param plugin plugin instance used to save bundled resources
  private void copyDefaultLanguages(JavaPlugin plugin) {
    for (String resource : bundledLanguages) {
      File target = new File(plugin.getDataFolder(), resource);
      if (!target.exists()) plugin.saveResource(resource, false);
    }
  }

  /// Loads a language file from the plugin data folder.
  ///
  /// @param plugin plugin instance used to locate the data folder
  /// @param language language code to load
  /// @return loaded YAML configuration
  private FileConfiguration loadLanguage(JavaPlugin plugin, String language) {
    FileConfiguration cachedLanguage = languageCache.get(language);
    if (cachedLanguage != null) return cachedLanguage;

    File file = new File(plugin.getDataFolder(), "languages/" + language + ".yml");
    if (!file.exists()) {
      if (!language.equals(fallbackLanguage) && missingLanguages.add(language)) {
        plugin
            .getLogger()
            .warning("Language file is missing. Trying language fallbacks: " + language);
      }
      FileConfiguration emptyLanguage = new YamlConfiguration();
      languageCache.put(language, emptyLanguage);
      return emptyLanguage;
    }

    FileConfiguration loadedLanguage = YamlConfiguration.loadConfiguration(file);
    languageCache.put(language, loadedLanguage);
    return loadedLanguage;
  }

  /// Loads a bundled language resource from the plugin jar.
  ///
  /// @param language language code to load
  /// @return loaded YAML configuration, or an empty configuration when the resource is missing
  private FileConfiguration loadBundledLanguage(String language) {
    FileConfiguration cachedLanguage = bundledLanguageCache.get(language);
    if (cachedLanguage != null) return cachedLanguage;

    String resourcePath = "languages/" + language + ".yml";
    try (InputStream stream = plugin.getResource(resourcePath)) {
      if (stream == null) {
        FileConfiguration emptyLanguage = new YamlConfiguration();
        bundledLanguageCache.put(language, emptyLanguage);
        return emptyLanguage;
      }

      FileConfiguration loadedLanguage =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(stream, StandardCharsets.UTF_8));
      bundledLanguageCache.put(language, loadedLanguage);
      return loadedLanguage;
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe(
              "Failed to load bundled language resource '"
                  + resourcePath
                  + "': "
                  + e.getLocalizedMessage());
      FileConfiguration emptyLanguage = new YamlConfiguration();
      bundledLanguageCache.put(language, emptyLanguage);
      return emptyLanguage;
    }
  }

  /// Returns the broad language code from a locale-like code.
  ///
  /// @param language normalized language code
  /// @return base language code
  private String getBaseLanguage(String language) {
    int separatorIndex = language.indexOf('_');
    if (separatorIndex < 0) return language;
    return language.substring(0, separatorIndex);
  }

  /// Normalizes a configured language code and falls back to the default language when blank.
  ///
  /// @param language configured language code
  /// @return normalized language code
  private String normalizeLanguage(String language) {
    return normalizeLanguage(language, defaultLanguage);
  }

  /// Normalizes a language code with a caller-defined fallback for blank values.
  ///
  /// @param language configured language code
  /// @param fallback language code used when configured value is blank
  /// @return normalized language code
  private String normalizeLanguage(String language, String fallback) {
    if (language == null || language.isBlank()) return fallback;
    return language.strip().replace('-', '_').toLowerCase(Locale.ROOT);
  }
}
