package io.github.tetratheta.mol.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/// Provides convenience accessors for Bukkit `config.yml` values.
///
/// Missing paths are written with caller-provided defaults. Runtime resources such as listeners,
/// commands, and scheduled tasks should be wired by the plugin runtime, not by configuration
/// classes.
///
/// Example:
/// ```java
/// public final class ExampleConfig extends BaseConfig {
///   public ExampleConfig(JavaPlugin plugin) {
///     super(plugin);
///   }
///
///   public int getLimit() {
///     return getInt("limit", 10, 1, 100);
///   }
/// }
/// ```
public abstract class BaseConfig {
  private final Logger logger;
  private final File configPath;
  private final JavaPlugin plugin;
  private FileConfiguration config;

  /// Prepares the plugin configuration file.
  ///
  /// @param provided Your plugin instance which extends `JavaPlugin`
  public BaseConfig(JavaPlugin provided) {
    plugin = provided;
    plugin.saveDefaultConfig();
    plugin.reloadConfig();
    logger = plugin.getLogger();
    config = plugin.getConfig();
    config.options().parseComments(true);
    config.options().copyDefaults(false);
    configPath = new File(plugin.getDataFolder(), "config.yml");
  }

  /// Returns the plugin that owns this configuration.
  ///
  /// @return owning plugin
  protected JavaPlugin getPlugin() {
    return plugin;
  }

  /// Returns the live Bukkit configuration object.
  ///
  /// @return plugin configuration
  protected FileConfiguration getConfig() {
    return config;
  }

  /// Get config value as `boolean` from given path or default value if the path does not present.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist
  /// @return Config value as `boolean`
  public boolean getBoolean(String path, boolean def) {
    if (config.isSet(path)) {
      return config.getBoolean(path, def);
    } else {
      config.set(path, def);
      return def;
    }
  }

  /// Get config value as `double` from given path or default value if the path does not present,
  /// with minimum/maximum value.
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// You can set minimum value and maximum value of the value. If the value of the path is outside
  /// the boundary, it will be force-set to nearest boundary value.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @param min  Minimum value
  /// @param max  Maximum value
  /// @return Config value as `double`
  public double getDouble(String path, double def, double min, double max) {
    if (config.isSet(path)) {
      double value = config.getDouble(path, def);
      if (value > max) {
        config.set(path, max);
        return max;
      }
      if (value < min) {
        config.set(path, min);
        return min;
      }
      return value;
    } else {
      config.set(path, def);
      return def;
    }
  }

  /// Get config value as `double` from given path with default if the path does not present,
  /// with default minimum value as `0` and maximum value as `100`.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// If the value of the path is outside the boundary, it will be force-set to nearest boundary
  /// value.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @return Config value as `double`
  public double getDouble(String path, double def) {
    return getDouble(path, def, 0, 100);
  }

  /// Get config value as `int` from given path with default value if the path does not present,
  /// with minimum/maximum value.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// You can set minimum value and maximum value of the value. If the value of the path is outside
  /// the boundary, it will be force-set to nearest boundary value.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @param min  Minimum value
  /// @param max  Maximum value
  /// @return Config value as `int`
  public int getInt(String path, int def, int min, int max) {
    if (config.isSet(path)) {
      int value = config.getInt(path, def);
      if (value > max) {
        config.set(path, max);
        return max;
      }
      if (value < min) {
        config.set(path, min);
        return min;
      }
      return value;
    } else {
      config.set(path, def);
      return def;
    }
  }

  /// Get config value as `int` from given path with default if the path does not present,
  /// with default minimum value as `0` and maximum value as `100`.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// If the value of the path is outside the boundary, it will be force-set to nearest boundary
  /// value.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @return Config value as `int`
  public int getInt(String path, int def) {
    return getInt(path, def, 0, 100);
  }

  /// Get config value as `List` from given path with default if the path does not present.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist
  /// @return Config value as `List`
  public List<String> getStringList(String path, List<String> def) {
    if (config.isSet(path)) {
      return config.getStringList(path);
    } else {
      config.set(path, def);
      return def;
    }
  }

  /// Get config value as `long` from given path with default if the path does not present, with
  /// minimum/maximum value.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// You can set minimum value and maximum value of the value. If the value of the path is outside
  /// of the boundary, it will be force-set to nearest boundary value.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @param min  Minimum value
  /// @param max  Maximum value
  /// @return Config value as `long`
  public long getLong(String path, long def, long min, long max) {
    if (config.isSet(path)) {
      long value = config.getLong(path, def);
      if (value > max) {
        config.set(path, max);
        return max;
      }
      if (value < min) {
        config.set(path, min);
        return min;
      }
      return value;
    } else {
      config.set(path, def);
      return def;
    }
  }

  /// Get config value as `long` from given path with default if the path does not present,
  /// with default minimum value as `0` and maximum value as `100`.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// If the value of the path is outside the boundary, it will be force-set to nearest boundary
  /// value.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @return Config value as `long`
  public long getLong(String path, long def) {
    return getLong(path, def, 0, 100);
  }

  /// Get config value as `String` from given path or default value if the path does not present.
  ///
  /// This will create the path with given default value if the path doesn't exist.
  ///
  /// @param path Config path to get value from
  /// @param def  Default value if the path doesn't exist.
  /// @return Config value as `String`
  public String getString(String path, String def) {
    if (config.isSet(path)) {
      return config.getString(path, def);
    } else {
      config.set(path, def);
      return def;
    }
  }

  /// Saves the current plugin configuration to `config.yml`.
  public void saveConfig() {
    try {
      config.options().parseComments(true);
      config.save(configPath);
      config = plugin.getConfig();
      config.options().parseComments(true);
    } catch (IOException e) {
      logger.severe("Failed to save configuration file! - " + e.getLocalizedMessage());
    }
  }
}
