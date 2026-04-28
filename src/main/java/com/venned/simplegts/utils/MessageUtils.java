package com.venned.simplegts.utils;

import com.venned.simplegts.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static final FileConfiguration config = Main.getInstance().getConfig();

    public static String get(String path) {
        String message = config.getString("messages." + path);
        return ChatColor.translateAlternateColorCodes('&', message != null ? message : "§cMissing message: " + path);
    }

    public static String format(String path, String... replacements) {
        String msg = get(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public static void send(Player player, String path, String... replacements) {
        player.sendMessage(format(path, replacements));
    }
}