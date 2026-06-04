package dev.nxauth.premium;

import dev.nxauth.NxAuth;
import org.bukkit.entity.Player;

public class PremiumManager {

    private final NxAuth plugin;
    private boolean geyserPresent = false;
    private boolean floodgatePresent = false;

    public PremiumManager(NxAuth plugin) {
        this.plugin = plugin;
        checkDependencies();
    }

    private void checkDependencies() {
        geyserPresent = plugin.getServer().getPluginManager().getPlugin("Geyser-Spigot") != null;
        floodgatePresent = plugin.getServer().getPluginManager().getPlugin("floodgate") != null;

        if (geyserPresent) plugin.getLogger().info("Geyser detected - Bedrock auto-login enabled!");
        if (floodgatePresent) plugin.getLogger().info("Floodgate detected - Bedrock UUID support enabled!");
    }

    public boolean isBedrockPlayer(Player player) {
        if (!plugin.getConfigManager().isBedrockAutoLogin()) return false;

        // Check via Floodgate API if available
        if (floodgatePresent) {
            try {
                Class<?> floodgateApi = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object api = floodgateApi.getMethod("getInstance").invoke(null);
                return (boolean) floodgateApi.getMethod("isFloodgatePlayer", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
            } catch (Exception ignored) {}
        }

        // Fallback: check username prefix
        String prefix = plugin.getConfigManager().getBedrockPrefix();
        return player.getName().startsWith(prefix);
    }

    public boolean isPremiumPlayer(Player player) {
        if (!plugin.getConfigManager().isPremiumEnabled()) return false;

        // Online-mode servers: all players are premium
        if (plugin.getServer().getOnlineMode()) return true;

        // Offline-mode: check if UUID matches Mojang's online UUID format
        // (online UUIDs are version 3, offline are version 3 too but different namespace)
        // Best check: if player has nxauth.premium.bypass permission from an auth plugin
        return player.hasPermission("nxauth.premium.bypass");
    }
}
