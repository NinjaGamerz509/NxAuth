package dev.nxauth.ui;

import dev.nxauth.NxAuth;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

public class BossBarManager {

    private final NxAuth plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Long> startTimes = new HashMap<>();

    // Color cycle for animation
    private final BarColor[] colors = {BarColor.GREEN, BarColor.YELLOW, BarColor.RED};
    private int colorIndex = 0;
    private int tickCount = 0;

    public BossBarManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void showBossBar(Player player) {
        if (!plugin.getConfigManager().isBossBarEnabled()) return;

        // Remove old bossbar if exists
        removeBossBar(player);

        String isRegistered = plugin.getDatabaseManager().isRegistered(player.getUniqueId().toString())
            ? "login" : "register";

        int timeout = plugin.getConfigManager().getLoginTimeout();
        String title = colorize(plugin.getMessageManager().get(
            "bossbar." + isRegistered, player.getUniqueId().toString(),
            Map.of("time", String.valueOf(timeout))));

        BossBar bar = Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        bar.addPlayer(player);
        bar.setVisible(true);

        bossBars.put(player.getUniqueId(), bar);
        startTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
        startTimes.remove(player.getUniqueId());
    }

    public void removeAll() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
        startTimes.clear();
    }

    // Called every N ticks by scheduler
    public void tick() {
        tickCount++;
        int timeout = plugin.getConfigManager().getLoginTimeout();
        boolean animate = plugin.getConfigManager().isBossBarAnimated();

        // Cycle color for animation
        if (animate && tickCount % 20 == 0) {
            colorIndex = (colorIndex + 1) % colors.length;
        }

        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            UUID uuid = entry.getKey();
            BossBar bar = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                toRemove.add(uuid);
                continue;
            }

            long elapsed = (System.currentTimeMillis() - startTimes.getOrDefault(uuid, System.currentTimeMillis())) / 1000;
            long remaining = timeout - elapsed;

            if (remaining <= 0) {
                // Timeout - kick player
                toRemove.add(uuid);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.kickPlayer(colorize(plugin.getMessageManager().get("login.timeout", player)));
                    }
                });
                continue;
            }

            // Update progress
            double progress = Math.max(0, (double) remaining / timeout);
            bar.setProgress(progress);

            // Update color based on remaining time
            if (remaining > timeout * 0.6) {
                bar.setColor(BarColor.GREEN);
            } else if (remaining > timeout * 0.3) {
                bar.setColor(BarColor.YELLOW);
            } else {
                bar.setColor(animate ? colors[colorIndex] : BarColor.RED);
            }

            // Update title
            String isRegistered = plugin.getDatabaseManager().isRegistered(uuid.toString())
                ? "login" : "register";
            String title = colorize(plugin.getMessageManager().get(
                "bossbar." + isRegistered, uuid.toString(),
                Map.of("time", String.valueOf(remaining))));
            bar.setTitle(title);

            // Warning sound at 10 seconds
            if (remaining == 10) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        SoundManager.playSound(player,
                            plugin.getConfigManager().getSoundTimeoutWarning(),
                            plugin.getConfigManager().getSoundVolume(),
                            plugin.getConfigManager().getSoundPitch());
                    }
                });
            }
        }

        // Cleanup
        for (UUID uuid : toRemove) {
            BossBar bar = bossBars.remove(uuid);
            if (bar != null) bar.removeAll();
            startTimes.remove(uuid);
        }
    }

    private String colorize(String msg) {
        return msg.replace("&", "\u00A7");
    }
}
