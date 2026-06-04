package dev.nxauth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nxauth.NxAuth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class WebServer {

    private final NxAuth plugin;
    private Javalin app;
    private final ObjectMapper json = new ObjectMapper();

    // Simple in-memory session store: token -> expiry
    private final Map<String, Long> sessions = new HashMap<>();

    public WebServer(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            int port = plugin.getConfigManager().getWebPort();

            app = Javalin.create(config -> {
                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = "/web";
                    staticFiles.location = Location.CLASSPATH;
                });
                config.plugins.enableCors(cors ->
                    cors.addRule(it -> it.anyHost()));
            }).start(port);

            registerRoutes();
            plugin.getLogger().info("Web dashboard running at http://localhost:" + port);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start web server!", e);
        }
    }

    private void registerRoutes() {

        // ==================== AUTH ====================
        app.post("/api/login", ctx -> {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String password = body.get("password");
            String expected = plugin.getConfigManager().getWebAdminPassword();

            if (expected.equals(password)) {
                String token = UUID.randomUUID().toString().replace("-", "");
                int sessionHours = plugin.getConfigManager().getWebSessionDuration();
                sessions.put(token, System.currentTimeMillis() + (sessionHours * 3600000L));
                ctx.json(Map.of("token", token, "success", true));
            } else {
                ctx.status(401).json(Map.of("error", "Invalid password"));
            }
        });

        // All routes below require auth
        app.before("/api/*", ctx -> {
            if (ctx.path().equals("/api/login")) return;
            String auth = ctx.header("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                ctx.status(401).json(Map.of("error", "Unauthorized"));
                return;
            }
            String token = auth.substring(7);
            Long expiry = sessions.get(token);
            if (expiry == null || System.currentTimeMillis() > expiry) {
                sessions.remove(token);
                ctx.status(401).json(Map.of("error", "Session expired"));
            }
        });

        // ==================== STATS ====================
        app.get("/api/stats", ctx -> {
            long total = plugin.getDatabaseManager().getTotalPlayers();
            int online = plugin.getServer().getOnlinePlayers().size();
            int max = plugin.getServer().getMaxPlayers();
            boolean maintenance = plugin.getMaintenanceManager().isEnabled();
            long failed24h = getFailedLogins24h();

            List<Map<String, Object>> onlineList = new ArrayList<>();
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                Map<String, Object> pm = new HashMap<>();
                pm.put("name", p.getName());
                pm.put("uuid", p.getUniqueId().toString());
                pm.put("authenticated", plugin.getAuthManager().isLoggedIn(p));
                onlineList.add(pm);
            });

            ctx.json(Map.of(
                "totalPlayers", total,
                "onlinePlayers", online,
                "maxPlayers", max,
                "maintenance", maintenance,
                "failedLogins24h", failed24h,
                "onlineList", onlineList,
                "serverName", plugin.getServer().getName(),
                "version", plugin.getDescription().getVersion()
            ));
        });

        // ==================== PLAYERS ====================
        app.get("/api/players", ctx -> {
            String search = ctx.queryParam("search") != null ? ctx.queryParam("search") : "";
            String filter = ctx.queryParam("filter") != null ? ctx.queryParam("filter") : "all";
            List<Map<String, Object>> players = getPlayers(search, filter);
            ctx.json(Map.of("players", players));
        });

        app.get("/api/players/export", ctx -> {
            // Check token in query param for download
            String token = ctx.queryParam("token");
            Long expiry = sessions.get(token);
            if (expiry == null || System.currentTimeMillis() > expiry) {
                ctx.status(401).result("Unauthorized");
                return;
            }
            String csv = exportPlayersCSV();
            ctx.header("Content-Disposition", "attachment; filename=\"nxauth_players.csv\"");
            ctx.contentType("text/csv");
            ctx.result(csv);
        });

        // ==================== LOGS ====================
        app.get("/api/logs", ctx -> {
            String type = ctx.queryParam("type") != null ? ctx.queryParam("type") : "all";
            int limit = ctx.queryParam("limit") != null ? Integer.parseInt(ctx.queryParam("limit")) : 50;
            List<Map<String, Object>> logs = getLogs(type, limit);
            ctx.json(Map.of("logs", logs));
        });

        // ==================== CONFIG ====================
        app.get("/api/config", ctx -> {
            // Return config as flat map
            Map<String, Object> configMap = flattenConfig();
            ctx.json(Map.of("config", configMap));
        });

        app.post("/api/config", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Map<String, Object> updates = (Map<String, Object>) body.get("updates");
            if (updates != null) {
                updates.forEach((key, value) -> plugin.getConfigManager().set(key, value));
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.reload());
            }
            ctx.json(Map.of("success", true));
        });

        // ==================== BACKUP ====================
        app.get("/api/backups", ctx -> {
            List<String> rawList = plugin.getBackupManager().listBackups();
            List<Map<String, String>> backups = new ArrayList<>();
            for (String entry : rawList) {
                String[] parts = entry.split(" \\(");
                Map<String, String> b = new HashMap<>();
                b.put("name", parts[0]);
                b.put("size", parts.length > 1 ? parts[1].replace(")", "") : "?");
                b.put("created", extractDateFromFilename(parts[0]));
                backups.add(b);
            }
            ctx.json(Map.of("backups", backups));
        });

        app.post("/api/backup/create", ctx -> {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getBackupManager().createBackup();
            });
            ctx.json(Map.of("success", true, "message", "Backup started"));
        });

        // ==================== SECURITY ====================
        app.get("/api/security", ctx -> {
            List<Map<String, Object>> ipBans = getIpBans();
            ctx.json(Map.of("ipBans", ipBans));
        });

        app.post("/api/security/ipban", ctx -> {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String ip = body.get("ip");
            if (ip != null && !ip.isEmpty()) {
                plugin.getDatabaseManager().addIpBan(ip, "Web dashboard ban", "WebAdmin", 0);
            }
            ctx.json(Map.of("success", true));
        });

        app.post("/api/security/ipunban", ctx -> {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String ip = body.get("ip");
            if (ip != null) plugin.getDatabaseManager().removeIpBan(ip);
            ctx.json(Map.of("success", true));
        });

        // ==================== MAINTENANCE ====================
        app.post("/api/maintenance", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            boolean enabled = (Boolean) body.get("enabled");
            plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getMaintenanceManager().setEnabled(enabled,
                    plugin.getServer().getConsoleSender()));
            ctx.json(Map.of("success", true, "maintenance", enabled));
        });

        // ==================== ACTIONS ====================
        app.post("/api/action", ctx -> {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String action = body.get("action");
            String playerName = body.get("player");

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                switch (action != null ? action : "") {
                    case "reload" -> plugin.reload();
                    case "forcelogin" -> {
                        var p = plugin.getServer().getPlayer(playerName);
                        if (p != null) {
                            plugin.getAuthManager().setLoggedIn(p, true);
                            new dev.nxauth.auth.JoinScreenManager(plugin).onLoginSuccess(p);
                        }
                    }
                    case "kick" -> {
                        var p = plugin.getServer().getPlayer(playerName);
                        if (p != null) p.kickPlayer(colorize("&cKicked by admin via web dashboard."));
                    }
                    case "unregister" -> {
                        var p = plugin.getServer().getPlayer(playerName);
                        if (p != null) {
                            plugin.getDatabaseManager().unregisterPlayer(p.getUniqueId().toString());
                            plugin.getAuthManager().setLoggedIn(p, false);
                        }
                    }
                    case "reset2fa" -> {
                        var p = plugin.getServer().getPlayer(playerName);
                        if (p != null)
                            plugin.getDatabaseManager().setTotpSecret(p.getUniqueId().toString(), null);
                    }
                }
            });
            ctx.json(Map.of("success", true));
        });
    }

    // ==================== HELPERS ====================

    private long getFailedLogins24h() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM nxauth_logs WHERE event_type='LOGIN_FAIL' AND timestamp > ?")) {
            ps.setLong(1, System.currentTimeMillis() - 86400000L);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "getFailedLogins24h error", e);
        }
        return 0;
    }

    private List<Map<String, Object>> getPlayers(String search, String filter) {
        List<Map<String, Object>> list = new ArrayList<>();
        Set<String> onlineNames = new HashSet<>();
        plugin.getServer().getOnlinePlayers().forEach(p -> onlineNames.add(p.getName().toLowerCase()));

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT uuid, username, last_login, ip_address, totp_enabled, is_banned FROM nxauth_players";
            if (!search.isEmpty()) sql += " WHERE username LIKE ?";
            sql += " ORDER BY last_login DESC LIMIT 100";

            PreparedStatement ps = conn.prepareStatement(sql);
            if (!search.isEmpty()) ps.setString(1, "%" + search + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                boolean online = onlineNames.contains(rs.getString("username").toLowerCase());
                if (filter.equals("online") && !online) continue;
                if (filter.equals("banned") && !rs.getBoolean("is_banned")) continue;

                Map<String, Object> p = new HashMap<>();
                p.put("uuid", rs.getString("uuid"));
                p.put("username", rs.getString("username"));
                p.put("lastLogin", rs.getLong("last_login"));
                p.put("ip", rs.getString("ip_address"));
                p.put("twofa", rs.getBoolean("totp_enabled"));
                p.put("banned", rs.getBoolean("is_banned"));
                p.put("online", online);
                list.add(p);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "getPlayers error", e);
        }
        return list;
    }

    private List<Map<String, Object>> getLogs(String type, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT username, event_type, ip_address, success, timestamp, details FROM nxauth_logs";
            if (!type.equals("all")) sql += " WHERE event_type=?";
            sql += " ORDER BY timestamp DESC LIMIT ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            if (!type.equals("all")) {
                ps.setString(1, type);
                ps.setInt(2, limit);
            } else {
                ps.setInt(1, limit);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> log = new HashMap<>();
                log.put("username", rs.getString("username"));
                log.put("eventType", rs.getString("event_type"));
                log.put("ip", rs.getString("ip_address"));
                log.put("success", rs.getBoolean("success"));
                log.put("timestamp", rs.getLong("timestamp"));
                log.put("details", rs.getString("details"));
                list.add(log);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "getLogs error", e);
        }
        return list;
    }

    private List<Map<String, Object>> getIpBans() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ip_address, reason, banned_at, expires_at FROM nxauth_ipbans LIMIT 100")) {
            while (rs.next()) {
                Map<String, Object> ban = new HashMap<>();
                ban.put("ip", rs.getString("ip_address"));
                ban.put("reason", rs.getString("reason"));
                ban.put("bannedAt", rs.getLong("banned_at"));
                ban.put("expiresAt", rs.getLong("expires_at"));
                list.add(ban);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "getIpBans error", e);
        }
        return list;
    }

    private String exportPlayersCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append("UUID,Username,LastLogin,IP,2FA,Registered\n");
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, username, last_login, ip_address, totp_enabled, registered_at FROM nxauth_players ORDER BY username")) {
            while (rs.next()) {
                sb.append(rs.getString("uuid")).append(",")
                  .append(rs.getString("username")).append(",")
                  .append(new java.util.Date(rs.getLong("last_login"))).append(",")
                  .append(rs.getString("ip_address") != null ? rs.getString("ip_address") : "").append(",")
                  .append(rs.getBoolean("totp_enabled")).append(",")
                  .append(new java.util.Date(rs.getLong("registered_at"))).append("\n");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "exportCSV error", e);
        }
        return sb.toString();
    }

    private Map<String, Object> flattenConfig() {
        Map<String, Object> flat = new LinkedHashMap<>();
        org.bukkit.configuration.ConfigurationSection cfg = plugin.getConfigManager().getRaw();
        flattenSection(cfg, "", flat);
        return flat;
    }

    private void flattenSection(org.bukkit.configuration.ConfigurationSection section, String prefix, Map<String, Object> result) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object val = section.get(key);
            if (val instanceof org.bukkit.configuration.ConfigurationSection sub) {
                flattenSection(sub, fullKey, result);
            } else {
                result.put(fullKey, val);
            }
        }
    }

    private String extractDateFromFilename(String name) {
        try {
            String dateStr = name.replace("nxauth_backup_", "").replace(".zip", "").replace(".db", "");
            return dateStr.replace("_", " ");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }

    public void stop() {
        if (app != null) {
            app.stop();
            plugin.getLogger().info("Web dashboard stopped.");
        }
    }
}
