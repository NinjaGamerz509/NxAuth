package dev.nxauth.auth;

import dev.nxauth.NxAuth;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

public class CaptchaManager {

    private final NxAuth plugin;
    private final Random random = new Random();

    public CaptchaManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void sendCaptcha(Player player, String action) {
        if (!plugin.getConfigManager().isCaptchaEnabled()) return;

        AuthManager.CaptchaData captcha = generateMathCaptcha();
        plugin.getAuthManager().setPendingCaptcha(player, captcha);

        plugin.getMessageManager().sendMessage(player, "captcha.prompt",
            Map.of("question", captcha.question, "action", action));
    }

    public boolean verifyCaptcha(Player player, String input) {
        AuthManager.CaptchaData captcha = plugin.getAuthManager().getCaptcha(player);
        if (captcha == null) return true; // No captcha pending

        if (captcha.isExpired()) {
            plugin.getAuthManager().removeCaptcha(player);
            plugin.getMessageManager().sendMessage(player, "captcha.timeout");
            return false;
        }

        try {
            int answer = Integer.parseInt(input.trim());
            if (answer == captcha.answer) {
                plugin.getAuthManager().removeCaptcha(player);
                return true;
            }
        } catch (NumberFormatException ignored) {}

        // Wrong answer
        captcha.fails++;
        int maxFails = plugin.getConfigManager().getCaptchaMaxFails();
        if (captcha.fails >= maxFails) {
            plugin.getAuthManager().removeCaptcha(player);
            player.kickPlayer(colorize("&cFailed captcha too many times!"));
            return false;
        }

        plugin.getMessageManager().sendMessage(player, "captcha.fail",
            Map.of("attempts", String.valueOf(maxFails - captcha.fails)));
        return false;
    }

    private AuthManager.CaptchaData generateMathCaptcha() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        int op = random.nextInt(3); // 0=add, 1=sub, 2=mul

        String question;
        int answer;

        switch (op) {
            case 1 -> {
                // Ensure positive result
                if (a < b) { int tmp = a; a = b; b = tmp; }
                question = a + " - " + b;
                answer = a - b;
            }
            case 2 -> {
                // Keep small for multiplication
                a = random.nextInt(5) + 1;
                b = random.nextInt(5) + 1;
                question = a + " x " + b;
                answer = a * b;
            }
            default -> {
                question = a + " + " + b;
                answer = a + b;
            }
        }

        return new AuthManager.CaptchaData(question, answer, plugin.getConfigManager().getCaptchaTimeout());
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}
