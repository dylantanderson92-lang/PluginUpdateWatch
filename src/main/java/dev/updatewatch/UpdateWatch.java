package dev.updatewatch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import dev.updatewatch.UpdateCheckService.Result;
import java.util.concurrent.*;

public final class UpdateWatch extends JavaPlugin implements Listener, TabCompleter {
    // Mutable state below is owned exclusively by the server thread.
    private Map<String, Result> results = Map.of();
    private final CheckState checkState = new CheckState();
    private UpdateCheckService.Report lastReport;
    private Settings settings;
    private boolean requireChecksum;
    private String loadedConfig;
    private String lastCheckFailure;
    private final Set<String> downloads = new HashSet<>();
    private ExecutorService worker;
    private BukkitTask timer;
    private Map<String, String> discoveryNotes = Map.of();

    @Override public void onEnable() {
        saveDefaultConfig();
        var compatibility = Compatibility.classify(getServer().getMinecraftVersion());
        if (compatibility == Compatibility.Status.BELOW_MINIMUM) {
            getLogger().severe("Requires Paper 1.21.11 or newer."); getServer().getPluginManager().disablePlugin(this); return;
        }
        if (compatibility == Compatibility.Status.UNVERIFIED) getLogger().warning("This Minecraft version is outside the targeted 1.21.11–26.3 range; compatibility is unverified.");
        try { loadSettings(); } catch (Exception e) {
            getLogger().severe(Failure.classify(e).describe(null)); getServer().getPluginManager().disablePlugin(this); return;
        }
        worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "PluginUpdateWatch-IO"); t.setDaemon(true); return t; });
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("pluginupdates")).setTabCompleter(this);
        schedule();
    }
    @Override public void onDisable() { checkState.invalidate(); if (worker != null) worker.shutdownNow(); }
    private void loadSettings() throws Exception {
        String contents = java.nio.file.Files.readString(getDataFolder().toPath().resolve("config.yml"));
        var parsed = ConfigManager.parse(contents);
        var loaded = Settings.parse(parsed, getDataFolder().toPath().toAbsolutePath().getParent());
        boolean checksumPolicy = Settings.requireChecksum(parsed);
        reloadConfig(); settings = loaded;
        requireChecksum = checksumPolicy;
        loadedConfig = contents;
    }
    private void schedule() {
        if (timer != null) timer.cancel();
        long minutes = Math.clamp(getConfig().getLong("check-interval-minutes", 360), 15, 10080);
        checkState.requestScan();
        timer = getServer().getScheduler().runTaskTimer(this, () -> {
            if (!checkState.running()) check(null, checkState.takePendingScan());
        }, 100, minutes * 1200);
    }
    private void sync(Runnable action) {
        if (isEnabled()) {
            try { getServer().getScheduler().runTask(this, action); } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { }
        }
    }
    private void tell(CommandSender sender, String message) {
        NamedTextColor color = message.contains("[ERROR]") ? NamedTextColor.RED
                : message.contains("[WARNING]") ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
        sender.sendMessage(Component.text("[Updates] ", NamedTextColor.AQUA).append(Component.text(message, color)));
    }
    private void check(CommandSender sender, boolean scan) {
        if (!getServer().isPrimaryThread()) throw new IllegalStateException("Check state must be accessed on the server thread");
        if (checkState.running()) { if (sender != null) tell(sender, "A check is already running."); return; }
        var installed = Arrays.stream(getServer().getPluginManager().getPlugins()).filter(p -> p != this)
                .map(p -> new Discovery.Installed(p.getName(), p.getPluginMeta().getVersion(), p.getPluginMeta().getWebsite())).toList();
        String snapshot = getConfig().saveToString();
        var configFile = getDataFolder().toPath().resolve("config.yml");
        String diskSnapshot;
        try { diskSnapshot = java.nio.file.Files.readString(configFile); }
        catch (Exception e) { tell(sender == null ? getServer().getConsoleSender() : sender, "Cannot read config.yml. " + Failure.classify(e).describe(null)); return; }
        if (!diskSnapshot.equals(loadedConfig)) {
            tell(sender == null ? getServer().getConsoleSender() : sender, "[WARNING] CONFIG_ERROR | Config changed on disk. Run /pu reload before checking or scanning."); return;
        }
        String minecraft = getServer().getMinecraftVersion();
        Settings checkSettings = settings;
        long epoch = checkState.begin();
        if (sender != null) tell(sender, scan ? "Scanning installed JARs and checking update sources..." : "Checking configured plugins...");
        worker.execute(() -> {
            UpdateCheckService.Report report;
            try {
                // Parse the file itself so invalid YAML can never be replaced by default values.
                report = UpdateCheckService.check(diskSnapshot, installed, minecraft, scan, checkSettings);
            } catch (Exception e) {
                sync(() -> {
                    if (checkState.finish(epoch)) {
                        lastCheckFailure = "Scan/check failed: " + UpdateCheckService.message(e);
                        tell(sender == null ? getServer().getConsoleSender() : sender, lastCheckFailure);
                        getLogger().warning(lastCheckFailure);
                    }
                    resumePendingScan();
                });
                return;
            }
            var resolution = report.resolution();
            var checked = report.results();
            sync(() -> {
                if (!checkState.finish(epoch)) { resumePendingScan(); return; }
                try {
                    if (!snapshot.equals(getConfig().saveToString()) || !diskSnapshot.equals(java.nio.file.Files.readString(configFile))) {
                        lastCheckFailure = "[WARNING] CONFIG_ERROR | Config changed during scan; results discarded. Run /pu reload.";
                        tell(sender == null ? getServer().getConsoleSender() : sender, lastCheckFailure); return;
                    }
                    if (scan && !resolution.entries().equals(getConfig().getMapList("updates"))) {
                        var updated = ConfigManager.parse(diskSnapshot);
                        updated.set("updates", resolution.entries());
                        ConfigManager.save(configFile, diskSnapshot, updated.saveToString());
                        loadedConfig = updated.saveToString();
                        reloadConfig();
                    }
                } catch (Exception e) {
                    lastCheckFailure = "Could not save discovery config. " + Failure.classify(e).describe(null);
                    tell(sender == null ? getServer().getConsoleSender() : sender, lastCheckFailure); return;
                }
                boolean changed = !checked.equals(results);
                changed |= !discoveryNotes.equals(resolution.notes());
                resolution.notes().forEach((name, note) -> {
                    if (!Objects.equals(discoveryNotes.get(name), note)) getLogger().warning(name + ": " + note);
                });
                discoveryNotes = resolution.notes();
                results = checked; lastReport = report;
                lastCheckFailure = null;
                for (var result : results.values()) if (result.error() != null) getLogger().warning(result.source().name() + ": " + result.error());
                if (checkSettings.debug()) getLogger().info("Check completed in " + report.durationMillis() + " ms; " + report.counts() + "; updates=" + updateCount());
                if (sender != null) show(sender);
                if (changed) {
                    show(getServer().getConsoleSender());
                    if (hasUpdates()) for (var player : getServer().getOnlinePlayers())
                        if (player.hasPermission("pluginupdatewatch.admin") && player != sender) tell(player, "Plugin updates are available. Use /pu list.");
                }
                resumePendingScan();
            });
        });
    }
    private void resumePendingScan() {
        if (checkState.takePendingScan()) check(null, true);
    }
    private long updateCount() { return results.values().stream().filter(r -> r.error() == null && r.status() == Versions.Status.UPDATE).count(); }
    private boolean hasUpdates() { return results.values().stream().anyMatch(r -> r.error() == null && r.status() != Versions.Status.CURRENT); }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("notify-on-join", true) && event.getPlayer().hasPermission("pluginupdatewatch.admin") && hasUpdates())
            tell(event.getPlayer(), "Plugin updates are available. Use /pu list.");
    }
    private void show(CommandSender sender) {
        if (lastCheckFailure != null) {
            tell(sender, lastCheckFailure);
            if (!results.isEmpty()) tell(sender, "[WARNING] The results below are cached from an earlier check, not a fresh update status.");
        }
        if (results.isEmpty() && discoveryNotes.isEmpty()) tell(sender, "No results yet. Use /pu scan to detect installed plugins.");
        discoveryNotes.forEach((name, note) -> tell(sender, (note.contains("[ERROR]") ? "" : "[WARNING] ") + name + ": " + note));
        for (Result r : results.values()) {
            if (r.error() != null) { tell(sender, r.source().name() + ": check failed - " + r.error()); continue; }
            String label = switch (r.status()) {
                case CURRENT -> "[INFO] CURRENT | up to date (or installed version is newer)";
                case UPDATE -> "[INFO] NOT_CURRENT | update available";
                case DIFFERENT -> "[WARNING] NOT_CURRENT | different release; verify version ordering";
            };
            NamedTextColor color = r.status() == Versions.Status.CURRENT ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            Component line = Component.text(r.source().name() + ": " + r.source().installed() + " -> " + r.release().version() + " - " + label, color);
            if (r.status() != Versions.Status.CURRENT) {
                line = line.append(Component.text(" [Release page]", NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(r.release().page())));
                if (r.release().download() != null && (!requireChecksum || r.release().hasChecksum())) line = line.append(Component.text(" [Download]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand("/pu download " + r.source().name())));
                else line = line.append(Component.text(" (manual download required)", NamedTextColor.YELLOW));
                if (r.release().download() != null && !r.release().hasChecksum()) line = line.append(Component.text(requireChecksum
                        ? " [WARNING] No checksum; automatic download blocked" : " [WARNING] Checksum unavailable; explicitly allowed without integrity verification", NamedTextColor.YELLOW));
            }
            sender.sendMessage(line);
        }
        long untracked = Arrays.stream(getServer().getPluginManager().getPlugins()).filter(p -> p != this && !results.containsKey(p.getName().toLowerCase(Locale.ROOT))).count();
        tell(sender, untracked + " plugin(s) not checked. See messages above; manual entries need only jar and source link.");
    }
    private void download(CommandSender sender, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Result r = results.get(key);
        if (r == null || r.error() != null || r.status() == Versions.Status.CURRENT || r.release().download() == null) {
            tell(sender, "No downloadable update cached for that plugin. Use /pu check or its release page."); return;
        }
        if (requireChecksum && !r.release().hasChecksum()) {
            String message = "[ERROR] PROVIDER_ERROR | " + r.source().type() + " supplies no checksum for " + r.source().name()
                    + ". Download blocked. Use its release page, or explicitly set downloads.require-checksum: false and /pu reload to allow an unverified download.";
            tell(sender, message); getLogger().warning(message); return;
        }
        if (!downloads.add(key)) { tell(sender, "That download is already running."); return; }
        tell(sender, "Downloading " + r.source().name() + " " + r.release().version() + "...");
        Settings downloadSettings = settings;
        boolean checksumRequired = requireChecksum;
        var downloadFolder = getDataFolder().toPath().resolve("downloads");
        if (!r.release().hasChecksum()) {
            String warning = "[WARNING] " + r.source().name() + ": checksum unavailable; explicitly allowed by config. HTTPS and JAR structure will be checked, but integrity is unverified.";
            tell(sender, warning); getLogger().warning(warning);
        }
        worker.execute(() -> {
            String message;
            try {
                var path = Remote.download(r.source(), r.release(), downloadFolder, downloadSettings, checksumRequired);
                message = (r.release().hasChecksum() ? "[INFO] Checksum verified. " : "[WARNING] Checksum unverified (explicitly allowed). ")
                        + "Saved " + path + ". Stop the server, replace the old plugin JAR, then restart. Check the release's Minecraft compatibility first.";
            } catch (Exception e) { message = Failure.classify(e).describe(r.source().type()) + " Retry downloads with /pu download " + r.source().name() + " after resolving the problem."; }
            String finalMessage = message;
            sync(() -> {
                downloads.remove(key); tell(sender, finalMessage);
                if (finalMessage.contains("[ERROR]") || finalMessage.contains("[WARNING]")) getLogger().warning(finalMessage);
                else getLogger().info(finalMessage);
            });
        });
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pluginupdatewatch.admin")) { tell(sender, "You do not have permission."); return true; }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> show(sender);
            case "check" -> check(sender, false);
            case "scan" -> check(sender, true);
            case "download" -> { if (args.length == 2) download(sender, args[1]); else tell(sender, "Usage: /pu download <plugin>"); }
            case "stats" -> {
                if (lastReport == null) tell(sender, "No completed check yet.");
                else tell(sender, "Last check: " + lastReport.durationMillis() + " ms; updates: " + updateCount() + "; provider success/failure: " + lastReport.counts());
            }
            case "reload" -> {
                try {
                    loadSettings(); checkState.invalidate(); results = Map.of(); discoveryNotes = Map.of(); lastReport = null; lastCheckFailure = null;
                    schedule(); tell(sender, "Configuration reloaded. Automatic scan will run shortly.");
                } catch (Exception e) { tell(sender, "Reload rejected; previous settings retained. " + Failure.classify(e).describe(null)); }
            }
            default -> tell(sender, "Usage: /pu [list|scan|check|download <plugin>|stats|reload]");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pluginupdatewatch.admin")) return List.of();
        List<String> choices = args.length == 1 ? List.of("list", "scan", "check", "download", "stats", "reload")
                : args.length == 2 && args[0].equalsIgnoreCase("download") ? results.values().stream()
                .filter(r -> r.error() == null && r.status() != Versions.Status.CURRENT && r.release().download() != null && (!requireChecksum || r.release().hasChecksum())).map(r -> r.source().name()).toList() : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
