package dev.nxauth.ui;

import dev.nxauth.NxAuth;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class AnimationManager {

    public static void spawnLoginParticles(Player player, String particleTypeName, int count) {
        try {
            Particle particle = Particle.valueOf(particleTypeName.toUpperCase());
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);

            // Also show to nearby players
            for (Player nearby : player.getWorld().getPlayers()) {
                if (nearby.equals(player)) continue;
                if (nearby.getLocation().distanceSquared(loc) < 400) { // 20 block radius
                    nearby.spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid particle type
        }
    }

    public static void spawnRegisterFirework(Player player) {
        Bukkit.getScheduler().runTask(NxAuth.getInstance(), () -> {
            Location loc = player.getLocation();
            Firework fw = (Firework) loc.getWorld().spawn(loc, Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();

            FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.RED, Color.fromRGB(255, 165, 0), Color.YELLOW)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build();

            meta.addEffect(effect);
            meta.setPower(1);
            fw.setFireworkMeta(meta);

            // Second firework with delay
            Bukkit.getScheduler().runTaskLater(NxAuth.getInstance(), () -> {
                if (!player.isOnline()) return;
                Firework fw2 = (Firework) player.getLocation().getWorld().spawn(player.getLocation(), Firework.class);
                FireworkMeta meta2 = fw2.getFireworkMeta();
                FireworkEffect effect2 = FireworkEffect.builder()
                    .withColor(Color.AQUA, Color.BLUE, Color.PURPLE)
                    .with(FireworkEffect.Type.STAR)
                    .trail(true)
                    .build();
                meta2.addEffect(effect2);
                meta2.setPower(1);
                fw2.setFireworkMeta(meta2);
            }, 15L);
        });
    }

    public static void playCinematic(Player player) {
        NxAuth plugin = NxAuth.getInstance();
        if (!plugin.getConfigManager().isCinematicEnabled()) return;

        // Lock player view and simulate camera pan using teleport
        int duration = plugin.getConfigManager().getCinematicDuration();

        // Apply cinematic effects
        player.sendTitle(colorize("&b&lWelcome"), colorize("&7" + player.getName()), 10, duration, 20);

        // After cinematic, clear title
        Bukkit.getScheduler().runTaskLater(plugin, player::resetTitle, duration + 20L);
    }

    private static String colorize(String s) {
        return s.replace("&", "\u00A7");
    }
}
