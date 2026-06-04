package dev.nxauth.notifications;

import dev.nxauth.NxAuth;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DiscordWebhook {

    private final NxAuth plugin;

    public DiscordWebhook(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void sendAsync(String event, String playerName, String ip, boolean success) {
        if (!plugin.getConfigManager().isDiscordEnabled()) return;
        if (!plugin.getConfigManager().isDiscordEventEnabled(event)) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String color = success ? "3066993" : "15158332"; // Green or Red decimal
            String title = formatEventTitle(event);
            String json = buildEmbed(title, playerName, ip, color, event);
            sendWebhook(json);
        });
    }

    public void sendRawAsync(String message) {
        if (!plugin.getConfigManager().isDiscordEnabled()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String json = "{\"content\":\"" + escapeJson(message) + "\"}";
            sendWebhook(json);
        });
    }

    public void sendDailyStats() {
        if (!plugin.getConfigManager().isDiscordEnabled()) return;
        if (!plugin.getConfigManager().isDiscordEventEnabled("daily-stats")) return;

        long total = plugin.getDatabaseManager().getTotalPlayers();
        int online = plugin.getServer().getOnlinePlayers().size();

        String json = buildEmbed("📊 Daily Stats Report",
            "Server", "N/A", "5765003",
            "Total Registered: " + total + " | Online: " + online);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> sendWebhook(json));
    }

    private String buildEmbed(String title, String player, String ip, String color, String details) {
        return String.format("""
            {
              "embeds": [{
                "title": "%s",
                "color": %s,
                "fields": [
                  {"name": "Player", "value": "%s", "inline": true},
                  {"name": "IP", "value": "%s", "inline": true},
                  {"name": "Details", "value": "%s", "inline": false}
                ],
                "footer": {"text": "NxAuth • %s"},
                "timestamp": "%s"
              }]
            }""",
            escapeJson(title), color,
            escapeJson(player), escapeJson(maskIp(ip)),
            escapeJson(details),
            plugin.getServer().getName(),
            Instant.now().toString()
        );
    }

    private void sendWebhook(String json) {
        try {
            String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL")) return;

            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 204 && code != 200) {
                plugin.getLogger().warning("Discord webhook failed with code: " + code);
            }
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("Discord webhook error: " + e.getMessage());
        }
    }

    private String formatEventTitle(String event) {
        return switch (event) {
            case "new-register" -> "✅ New Player Registered";
            case "login-fail" -> "⚠️ Failed Login Attempt";
            case "max-attempts-reached" -> "🚫 Player Tempbanned";
            case "maintenance-toggle" -> "🔧 Maintenance Mode Changed";
            case "suspicious-activity" -> "🔴 Suspicious Activity";
            case "daily-stats" -> "📊 Daily Stats";
            default -> "ℹ️ " + event;
        };
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) return "N/A";
        String[] parts = ip.split("\\.");
        if (parts.length == 4) return parts[0] + "." + parts[1] + ".***." + "***";
        return ip.substring(0, Math.min(6, ip.length())) + "***";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
