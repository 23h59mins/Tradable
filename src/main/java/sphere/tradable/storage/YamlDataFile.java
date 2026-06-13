// File: src/main/java/sphere/tradable/storage/YamlDataFile.java
package sphere.tradable.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public abstract class YamlDataFile {
    protected final JavaPlugin plugin;

    private final String fileName;
    private final boolean copyDefaults;

    private File file;
    private YamlConfiguration configuration;

    protected YamlDataFile(final JavaPlugin plugin, final String fileName) {
        this(plugin, fileName, true);
    }

    protected YamlDataFile(final JavaPlugin plugin, final String fileName, final boolean copyDefaults) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.copyDefaults = copyDefaults;
        reload();
    }

    public final synchronized void reload() {
        ensureDataFolder();

        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            createBackingFile();
        }

        this.configuration = YamlConfiguration.loadConfiguration(file);
        onReload(configuration);
    }

    public final synchronized void save() {
        if (configuration == null || file == null) {
            throw new IllegalStateException("YAML file is not initialized: " + fileName);
        }

        beforeSave(configuration);

        try {
            configuration.save(file);
        } catch (final IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + fileName, exception);
        }
    }

    public final synchronized FileConfiguration config() {
        if (configuration == null) {
            reload();
        }

        return configuration;
    }

    protected final synchronized ConfigurationSection section(final String path) {
        final ConfigurationSection existing = config().getConfigurationSection(path);
        return existing != null ? existing : config().createSection(path);
    }

    protected final synchronized void remove(final String path) {
        config().set(path, null);
    }

    public final File file() {
        return file;
    }

    public final String fileName() {
        return fileName;
    }

    protected void onReload(final FileConfiguration configuration) {
    }

    protected void beforeSave(final FileConfiguration configuration) {
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning(
                    "Failed to create plugin data folder: " + plugin.getDataFolder().getAbsolutePath()
            );
        }
    }

    private void createBackingFile() {
        if (copyDefaults && plugin.getResource(fileName) != null) {
            plugin.saveResource(fileName, false);
            return;
        }

        try {
            if (!file.createNewFile()) {
                plugin.getLogger().warning("Could not create " + fileName + ".");
            }
        } catch (final IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create " + fileName, exception);
        }
    }
}