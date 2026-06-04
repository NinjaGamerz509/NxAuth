package dev.nxauth.commands;

import dev.nxauth.NxAuth;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class ChangePasswordCommand implements CommandExecutor {
    private final NxAuth plugin;
    public ChangePasswordCommand(NxAuth plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only!"); return true; }
        if (!plugin.getAuthManager().isLoggedIn(player)) { plugin.getMessageManager().sendMessage(player, "login.prompt"); return true; }
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "misc.usage", Map.of("usage", "/changepassword <oldPassword> <newPassword>"));
            return true;
        }
        String uuid = player.getUniqueId().toString();
        String oldHash = plugin.getDatabaseManager().getPasswordHash(uuid);
        at.favre.lib.crypto.bcrypt.BCrypt.Result verify = at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(args[0].toCharArray(), oldHash);
        if (!verify.verified) { plugin.getMessageManager().sendMessage(player, "changepassword.wrong-old-password"); return true; }
        if (args[0].equals(args[1])) { plugin.getMessageManager().sendMessage(player, "changepassword.same-password"); return true; }
        String newHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, args[1].toCharArray());
        plugin.getDatabaseManager().updatePassword(uuid, newHash);
        plugin.getSessionManager().deleteSession(uuid);
        plugin.getMessageManager().sendMessage(player, "changepassword.success");
        return true;
    }
}
