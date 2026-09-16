package dev.updatewatch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.*;

public final class UpdateWatch extends JavaPlugin implements Listener, TabCompleter {
    private record Result(Remote.Source source, Remote.Release release, Versions.Status status, String error) {}
    private final Map<String, Result> results = new LinkedHashMap<>();
    private final Set<String> downloads = new HashSet<>();
    private ExecutorService worker;
    private BukkitTask timer;
    private boolean checking;
    private int generation;

    @Override public void onEnable() {
        saveDefaultConfig();
        worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "PluginUpdateWatch-IO"); t.setDaemon(true); return t; });
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("pluginupdates")).setTabCompleter(this);
        schedule();
    }
    @Override public void onDisable() { generation++; if (worker != null) worker.shutdownNow(); }
    private void schedule() {
        if (timer != null) timer.cancel();
        long minutes = Math.clamp(getConfig().getLong("check-interval-minutes", 360), 15, 10080);
        timer = getServer().getScheduler().runTaskTimer(this, () -> check(null), 100, minutes * 1200);
    }
    private void sync(Runnable action) {
        if (isEnabled()) {
            try { getServer().getScheduler().runTask(this, action); } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { }
        }
    }
    private void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[Updates] ", NamedTextColor.AQUA).append(Component.text(message, NamedTextColor.GRAY)));
    }
    private List<Remote.Source> sources() {
        List<Remote.Source> list = new ArrayList<>();
        var config = getConfig().getConfigurationSection("plugins");
        if (config == null) return list;
        for (Plugin p : getServer().getPluginManager().getPlugins()) {
            var section = config.getConfigurationSection(p.getName());
            if (section == null || !section.getBoolean("enabled", true)) continue;
            String type = section.getString("source", "spigot");
            list.add(new Remote.Source(p.getName(), p.getPluginMeta().getVersion(), type,
                    section.getString(type.equalsIgnoreCase("github") ? "repository" : "resource-id", ""),
                    section.getString("asset-regex", ".*\\.jar")));
        }
        return list;
    }
    private void check(CommandSender sender) {
        if (checking) { if (sender != null) tell(sender, "A check is already running."); return; }
        List<Remote.Source> sources = sources();
        if (sources.isEmpty()) { if (sender != null) tell(sender, "No installed plugins have configured sources. Edit plugins/PluginUpdateWatch/config.yml, then /pu reload."); return; }
        checking = true;
        int epoch = generation;
        if (sender != null) tell(sender, "Checking " + sources.size() + " plugin(s)...");
        worker.execute(() -> {
            Map<String, Result> checked = new LinkedHashMap<>();
            for (var source : sources) {
                if (Thread.currentThread().isInterrupted()) break;
                Result result;
                try {
                    var release = Remote.latest(source);
                    result = new Result(source, release, Versions.compare(source.installed(), release.version()), null);
                } catch (Exception e) { result = new Result(source, null, null, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()); }
                checked.put(source.name().toLowerCase(Locale.ROOT), result);
            }
            sync(() -> {
                checking = false;
                if (epoch != generation) return;
                boolean changed = !checked.equals(results);
                results.clear(); results.putAll(checked);
                if (sender != null) show(sender);
                if (changed) {
                    show(getServer().getConsoleSender());
                    if (hasUpdates()) for (var player : getServer().getOnlinePlayers())
                        if (player.hasPermission("pluginupdatewatch.admin") && player != sender) tell(player, "Plugin updates are available. Use /pu list.");
                }
            });
        });
    }
    private boolean hasUpdates() { return results.values().stream().anyMatch(r -> r.error() == null && r.status() != Versions.Status.CURRENT); }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("notify-on-join", true) && event.getPlayer().hasPermission("pluginupdatewatch.admin") && hasUpdates())
            tell(event.getPlayer(), "Plugin updates are available. Use /pu list.");
    }
    private void show(CommandSender sender) {
        if (results.isEmpty()) tell(sender, "No results yet. Use /pu check. Only plugins with configured sources are checked.");
        for (Result r : results.values()) {
            if (r.error() != null) { tell(sender, r.source().name() + ": check failed - " + r.error()); continue; }
            String label = switch (r.status()) {
                case CURRENT -> "up to date (or installed version is newer)";
                case UPDATE -> "update available";
                case DIFFERENT -> "different release; verify version ordering";
            };
            Component line = Component.text(r.source().name() + ": " + r.source().installed() + " -> " + r.release().version() + " - " + label, NamedTextColor.GRAY);
            if (r.status() != Versions.Status.CURRENT) {
                line = line.append(Component.text(" [Release page]", NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(r.release().page())));
                if (r.release().download() != null) line = line.append(Component.text(" [Download]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand("/pu download " + r.source().name())));
                else line = line.append(Component.text(" (manual download required)", NamedTextColor.YELLOW));
            }
            sender.sendMessage(line);
        }
        long untracked = Arrays.stream(getServer().getPluginManager().getPlugins()).filter(p -> p != this && !results.containsKey(p.getName().toLowerCase(Locale.ROOT))).count();
        tell(sender, untracked + " other plugin(s) are untracked; add their sources in config.yml.");
    }
    private void download(CommandSender sender, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Result r = results.get(key);
        if (r == null || r.error() != null || r.status() == Versions.Status.CURRENT || r.release().download() == null) {
            tell(sender, "No downloadable update cached for that plugin. Use /pu check or its release page."); return;
        }
        if (!downloads.add(key)) { tell(sender, "That download is already running."); return; }
        tell(sender, "Downloading " + r.source().name() + " " + r.release().version() + "...");
        worker.execute(() -> {
            String message;
            try {
                var path = Remote.download(r.source(), r.release(), getDataFolder().toPath().resolve("downloads"));
                message = "Saved " + path + ". Stop the server, replace the old plugin JAR, then restart. Check the release's Minecraft compatibility first.";
            } catch (Exception e) { message = "Download failed: " + e.getMessage(); }
            String finalMessage = message;
            sync(() -> { downloads.remove(key); tell(sender, finalMessage); getLogger().info(finalMessage); });
        });
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pluginupdatewatch.admin")) { tell(sender, "You do not have permission."); return true; }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> show(sender);
            case "check" -> check(sender);
            case "download" -> { if (args.length == 2) download(sender, args[1]); else tell(sender, "Usage: /pu download <plugin>"); }
            case "reload" -> { reloadConfig(); generation++; results.clear(); schedule(); tell(sender, "Configuration reloaded. Use /pu check."); }
            default -> tell(sender, "Usage: /pu [list|check|download <plugin>|reload]");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pluginupdatewatch.admin")) return List.of();
        List<String> choices = args.length == 1 ? List.of("list", "check", "download", "reload")
                : args.length == 2 && args[0].equalsIgnoreCase("download") ? results.values().stream()
                .filter(r -> r.error() == null && r.status() != Versions.Status.CURRENT && r.release().download() != null).map(r -> r.source().name()).toList() : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
