package dev.nxauth.auth;

import dev.nxauth.NxAuth;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SessionManager {

    private final NxAuth plugin;
    // Track last movement time for AFK detection
    private final Map<UUID, Long> lastMovement = new HashMap<>();

    public SessionManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public boolean hasValidSession(Player player) {
        if (!plugin.getConfigManager().isIpSessionEnabled()) return false;

        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();
        int durationHours = plugin.getConfigManager().getIpSessionDuration();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT ip_address, expires_at FROM nxauth_sessions WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String savedIp = rs.getString("ip_address");
                long expiresAt = rs.getLong("expires_at");

                if (savedIp.equals(ip) && System.currentTimeMillis() < expiresAt) {
                    // Refresh session
                    saveSession(uuid, ip, durationHours);
                    return true;
                } else {
                    // Expired or IP changed
                    deleteSession(uuid);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Session check error", e);
        }
        return false;
    }

    public void saveSession(String uuid, String ip, int durationHours) {
        long expiresAt = System.currentTimeMillis() + (durationHours * 3600000L);
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO nxauth_sessions (uuid, ip_address, created_at, expires_at) VALUES (?,?,?,?)")) {
            ps.setString(1, uuid);
            ps.setString(2, ip);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Save session error", e);
        }
    }

    public void deleteSession(String uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nxauth_sessions WHERE uuid=?")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Delete session error", e);
        }
    }

    public void updateMovement(Player player) {
        lastMovement.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void checkAfkLogout() {
        int afkMinutes = plugin.getConfigManager().getAfkLogoutMinutes();
        if (afkMinutes <= 0) return;
        long threshold = afkMinutes * 60000L;
        long now = System.currentTimeMillis();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getAuthManager().isLoggedIn(player)) continue;
            long last = lastMovement.getOrDefault(player.getUniqueId(), now);
            if (now - last > threshold) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getAuthManager().logout(player);
                    plugin.getMessageManager().sendMessage(player, "logout.success");
                });
            }
        }
    }

    public void saveAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getAuthManager().isLoggedIn(player)) {
                String ip = player.getAddress().getAddress().getHostAddress();
                saveSession(player.getUniqueId().toString(), ip,
                    plugin.getConfigManager().getIpSessionDuration());
            }
        }
    }

    public void removePlayer(Player player) {
        lastMovement.remove(player.getUniqueId());
    }
}
