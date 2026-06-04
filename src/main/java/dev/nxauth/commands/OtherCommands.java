package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.AuthManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

// ============================================================
// RegisterCommand
// ============================================================
class RegisterCommand implements CommandExecutor {
    private final NxAuth plugin;
    RegisterCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "misc.usage",
                Map.of("usage", "/register <password> <confirmPassword>"));
            return true;
        }

        // Check captcha first
        if (plugin.getAuthManager().hasPendingCaptcha(player)) {
            plugin.getMessageManager().sendMessage(player, "captcha.prompt",
                Map.of("question", plugin.getAuthManager().getCaptcha(player).question));
            return true;
        }

        AuthManager.RegisterResult result = plugin.getAuthManager().register(player, args[0], args[1]);

        switch (result) {
            case SUCCESS -> plugin.getMessageManager().sendMessage(player, "register.success",
                Map.of("player", player.getName()));
            case ALREADY_LOGGED_IN -> plugin.getMessageManager().sendMessage(player, "login.already-logged-in");
            case ALREADY_REGISTERED -> plugin.getMessageManager().sendMessage(player, "register.already-registered");
            case PASSWORD_MISMATCH -> plugin.getMessageManager().sendMessage(player, "register.password-mismatch");
            case TOO_SHORT -> plugin.getMessageManager().sendMessage(player, "register.password-too-short",
                Map.of("min", String.valueOf(plugin.getConfigManager().getMinPasswordLength())));
            case TOO_LONG -> plugin.getMessageManager().sendMessage(player, "register.password-too-long",
                Map.of("max", String.valueOf(plugin.getConfigManager().getMaxPasswordLength())));
            case WEAK_PASSWORD -> plugin.getMessageManager().sendMessage(player, "register.weak-password");
            case IP_LIMIT -> plugin.getMessageManager().sendMessage(player, "register.ip-limit");
            case DATABASE_ERROR -> player.sendMessage(colorize("&cDatabase error! Please contact an admin."));
        }
        return true;
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}

// ============================================================
// LogoutCommand
// ============================================================
class LogoutCommand implements CommandExecutor {
    private final NxAuth plugin;
    LogoutCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }

        if (plugin.getAuthManager().logout(player)) {
            plugin.getMessageManager().sendMessage(player, "logout.success");
            // Delete session so they need to re-login
            plugin.getSessionManager().deleteSession(player.getUniqueId().toString());
        } else {
            plugin.getMessageManager().sendMessage(player, "logout.not-logged-in");
        }
        return true;
    }
}

// ============================================================
// ChangePasswordCommand
// ============================================================
class ChangePasswordCommand implements CommandExecutor {
    private final NxAuth plugin;
    ChangePasswordCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (!plugin.getAuthManager().isLoggedIn(player)) {
            plugin.getMessageManager().sendMessage(player, "login.prompt"); return true;
        }
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "misc.usage",
                Map.of("usage", "/changepassword <oldPassword> <newPassword>"));
            return true;
        }

        String uuid = player.getUniqueId().toString();
        String oldHash = plugin.getDatabaseManager().getPasswordHash(uuid);
        at.favre.lib.crypto.bcrypt.BCrypt.Result verify =
            at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(args[0].toCharArray(), oldHash);

        if (!verify.verified) {
            plugin.getMessageManager().sendMessage(player, "changepassword.wrong-old-password");
            return true;
        }
        if (args[0].equals(args[1])) {
            plugin.getMessageManager().sendMessage(player, "changepassword.same-password");
            return true;
        }

        String newHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, args[1].toCharArray());
        plugin.getDatabaseManager().updatePassword(uuid, newHash);
        // Invalidate all sessions
        plugin.getSessionManager().deleteSession(uuid);
        plugin.getMessageManager().sendMessage(player, "changepassword.success");
        plugin.getDatabaseManager().logEvent(uuid, player.getName(), "PASSWORD_CHANGE",
            player.getAddress().getAddress().getHostAddress(), true, "");
        return true;
    }
}

// ============================================================
// LanguageCommand
// ============================================================
class LanguageCommand implements CommandExecutor {
    private final NxAuth plugin;
    LanguageCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (!plugin.getConfigManager().isPerPlayerLanguage()) {
            player.sendMessage(colorize("&cPer-player language is disabled.")); return true;
        }
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "language.list",
                Map.of("languages", plugin.getMessageManager().getAvailableLanguages()));
            return true;
        }
        if (!plugin.getMessageManager().isValidLanguage(args[0])) {
            plugin.getMessageManager().sendMessage(player, "language.invalid",
                Map.of("languages", plugin.getMessageManager().getAvailableLanguages()));
            return true;
        }
        plugin.getMessageManager().setPlayerLanguage(player.getUniqueId(), args[0]);
        plugin.getMessageManager().sendMessage(player, "language.changed");
        return true;
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}

// ============================================================
// TwoFACommand
// ============================================================
class TwoFACommand implements CommandExecutor {
    private final NxAuth plugin;
    TwoFACommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (!plugin.getAuthManager().isLoggedIn(player)) {
            plugin.getMessageManager().sendMessage(player, "login.prompt"); return true;
        }
        if (!plugin.getConfigManager().is2faEnabled()) {
            player.sendMessage(colorize("&c2FA is disabled on this server.")); return true;
        }
        if (args.length < 1) {
            player.sendMessage(colorize("&eUsage: &f/2fa <enable|disable|backup|<code>>"));
            return true;
        }

        String uuid = player.getUniqueId().toString();

        if (plugin.getAuthManager().isPending2FA(player)) {
            // Player is verifying 2FA code after login
            // TODO: Verify TOTP code
            plugin.getMessageManager().sendMessage(player, "two-factor.success");
            plugin.getAuthManager().completeLogin(player, player.getAddress().getAddress().getHostAddress());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable" -> {
                if (!plugin.getConfigManager().isPlayerToggle2fa()) {
                    player.sendMessage(colorize("&cYou cannot toggle 2FA yourself.")); return true;
                }
                // Generate TOTP secret and show QR code URL
                String secret = generateTotpSecret();
                plugin.getDatabaseManager().setTotpSecret(uuid, secret);
                player.sendMessage(colorize("&a2FA enabled! Check the web dashboard for your QR code."));
                player.sendMessage(colorize("&7Secret: &f" + secret));
                plugin.getMessageManager().sendMessage(player, "two-factor.enabled");
            }
            case "disable" -> {
                if (!plugin.getConfigManager().isPlayerToggle2fa()) {
                    player.sendMessage(colorize("&cYou cannot toggle 2FA yourself.")); return true;
                }
                plugin.getDatabaseManager().setTotpSecret(uuid, null);
                plugin.getMessageManager().sendMessage(player, "two-factor.disabled");
            }
            default -> player.sendMessage(colorize("&eUsage: &f/2fa <enable|disable|backup>"));
        }
        return true;
    }

    private String generateTotpSecret() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 32; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}
