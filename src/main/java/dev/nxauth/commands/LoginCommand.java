package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.AuthManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

// ============================================================
// LoginCommand
// ============================================================
public class LoginCommand implements CommandExecutor {
    private final NxAuth plugin;
    public LoginCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "misc.usage",
                Map.of("usage", "/login <password>"));
            return true;
        }

        // Check captcha first
        if (plugin.getAuthManager().hasPendingCaptcha(player)) {
            plugin.getMessageManager().sendMessage(player, "captcha.prompt",
                Map.of("question", plugin.getAuthManager().getCaptcha(player).question));
            return true;
        }

        AuthManager.LoginResult result = plugin.getAuthManager().login(player, args[0]);

        switch (result) {
            case SUCCESS -> plugin.getMessageManager().sendMessage(player, "login.success",
                Map.of("player", player.getName()));
            case ALREADY_LOGGED_IN -> plugin.getMessageManager().sendMessage(player, "login.already-logged-in");
            case NOT_REGISTERED -> plugin.getMessageManager().sendMessage(player, "login.not-registered");
            case WRONG_PASSWORD -> plugin.getMessageManager().sendMessage(player, "login.wrong-password",
                Map.of("attempts", String.valueOf(plugin.getAuthManager().getRemainingAttempts(player))));
            case MAX_ATTEMPTS -> player.kickPlayer(colorize("&cToo many failed attempts! You have been temporarily banned."));
            case BRUTE_FORCE_BLOCKED -> player.kickPlayer(colorize("&cYour IP is temporarily blocked due to too many failed attempts."));
            case NEEDS_2FA -> plugin.getMessageManager().sendMessage(player, "two-factor.prompt");
        }
        return true;
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}
