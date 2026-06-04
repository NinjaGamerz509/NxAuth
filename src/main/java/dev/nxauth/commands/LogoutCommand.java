package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class LogoutCommand implements CommandExecutor {
    private final NxAuth plugin;
    public LogoutCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (plugin.getAuthManager().logout(player)) {
            plugin.getMessageManager().sendMessage(player, "logout.success");
            plugin.getSessionManager().deleteSession(player.getUniqueId().toString());
        } else {
            plugin.getMessageManager().sendMessage(player, "logout.not-logged-in");
        }
        return true;
    }
}
