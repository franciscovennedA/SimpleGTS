package com.venned.simplegts.commands;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.ItemGTS;
import com.venned.simplegts.build.PokemonGTS;
import com.venned.simplegts.gui.ItemMenu;
import com.venned.simplegts.gui.MainMenu;
import com.venned.simplegts.gui.PokemonMenu;
import com.venned.simplegts.utils.CobblemonUtils;
import com.venned.simplegts.utils.MessageUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class MainCommand implements CommandExecutor {

    // Mapa simple de traducción de materiales al español
    private static final Map<Material, String> ESP_TRANSLATIONS = new HashMap<>();
    static {
        ESP_TRANSLATIONS.put(Material.DIAMOND, "Diamante");
        ESP_TRANSLATIONS.put(Material.IRON_INGOT, "Lingote de hierro");
        ESP_TRANSLATIONS.put(Material.COBBLESTONE, "Adoquín");
        // ... agregar más traducciones según necesidad
    }

    private String translate(Material mat) {
        return ESP_TRANSLATIONS.getOrDefault(mat, capitalize(mat.name()));
    }

    private String capitalize(String text) {
        text = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String formatPrice(int price) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        return nf.format(price);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (args.length == 0) {
            MainMenu.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /gts add <pokemon/item>");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "pokemon" -> {
                        if (args.length < 4) {
                            player.sendMessage("§cUsage: /gts add pokemon <slot> <price>");
                            return true;
                        }

                        int slot, price;
                        try {
                            slot = Integer.parseInt(args[2]) - 1;
                            price = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            MessageUtils.send(player, "invalid-number");
                            return true;
                        }

                        PokemonGTS result = CobblemonUtils.savePokemon(player, slot, price);
                        if (result != null) {
                            String formatted = formatPrice(result.getPrice());

                            String messageGlobal = MessageUtils.get("pokemon-added-global");

                            messageGlobal = messageGlobal.replace("%player%", player.getName());
                            messageGlobal = messageGlobal.replace("%pokemon%", result.getSpecie());
                            messageGlobal = messageGlobal.replace("%price%", formatted);

                            Bukkit.broadcastMessage(messageGlobal);


                            MessageUtils.send(player, "pokemon-added-success");
                        } else {
                            MessageUtils.send(player, "pokemon-added-deny");
                        }
                        return true;
                    }
                    case "item" -> {
                        if (args.length < 4) {
                            player.sendMessage("§cUso: /gts add item <price> <slot>");
                            return true;
                        }

                        int price, slot;
                        try {
                            price = Integer.parseInt(args[2]);
                            slot = Integer.parseInt(args[3]) - 1;
                        } catch (NumberFormatException e) {
                            MessageUtils.send(player, "invalid-number");
                            return true;
                        }

                        if (slot < 0 || slot > 8) {
                            MessageUtils.send(player, "invalid-slot");
                            return true;
                        }

                        ItemStack item = player.getInventory().getItem(slot);
                        if (item == null || item.getType() == Material.AIR) {
                            MessageUtils.send(player, "item-slot-empty");
                            return true;
                        }

                        // Crear y registrar el ItemGTS
                        ItemGTS itemGTS = new ItemGTS(item.clone(), price, player.getUniqueId(), System.currentTimeMillis());
                        Main.getInstance().getItemGTSManager().getItemGTSSet().add(itemGTS);

                        // un IChatBaseComponent con translate


                        int amount = item.getAmount();
                        String formatted = formatPrice(price);

                        String key = item.getTranslationKey();
                        TranslatableComponent itemComp = new TranslatableComponent(key);
                        itemComp.addExtra(" x" + amount);
                        itemComp.setColor(ChatColor.GREEN);

                        String prefix = MessageUtils.format("item-added-prefix",
                                "%player%", player.getName());

                        String suffix = MessageUtils.format("item-added-suffix",
                                "%amount%", String.valueOf(amount),
                                "%price%", formatted);

                        TextComponent before = new TextComponent(ChatColor.translateAlternateColorCodes('&', prefix));
                        TextComponent after = new TextComponent(ChatColor.translateAlternateColorCodes('&', suffix));

                        BaseComponent[] message = new BaseComponent[]{ before, itemComp, after };

                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.spigot().sendMessage(message);
                        }

                        player.getInventory().setItem(slot, null);
                        MessageUtils.send(player, "item-added-success");
                        return true;
                    }
                    default -> {
                        player.sendMessage("§cSubcomando desconocido. Usa 'pokemon' o 'item'.");
                        return true;
                    }
                }
            }
            default -> {
                player.sendMessage("§cSubcommand Unknown. Usa /gts add.");
                return true;
            }
        }
    }
}