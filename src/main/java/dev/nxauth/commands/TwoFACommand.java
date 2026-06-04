package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class TwoFACommand implements CommandExecutor {
    private final NxAuth plugin;
    public TwoFACommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (!plugin.getAuthManager().isLoggedIn(player)) { plugin.getMessageManager().sendMessage(player, "login.prompt"); return true; }
        if (!plugin.getConfigManager().is2faEnabled()) { player.sendMessage("\u00A7c2FA is disabled."); return true; }
        if (args.length < 1) { player.sendMessage("\u00A7eUsage: \u00A7f/2fa <enable|disable>"); return true; }
        String uuid = player.getUniqueId().toString();
        switch (args[0].toLowerCase()) {
            case "enable" -> {
                String secret = generateSecret();
                plugin.getDatabaseManager().setTotpSecret(uuid, secret);
                player.sendMessage("\u00A7a2FA enabled! Secret: \u00A7f" + secret);
                plugin.getMessageManager().sendMessage(player, "two-factor.enabled");
            }
            case "disable" -> {
                plugin.getDatabaseManager().setTotpSecret(uuid, null);
                plugin.getMessageManager().sendMessage(player, "two-factor.disabled");
            }
            default -> player.sendMessage("\u00A7eUsage: \u00A7f/2fa <enable|disable>");
        }
        return true;
    }

    private String generateSecret() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 32; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }
}
