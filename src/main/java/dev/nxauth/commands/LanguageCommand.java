package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class LanguageCommand implements CommandExecutor {
    private final NxAuth plugin;
    public LanguageCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (!plugin.getConfigManager().isPerPlayerLanguage()) { player.sendMessage("\u00A7cPer-player language is disabled."); return true; }
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "language.list", Map.of("languages", plugin.getMessageManager().getAvailableLanguages()));
            return true;
        }
        if (!plugin.getMessageManager().isValidLanguage(args[0])) {
            plugin.getMessageManager().sendMessage(player, "language.invalid", Map.of("languages", plugin.getMessageManager().getAvailableLanguages()));
            return true;
        }
        plugin.getMessageManager().setPlayerLanguage(player.getUniqueId(), args[0]);
        plugin.getMessageManager().sendMessage(player, "language.changed");
        return true;
    }
}
