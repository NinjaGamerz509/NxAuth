package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final NxAuth plugin;

    public AdminCommand(NxAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("nxauth.admin")) {
            sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + "&cNo permission!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + "&aConfiguration reloaded!"));
            }
            case "forcelogin" -> {
                if (args.length < 2) { sender.sendMessage(colorize("&cUsage: /nxauth forcelogin <player>")); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(colorize("&cPlayer not found!")); return true; }
                plugin.getAuthManager().setLoggedIn(target, true);
                new dev.nxauth.auth.JoinScreenManager(plugin).onLoginSuccess(target);
                sender.sendMessage(colorize("&aForce logged in &f" + target.getName()));
                target.sendMessage(colorize("&aYou have been force-logged in by an admin."));
            }
            case "unregister" -> {
                if (args.length < 2) { sender.sendMessage(colorize("&cUsage: /nxauth unregister <player>")); return true; }
                String name = args[1];
                // Try online player first
                Player target = plugin.getServer().getPlayer(name);
                String uuid = target != null ? target.getUniqueId().toString() : null;
                if (uuid == null) { sender.sendMessage(colorize("&cPlayer must be online to unregister.")); return true; }
                plugin.getDatabaseManager().unregisterPlayer(uuid);
                plugin.getAuthManager().setLoggedIn(target, false);
                sender.sendMessage(colorize("&aUnregistered &f" + name));
            }
            case "tempban" -> {
                if (args.length < 3) { sender.sendMessage(colorize("&cUsage: /nxauth tempban <player> <minutes>")); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(colorize("&cPlayer not found!")); return true; }
                try {
                    int minutes = Integer.parseInt(args[2]);
                    String ip = target.getAddress().getAddress().getHostAddress();
                    long expires = System.currentTimeMillis() + (minutes * 60000L);
                    plugin.getDatabaseManager().addIpBan(ip, "Admin tempban", sender.getName(), expires);
                    target.kickPlayer(colorize("&cYou have been temporarily banned for " + minutes + " minutes."));
                    sender.sendMessage(colorize("&aTempbanned &f" + target.getName() + " &afor &f" + minutes + " &aminutes."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(colorize("&cInvalid number!"));
                }
            }
            case "ipban" -> {
                if (args.length < 2) { sender.sendMessage(colorize("&cUsage: /nxauth ipban <ip>")); return true; }
                String ip = args[1];
                plugin.getDatabaseManager().addIpBan(ip, "Admin ban", sender.getName(), 0);
                sender.sendMessage(colorize("&aIP banned: &f" + ip));
            }
            case "ipunban" -> {
                if (args.length < 2) { sender.sendMessage(colorize("&cUsage: /nxauth ipunban <ip>")); return true; }
                plugin.getDatabaseManager().removeIpBan(args[1]);
                sender.sendMessage(colorize("&aIP unbanned: &f" + args[1]));
            }
            case "maintenance" -> {
                if (args.length < 2) {
                    boolean current = plugin.getMaintenanceManager().isEnabled();
                    sender.sendMessage(colorize("&eMaintenance is currently: " + (current ? "&cON" : "&aOFF")));
                    return true;
                }
                if (args[1].equalsIgnoreCase("on")) {
                    plugin.getMaintenanceManager().setEnabled(true, sender);
                } else if (args[1].equalsIgnoreCase("off")) {
                    plugin.getMaintenanceManager().setEnabled(false, sender);
                }
            }
            case "stats" -> {
                long total = plugin.getDatabaseManager().getTotalPlayers();
                int online = plugin.getServer().getOnlinePlayers().size();
                sender.sendMessage(colorize("&b=== NxAuth Stats ==="));
                sender.sendMessage(colorize("&7Total Registered: &f" + total));
                sender.sendMessage(colorize("&7Online Players: &f" + online));
                sender.sendMessage(colorize("&7DB Type: &f" + plugin.getDatabaseManager().getDbType().toUpperCase()));
                sender.sendMessage(colorize("&7Maintenance: &f" + (plugin.getMaintenanceManager().isEnabled() ? "&cON" : "&aOFF")));
            }
            case "backup" -> {
                sender.sendMessage(colorize("&eStarting manual backup..."));
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getBackupManager().createBackup();
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage(colorize(success ? "&aBackup created successfully!" : "&cBackup failed!")));
                });
            }
            case "2fa" -> {
                if (args.length < 3) { sender.sendMessage(colorize("&cUsage: /nxauth 2fa reset <player>")); return true; }
                if (args[1].equalsIgnoreCase("reset")) {
                    Player target = plugin.getServer().getPlayer(args[2]);
                    if (target == null) { sender.sendMessage(colorize("&cPlayer not found!")); return true; }
                    plugin.getDatabaseManager().setTotpSecret(target.getUniqueId().toString(), null);
                    sender.sendMessage(colorize("&aReset 2FA for &f" + target.getName()));
                }
            }
            case "version" -> {
                sender.sendMessage(colorize("&bNxAuth &fv" + plugin.getDescription().getVersion()));
                sender.sendMessage(colorize("&7Database: &f" + plugin.getDatabaseManager().getDbType().toUpperCase()));
                sender.sendMessage(colorize("&7Web Dashboard: &f" +
                    (plugin.getConfigManager().isWebDashboardEnabled() ? "&aEnabled" : "&cDisabled")));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&b=== NxAuth Admin Commands ==="));
        sender.sendMessage(colorize("&f/nxauth reload &7- Reload config"));
        sender.sendMessage(colorize("&f/nxauth forcelogin <player> &7- Force login"));
        sender.sendMessage(colorize("&f/nxauth unregister <player> &7- Delete account"));
        sender.sendMessage(colorize("&f/nxauth tempban <player> <min> &7- Temp ban"));
        sender.sendMessage(colorize("&f/nxauth ipban <ip> &7- Ban IP"));
        sender.sendMessage(colorize("&f/nxauth ipunban <ip> &7- Unban IP"));
        sender.sendMessage(colorize("&f/nxauth maintenance <on|off> &7- Toggle maintenance"));
        sender.sendMessage(colorize("&f/nxauth stats &7- View stats"));
        sender.sendMessage(colorize("&f/nxauth backup &7- Create backup"));
        sender.sendMessage(colorize("&f/nxauth 2fa reset <player> &7- Reset 2FA"));
        sender.sendMessage(colorize("&f/nxauth version &7- Plugin info"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("nxauth.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return List.of("reload", "forcelogin", "unregister", "tempban", "ipban",
                "ipunban", "maintenance", "stats", "backup", "2fa", "version");
        }
        if (args.length == 2) {
            if (List.of("forcelogin", "unregister", "tempban").contains(args[0].toLowerCase())) {
                List<String> players = new ArrayList<>();
                plugin.getServer().getOnlinePlayers().forEach(p -> players.add(p.getName()));
                return players;
            }
            if (args[0].equalsIgnoreCase("maintenance")) return List.of("on", "off");
            if (args[0].equalsIgnoreCase("2fa")) return List.of("reset");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("2fa") && args[1].equalsIgnoreCase("reset")) {
            List<String> players = new ArrayList<>();
            plugin.getServer().getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }
        return Collections.emptyList();
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}
