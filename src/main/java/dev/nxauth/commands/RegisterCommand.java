package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class RegisterCommand implements CommandExecutor {
    private final NxAuth plugin;
    public RegisterCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "misc.usage", Map.of("usage", "/register <password> <confirmPassword>"));
            return true;
        }
        if (plugin.getAuthManager().hasPendingCaptcha(player)) {
            plugin.getMessageManager().sendMessage(player, "captcha.prompt", Map.of("question", plugin.getAuthManager().getCaptcha(player).question));
            return true;
        }
        dev.nxauth.auth.AuthManager.RegisterResult result = plugin.getAuthManager().register(player, args[0], args[1]);
        switch (result) {
            case SUCCESS -> plugin.getMessageManager().sendMessage(player, "register.success", Map.of("player", player.getName()));
            case ALREADY_LOGGED_IN -> plugin.getMessageManager().sendMessage(player, "login.already-logged-in");
            case ALREADY_REGISTERED -> plugin.getMessageManager().sendMessage(player, "register.already-registered");
            case PASSWORD_MISMATCH -> plugin.getMessageManager().sendMessage(player, "register.password-mismatch");
            case TOO_SHORT -> plugin.getMessageManager().sendMessage(player, "register.password-too-short", Map.of("min", String.valueOf(plugin.getConfigManager().getMinPasswordLength())));
            case TOO_LONG -> plugin.getMessageManager().sendMessage(player, "register.password-too-long", Map.of("max", String.valueOf(plugin.getConfigManager().getMaxPasswordLength())));
            case WEAK_PASSWORD -> plugin.getMessageManager().sendMessage(player, "register.weak-password");
            case IP_LIMIT -> plugin.getMessageManager().sendMessage(player, "register.ip-limit");
            case DATABASE_ERROR -> player.sendMessage("\u00A7cDatabase error!");
        }
        return true;
    }
}
