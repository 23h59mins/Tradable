package sphere.tradable;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;
import sphere.tradable.command.AdminCommands;
import sphere.tradable.command.PlayerCommands;
import sphere.tradable.gui.MenuListener;
import sphere.tradable.model.AuctionEntry;
import sphere.tradable.model.BountyEntry;
import sphere.tradable.placeholder.TradablePlaceholderExpansion;
import sphere.tradable.service.AuctionService;
import sphere.tradable.service.BountyService;
import sphere.tradable.service.EconomyService;
import sphere.tradable.storage.AuctionStorage;
import sphere.tradable.storage.BalanceStorage;
import sphere.tradable.storage.BountyStorage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public final class Tradable extends JavaPlugin {
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String MESSAGES_FILE_NAME = "messages.yml";

    private static final String SHOP_ROOT_PATH = "shop";
    private static final String SHOP_CATEGORIES_PATH = "shop.categories";
    private static final String SHOP_CLEAR_ON_NEXT_START_PATH = "shop.clear-categories-on-next-start";

    private static final String ADMIN_PERMISSION = "tradable.admin";

    private static final Set<String> PLAYER_COMMAND_ALIASES = Set.of(
            "bal", "balance", "money",
            "baltop", "topbal", "balancetop",
            "shop",
            "provide", "pay", "givecash",
            "bounty", "bounties",
            "bountyset", "setbounty",
            "ah", "auctionhouse", "auction",
            "ahsell", "sellah"
    );

    private static final Set<String> ADMIN_COMMAND_ALIASES = Set.of(
            "tradable", "tr"
    );

    private File messagesFile;
    private YamlConfiguration messages;

    private BalanceStorage balanceStorage;
    private AuctionStorage auctionStorage;
    private BountyStorage bountyStorage;

    private EconomyService economyService;
    private AuctionService auctionService;
    private BountyService bountyService;

    private AdminCommands adminCommands;
    private PlayerCommands playerCommands;

    private TradablePlaceholderExpansion placeholderExpansion;
    private boolean successfullyEnabled;

    @Override
    public void onLoad() {
        registerSerializables();
    }

    @Override
    public void onEnable() {
        try {
            bootstrap();
            successfullyEnabled = true;
            getLogger().info("Tradable has been enabled successfully.");
        } catch (final Exception exception) {
            successfullyEnabled = false;
            getLogger().log(Level.SEVERE, "Tradable failed to enable.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        unregisterPlaceholders();

        if (successfullyEnabled) {
            saveAll();
        } else {
            getLogger().warning("Tradable did not finish enabling, so the full save routine was skipped.");
        }

        getLogger().info("Tradable has been disabled.");
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public BalanceStorage getBalanceStorage() {
        return balanceStorage;
    }

    public AuctionStorage getAuctionStorage() {
        return auctionStorage;
    }

    public BountyStorage getBountyStorage() {
        return bountyStorage;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public BountyService getBountyService() {
        return bountyService;
    }

    public void reloadPlugin() {
        reloadMainConfig();
        reloadMessages();
        reloadStorages();
        registerPlaceholders();
    }

    public void refreshShopConfig() {
        reloadMainConfig();
    }

    public void saveAndRefreshShopConfig() {
        saveConfig();
        refreshShopConfig();
    }

    private void bootstrap() {
        ensureDataFolder();

        loadMainConfig();
        loadMessages();

        initializeStorages();
        initializeServices();
        initializeCommands();

        registerListeners();
        registerCommands();
        registerPlaceholders();
    }

    private void registerPlaceholders() {
        unregisterPlaceholders();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI was not found. Tradable placeholders were not registered.");
            return;
        }

        placeholderExpansion = new TradablePlaceholderExpansion(this);

        if (!placeholderExpansion.register()) {
            getLogger().warning("Failed to register Tradable PlaceholderAPI expansion.");
            placeholderExpansion = null;
            return;
        }

        getLogger().info("Registered Tradable PlaceholderAPI expansion.");
    }

    private void unregisterPlaceholders() {
        if (placeholderExpansion == null) {
            return;
        }

        try {
            placeholderExpansion.unregister();
        } catch (final Throwable ignored) {
        } finally {
            placeholderExpansion = null;
        }
    }

    private void registerSerializables() {
        ConfigurationSerialization.registerClass(AuctionEntry.class, "AuctionEntry");
        ConfigurationSerialization.registerClass(BountyEntry.class, "BountyEntry");
    }

    private void loadMainConfig() {
        ensureBackedFile(CONFIG_FILE_NAME);
        reloadConfig();
        normalizeMainConfig();
        clearShopCategoriesOnNextStart();
    }

    private void reloadMainConfig() {
        ensureBackedFile(CONFIG_FILE_NAME);
        reloadConfig();
        normalizeMainConfig();
        clearShopCategoriesOnNextStart();
    }

    private void normalizeMainConfig() {
        final FileConfiguration config = getConfig();
        boolean changed = false;

        if (!config.isConfigurationSection(SHOP_ROOT_PATH)) {
            config.createSection(SHOP_ROOT_PATH);
            changed = true;
        }

        if (!config.contains("shop.enabled")) {
            config.set("shop.enabled", true);
            changed = true;
        }

        if (!config.contains("shop.default-sort")) {
            config.set("shop.default-sort", "lowest");
            changed = true;
        }

        if (!config.contains(SHOP_CLEAR_ON_NEXT_START_PATH)) {
            config.set(SHOP_CLEAR_ON_NEXT_START_PATH, false);
            changed = true;
        }

        if (!config.isConfigurationSection(SHOP_CATEGORIES_PATH)) {
            config.set(SHOP_CATEGORIES_PATH, null);
            config.createSection(SHOP_CATEGORIES_PATH);
            changed = true;
        }

        if (changed) {
            saveConfig();
            reloadConfig();
        }
    }

    private void clearShopCategoriesOnNextStart() {
        final FileConfiguration config = getConfig();

        if (!config.getBoolean(SHOP_CLEAR_ON_NEXT_START_PATH, false)) {
            return;
        }

        final int removedCount = countShopCategories(config);

        config.set(SHOP_CATEGORIES_PATH, null);
        config.createSection(SHOP_CATEGORIES_PATH);
        config.set(SHOP_CLEAR_ON_NEXT_START_PATH, false);

        saveConfig();
        reloadConfig();

        getLogger().info(
                "Cleared " + removedCount + " shop categor" +
                        (removedCount == 1 ? "y" : "ies") +
                        ". The shop is now empty, and '" +
                        SHOP_CLEAR_ON_NEXT_START_PATH +
                        "' has been reset to false."
        );
    }

    private int countShopCategories(final FileConfiguration config) {
        final ConfigurationSection section = config.getConfigurationSection(SHOP_CATEGORIES_PATH);
        return section == null ? 0 : section.getKeys(false).size();
    }

    private void loadMessages() {
        messagesFile = ensureBackedFile(MESSAGES_FILE_NAME);
        messages = loadYaml(messagesFile, MESSAGES_FILE_NAME);
    }

    private void reloadMessages() {
        loadMessages();
    }

    private void initializeStorages() {
        balanceStorage = new BalanceStorage(this);
        auctionStorage = new AuctionStorage(this);
        bountyStorage = new BountyStorage(this);
    }

    private void initializeServices() {
        economyService = new EconomyService(balanceStorage);
        auctionService = new AuctionService(this, auctionStorage);
        bountyService = new BountyService(bountyStorage);
    }

    private void initializeCommands() {
        adminCommands = new AdminCommands(this);
        playerCommands = new PlayerCommands(this);
    }

    private void registerListeners() {
        registerListener(new MenuListener());
        registerListener(createCommandVisibilityListener());
    }

    private Listener createCommandVisibilityListener() {
        return new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onPlayerCommandSend(final PlayerCommandSendEvent event) {
                final Collection<String> commands = event.getCommands();
                if (commands.isEmpty()) {
                    return;
                }

                final boolean isAdmin = event.getPlayer().hasPermission(ADMIN_PERMISSION);
                final String pluginName = getName().toLowerCase(Locale.ROOT);

                final Set<String> removals = new HashSet<>();

                for (final String raw : commands) {
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }

                    final String command = raw.toLowerCase(Locale.ROOT);

                    if (!isAdmin && ADMIN_COMMAND_ALIASES.contains(command)) {
                        removals.add(raw);
                        continue;
                    }

                    final int separator = command.indexOf(':');
                    if (separator <= 0 || separator >= command.length() - 1) {
                        continue;
                    }

                    final String namespace = command.substring(0, separator);
                    final String base = command.substring(separator + 1);

                    if (!namespace.equals(pluginName)) {
                        continue;
                    }

                    if (PLAYER_COMMAND_ALIASES.contains(base)) {
                        removals.add(raw);
                        continue;
                    }

                    if (!isAdmin || ADMIN_COMMAND_ALIASES.contains(base)) {
                        removals.add(raw);
                    }
                }

                commands.removeAll(removals);
            }
        };
    }

    private void registerCommands() {
        registerCommand("tradable", adminCommands, adminCommands);

        registerPlayerCommand("bal");
        registerPlayerCommand("baltop");
        registerPlayerCommand("shop");
        registerPlayerCommand("provide");
        registerPlayerCommand("bountyset");
        registerPlayerCommand("bounty");
        registerPlayerCommand("ah");
        registerPlayerCommand("ahsell");
    }

    private void registerPlayerCommand(final String name) {
        registerCommand(name, playerCommands, playerCommands);
    }

    private void registerCommand(
            final String name,
            final CommandExecutor executor,
            final TabCompleter completer
    ) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(executor, "executor");

        final PluginCommand command = getCommand(name);

        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + name);
            return;
        }

        command.setExecutor(executor);

        if (completer != null) {
            command.setTabCompleter(completer);
        }
    }

    private void registerListener(final Listener listener) {
        Objects.requireNonNull(listener, "listener");
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void reloadStorages() {
        runSafely("reload balances.yml", balanceStorage == null ? null : balanceStorage::reload);
        runSafely("reload auctions.yml", auctionStorage == null ? null : auctionStorage::reload);
        runSafely("reload bounties.yml", bountyStorage == null ? null : bountyStorage::reload);
    }

    private void saveAll() {
        runSafely("save balances.yml", balanceStorage == null ? null : balanceStorage::save);
        runSafely("save auctions.yml", auctionStorage == null ? null : auctionStorage::save);
        runSafely("save bounties.yml", bountyStorage == null ? null : bountyStorage::save);
        runSafely("save " + MESSAGES_FILE_NAME, this::saveMessages);
        runSafely("save " + CONFIG_FILE_NAME, this::saveConfig);
    }

    private void saveMessages() throws IOException {
        if (messages == null || messagesFile == null) {
            return;
        }

        messages.save(messagesFile);
    }

    private void runSafely(final String action, final ThrowingRunnable task) {
        if (task == null) {
            return;
        }

        try {
            task.run();
        } catch (final Exception exception) {
            getLogger().log(Level.WARNING, "Failed to " + action + ".", exception);
        }
    }

    private YamlConfiguration loadYaml(final File file, final String fileName) {
        final YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.load(file);
            return configuration;
        } catch (final IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load " + fileName + ".", exception);
        }
    }

    private File ensureBackedFile(final String fileName) {
        ensureDataFolder();

        final File file = new File(getDataFolder(), fileName);

        if (file.exists()) {
            return file;
        }

        if (getResource(fileName) != null) {
            saveResource(fileName, false);
            return file;
        }

        try {
            if (!file.createNewFile()) {
                getLogger().warning("Could not create " + fileName + ".");
            }
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create " + fileName + ".", exception);
        }

        return file;
    }

    private void ensureDataFolder() {
        final File dataFolder = getDataFolder();

        if (dataFolder.exists()) {
            return;
        }

        if (!dataFolder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create plugin data folder: " + dataFolder.getAbsolutePath()
            );
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}