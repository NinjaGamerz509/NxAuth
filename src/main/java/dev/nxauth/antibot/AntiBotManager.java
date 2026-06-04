// ============================================================
// AntiBotManager.java
// ============================================================
package dev.nxauth.antibot;

import dev.nxauth.NxAuth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiBotManager {

    private final NxAuth plugin;
    private final List<Long> recentJoins = Collections.synchronizedList(new ArrayList<>());
    private boolean locked = false;
    private long lockExpires = 0;

    public AntiBotManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void recordJoin(String ip) {
        if (!plugin.getConfigManager().isAntiBotEnabled()) return;
        long now = System.currentTimeMillis();
        recentJoins.add(now);

        // Count joins in last second
        recentJoins.removeIf(t -> now - t > 1000);
        int count = recentJoins.size();

        if (count >= plugin.getConfigManager().getJoinRateLimit()) {
            triggerLockdown(count);
        }
    }

    private void triggerLockdown(int count) {
        if (locked) return;
        locked = true;
        int duration = plugin.getConfigManager().getLockdownDuration();
        lockExpires = System.currentTimeMillis() + (duration * 1000L);

        plugin.getAdminAlertManager().sendAlert("bot-detection",
            "&4[NxAuth] BOT ATTACK! &c" + count + " joins/second! Lockdown for " + duration + "s");
        plugin.getDiscordWebhook().sendRawAsync("🚨 **BOT ATTACK DETECTED**\n" +
            count + " joins/second! Server locked down for " + duration + "s");

        plugin.getLogger().warning("BOT ATTACK DETECTED! " + count + " joins/second. Lockdown activated.");
    }

    public boolean isLocked() {
        if (!locked) return false;
        if (System.currentTimeMillis() > lockExpires) {
            locked = false;
            plugin.getAdminAlertManager().sendAlert("bot-detection",
                "&a[NxAuth] Server lockdown lifted.");
            return false;
        }
        return true;
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        recentJoins.removeIf(t -> now - t > 5000);
    }
}
