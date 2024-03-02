package dev.daniboy.tpsrambar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPSRAMBar extends JavaPlugin implements CommandExecutor, Listener {

    private int updateIntervalTicks = 20; // 1 second
    private Map<UUID, BossBar> playerTPSBars = new HashMap<>();
    private Map<UUID, BossBar> playerRAMBars = new HashMap<>();
    private BarColor tpsBarColor = BarColor.GREEN;
    private BarColor ramBarColor = BarColor.BLUE;
    private ChatColor tpsBarTextColor = ChatColor.WHITE;
    private ChatColor ramBarTextColor = ChatColor.WHITE;
    private String tpsBarEnabledMessage = "&aTPS bar has been enabled for you.";
    private String tpsBarDisabledMessage = "&cTPS bar has been disabled for you.";
    private String ramBarEnabledMessage = "&aRAM bar has been enabled for you.";
    private String ramBarDisabledMessage = "&cRAM bar has been disabled for you.";
    private String reloadMessage = "&aConfiguration reloaded.";

    @Override
    public void onEnable() {
        getLogger().info("\u001B[32m================================");
        getLogger().info("\u001B[32m  TPSRAMBar Plugin Enabled");
        getLogger().info("\u001B[32m================================");
        getLogger().info("\u001B[33mVersion: " + getDescription().getVersion());
        getLogger().info("\u001B[33mAuthor: " + getDescription().getAuthors().get(0));
        getLogger().info("\u001B[33mDescription: " + getDescription().getDescription());
        getLogger().info("\u001B[32m================================");
        getCommand("tpsbar").setExecutor(this);
        getCommand("rambar").setExecutor(this);
        getCommand("tpsramreload").setExecutor(this); // Register reload command
        getServer().getPluginManager().registerEvents(this, this);

        // Load configuration
        loadConfig();

        // Schedule task to update boss bars periodically
        new BukkitRunnable() {
            @Override
            public void run() {
                updateBossBars();
            }
        }.runTaskTimer(this, 0, updateIntervalTicks);
    }

    @Override
    public void onDisable() {
        getLogger().info("\u001B[32m================================");
        getLogger().info("\u001B[32m  TPSRAMBar Plugin Disabled");
        getLogger().info("\u001B[32m================================");
        // Remove all boss bars on server shutdown
        removeAllBossBars();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("tpsbar")) {
            toggleBossBar(player, "tps");
        } else if (command.getName().equalsIgnoreCase("rambar")) {
            toggleBossBar(player, "ram");
        } else if (command.getName().equalsIgnoreCase("tpsramreload")) { // Reload command
            if (player.hasPermission("tpsrambar.reload")) {
                reloadConfig(); // Reload configuration
                loadConfig(); // Load reloaded configuration
                updateBossBarTitles(); // Update boss bar titles
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMessage));
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("tpsrambar.tps")) {
            sendTPSBar(player); // Automatically send TPS bar to players on join
        }
        if (player.hasPermission("tpsrambar.ram")) {
            sendRAMBar(player); // Automatically send RAM bar to players on join
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removePlayerTPSBar(player);
        removePlayerRAMBar(player);
    }

    private void removeAllBossBars() {
        removeAllBars(playerTPSBars);
        removeAllBars(playerRAMBars);
    }

    private void removeAllBars(Map<UUID, BossBar> bars) {
        for (BossBar bossBar : bars.values()) {
            bossBar.removeAll();
        }
        bars.clear();
    }

    private void removePlayerTPSBar(Player player) {
        removePlayerBar(player, playerTPSBars);
    }

    private void removePlayerRAMBar(Player player) {
        removePlayerBar(player, playerRAMBars);
    }

    private void removePlayerBar(Player player, Map<UUID, BossBar> bars) {
        if (bars.containsKey(player.getUniqueId())) {
            bars.get(player.getUniqueId()).removeAll();
            bars.remove(player.getUniqueId());
        }
    }

    private void loadConfig() {
        // Load boss bar color configuration
        tpsBarColor = getColor("tps-bar.color", BarColor.GREEN);
        ramBarColor = getColor("ram-bar.color", BarColor.BLUE);
        // Load boss bar text color configuration
        tpsBarTextColor = getChatColor("tps-bar.text-color", ChatColor.WHITE);
        ramBarTextColor = getChatColor("ram-bar.text-color", ChatColor.WHITE);
        // Load messages configuration
        tpsBarEnabledMessage = getMessage("tps-bar.enabled-message", "&aTPS bar has been enabled for you.");
        tpsBarDisabledMessage = getMessage("tps-bar.disabled-message", "&cTPS bar has been disabled for you.");
        ramBarEnabledMessage = getMessage("ram-bar.enabled-message", "&aRAM bar has been enabled for you.");
        ramBarDisabledMessage = getMessage("ram-bar.disabled-message", "&cRAM bar has been disabled for you.");
        reloadMessage = getMessage("reload-message", "&aConfiguration reloaded.");

        // Save default config if not exist
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private BarColor getColor(String path, BarColor defaultColor) {
        return BarColor.valueOf(getConfig().getString(path, defaultColor.name()));
    }

    private ChatColor getChatColor(String path, ChatColor defaultColor) {
        return ChatColor.valueOf(getConfig().getString(path, defaultColor.name()));
    }

    private String getMessage(String path, String defaultMessage) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, defaultMessage));
    }

    private void sendTPSBar(Player player) {
        sendBar(player, playerTPSBars, tpsBarColor, tpsBarTextColor, tpsBarEnabledMessage);
        updateTPSBar(player);
    }

    private void sendRAMBar(Player player) {
        sendBar(player, playerRAMBars, ramBarColor, ramBarTextColor, ramBarEnabledMessage);
        updateRAMBar(player);
    }

    private void sendBar(Player player, Map<UUID, BossBar> bars, BarColor color, ChatColor textColor, String enabledMessage) {
        if (!bars.containsKey(player.getUniqueId())) {
            BossBar bar = Bukkit.createBossBar("", color, BarStyle.SOLID);
            bars.put(player.getUniqueId(), bar);
            player.sendMessage(textColor + enabledMessage);
        }
    }

    private void toggleBossBar(Player player, String barType) {
        Map<UUID, BossBar> bars = barType.equalsIgnoreCase("tps") ? playerTPSBars : playerRAMBars;
        BarColor color = barType.equalsIgnoreCase("tps") ? tpsBarColor : ramBarColor;
        ChatColor textColor = barType.equalsIgnoreCase("tps") ? tpsBarTextColor : ramBarTextColor;
        String enabledMessage = barType.equalsIgnoreCase("tps") ? tpsBarEnabledMessage : ramBarEnabledMessage;
        String disabledMessage = barType.equalsIgnoreCase("tps") ? tpsBarDisabledMessage : ramBarDisabledMessage;

        if (bars.containsKey(player.getUniqueId())) {
            bars.get(player.getUniqueId()).removeAll();
            bars.remove(player.getUniqueId());
            player.sendMessage(textColor + disabledMessage);
        } else {
            sendBar(player, bars, color, textColor, enabledMessage);
        }
    }

    private void updateBossBars() {
        Bukkit.getOnlinePlayers().forEach(this::updateBars);
    }

    private void updateBars(Player player) {
        if (player.hasPermission("tpsrambar.tps")) {
            updateTPSBar(player);
        }
        if (player.hasPermission("tpsrambar.ram")) {
            updateRAMBar(player);
        }
    }

    private void updateTPSBar(Player player) {
        try {
            Method getTPSMethod = Bukkit.getServer().getClass().getMethod("getTPS");
            double[] tps = (double[]) getTPSMethod.invoke(Bukkit.getServer());
            double currentTPS = Math.min(tps[0], 20); // Limit TPS to 20
            double averageTPS = Math.min(tps[1], 20); // Limit TPS to 20

            BossBar tpsBar = playerTPSBars.get(player.getUniqueId());
            if (tpsBar != null) {
                tpsBar.setTitle(tpsBarTextColor + "TPS: " + String.format("%.2f", currentTPS) + " AVG: " + String.format("%.2f", averageTPS));
                tpsBar.setProgress(currentTPS / 20.0); // Since there are 20 ticks per second
                tpsBar.addPlayer(player);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to update TPS bar: " + e.getMessage());
        }
    }

    private void updateRAMBar(Player player) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();

        BossBar ramBar = playerRAMBars.get(player.getUniqueId());
        if (ramBar != null) {
            ramBar.setTitle(ramBarTextColor + "RAM Usage: " + (usedMemory / 1048576) + "MB / " + (maxMemory / 1048576) + "MB");
            ramBar.setProgress((double) usedMemory / maxMemory);
            ramBar.addPlayer(player);
        }
    }

    private void updateBossBarTitles() {
        playerTPSBars.values().forEach(bar -> bar.setColor(tpsBarColor));
        playerRAMBars.values().forEach(bar -> bar.setColor(ramBarColor));
    }
}
