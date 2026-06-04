package dev.nxauth.auth;

import dev.nxauth.NxAuth;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinScreenManager {

    private final NxAuth plugin;
    // Saved inventories before hiding
    private static final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private static final Map<UUID, Location> savedLocations = new HashMap<>();

    public JoinScreenManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void applyAuthRestrictions(Player player) {
        if (!plugin.getConfigManager().isJoinScreenEnabled()) return;

        // Save current location
        savedLocations.put(player.getUniqueId(), player.getLocation());

        // Teleport to auth location
        if (plugin.getConfigManager().isAuthLocationEnabled()) {
            teleportToAuthLocation(player);
        }

        // Apply effects
        if (plugin.getConfigManager().isBlindnessEnabled()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        }

        if (plugin.getConfigManager().isFreezeEnabled()) {
            // Will be enforced in PlayerMoveListener
        }

        // Hide inventory
        if (plugin.getConfigManager().isHideInventory()) {
            savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
            player.getInventory().clear();
        }

        // Hide from other players
        if (plugin.getConfigManager().isHideFromOthers()) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(player) && plugin.getAuthManager().isLoggedIn(online)) {
                    online.hidePlayer(plugin, player);
                }
                // Hide logged-in players from unlogged player
                if (!plugin.getAuthManager().isLoggedIn(online)) continue;
                player.hidePlayer(plugin, online);
            }
        }

        // Show login/register prompt
        player.sendTitle(
            colorize("&b&lNxAuth"),
            colorize(plugin.getDatabaseManager().isRegistered(player.getUniqueId().toString())
                ? "&eType &f/login <password> &eto continue"
                : "&eType &f/register <pass> <confirm> &eto join"),
            10, 200, 10
        );
    }

    public void onLoginSuccess(Player player) {
        // Remove effects
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        // Restore inventory
        if (savedInventories.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventories.remove(player.getUniqueId()));
        }

        // Restore location or leave at auth spot
        // (server owner can choose via config - for now stay at auth spawn)

        // Show to all players and vice versa
        if (plugin.getConfigManager().isHideFromOthers()) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
                player.showPlayer(plugin, online);
            }
        }

        // Clear title
        player.resetTitle();

        // Welcome actionbar
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendActionBar(colorize("&aWelcome, &f" + player.getName() + "&a! You are now logged in."));
        }, 5L);

        savedLocations.remove(player.getUniqueId());
    }

    public void onPlayerQuit(Player player) {
        savedInventories.remove(player.getUniqueId());
        savedLocations.remove(player.getUniqueId());
    }

    public boolean isFrozen(Player player) {
        return !plugin.getAuthManager().isLoggedIn(player)
            && plugin.getConfigManager().isFreezeEnabled()
            && plugin.getConfigManager().isJoinScreenEnabled();
    }

    private void teleportToAuthLocation(Player player) {
        String worldName = plugin.getConfigManager().getAuthWorld();
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) world = player.getWorld(); // fallback

        Location authLoc = new Location(world,
            plugin.getConfigManager().getAuthX(),
            plugin.getConfigManager().getAuthY(),
            plugin.getConfigManager().getAuthZ());

        player.teleport(authLoc);
    }

    private String colorize(String msg) {
        return msg.replace("&", "\u00A7");
    }
}
