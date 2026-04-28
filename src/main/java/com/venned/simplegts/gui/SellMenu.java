package com.venned.simplegts.gui;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.ItemGTS;
import com.venned.simplegts.build.PokemonGTS;
import com.venned.simplegts.utils.CobblemonUtils;
import com.venned.simplegts.utils.EconomyUtils;
import com.venned.simplegts.utils.NameSpaceUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SellMenu implements Listener {

    private static final Map<UUID, Integer> playerPages = new HashMap<>(); // Guardar páginas por jugador
    private static final int ITEMS_PER_PAGE = 45; // 5 líneas de 9 ítems, 1 línea reservada para botones

    public SellMenu(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        // Unimos los dos tipos
        List<Object> itemsForSale = new ArrayList<>();

        List<ItemGTS> itemList = Main.getInstance().getItemGTSManager().getItemGTSSet()
                .stream().filter(p -> p.getOwner().equals(player.getUniqueId())).toList();
        List<PokemonGTS> pokemonList = Main.getInstance().getPokemonGTSManager().getPokemonGTSSet()
                .stream().filter(p -> p.getOwner().equals(player.getUniqueId())).toList();

        itemsForSale.addAll(itemList);
        itemsForSale.addAll(pokemonList);

        int totalPages = (int) Math.ceil((double) itemsForSale.size() / ITEMS_PER_PAGE);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, "Manage Sales - Page " + (page + 1));

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, itemsForSale.size());

        for (int i = start; i < end; i++) {
            Object obj = itemsForSale.get(i);
            if (obj instanceof ItemGTS) {
                ItemStack item = buildItemGTSItem((ItemGTS) obj);
                inv.addItem(item);
            } else if (obj instanceof PokemonGTS) {
                ItemStack item = buildPokemonGTSItem((PokemonGTS) obj);
                inv.addItem(item);
            }
        }

        if (page > 0) {
            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta meta = previous.getItemMeta();
            meta.setDisplayName("§ePrevious Page");
            previous.setItemMeta(meta);
            inv.setItem(45, previous);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName("§eNext Page");
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }

        ItemStack home = new ItemStack(Material.BARRIER);
        ItemMeta homeMeta = home.getItemMeta();
        homeMeta.setDisplayName("§cReturn to Menu");
        home.setItemMeta(homeMeta);
        inv.setItem(49, home);

        player.openInventory(inv);
    }

    private static ItemStack buildPokemonGTSItem(PokemonGTS key) {
        // Aquí puedes construir tu representación del Pokémon
        ItemStack item = CobblemonUtils.getItemPokemon(key.getSpecie(), key.isShiny());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Pokémon: §e" + key.getSpecie());
            lore.add("§7Level: §e" + key.getLevel());
            lore.add("§7Price: §a阿" + key.getPrice());
            lore.add("§cClick to delete");
            lore.add(getRemainingTimeLore(key.getPutInMerch()));

            meta.setDisplayName("§b" + key.getSpecie());
            meta.getPersistentDataContainer().set(NameSpaceUtils.owner, PersistentDataType.STRING, key.getOwner().toString());
            meta.getPersistentDataContainer().set(NameSpaceUtils.buyTime, PersistentDataType.LONG, key.getPutInMerch());
            meta.getPersistentDataContainer().set(NameSpaceUtils.pokemonBuy, PersistentDataType.STRING, key.getSpecie());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack buildItemGTSItem(ItemGTS key) {
        // Es igual al buildPokemonItem que ya tenías (lo adaptamos)
        ItemStack item = key.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            lore.add(" ");
            lore.add("§7Owner: §e" + Bukkit.getOfflinePlayer(key.getOwner()).getName());
            lore.add("§7Price: §a阿" + key.getMoney());
            lore.add("§cClick to delete");
            lore.add(getRemainingTimeLore(key.getPutInMerch()));

            meta.getPersistentDataContainer().set(NameSpaceUtils.owner, PersistentDataType.STRING, key.getOwner().toString());
            meta.getPersistentDataContainer().set(NameSpaceUtils.buyTime, PersistentDataType.LONG, key.getPutInMerch());

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static String getRemainingTimeLore(long timeStart) {
        Instant start = Instant.ofEpochMilli(timeStart);
        Instant now = Instant.now();
        Duration duration = Duration.between(start, now);

        int hoursConfig = Main.getInstance().getConfig().getInt("time-default", 24);

        long maxSeconds = (long) hoursConfig * 60 * 60;
        long elapsedSeconds = duration.getSeconds();
        long remainingSeconds = maxSeconds - elapsedSeconds;
        if (remainingSeconds < 0) remainingSeconds = 0;

        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        return "§7Remaining Time: §a" + hours + "h " + minutes + "m " + seconds + "s";
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Manage Sales - Page")) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (!clicked.hasItemMeta()) return;

            String displayName = clicked.getItemMeta().getDisplayName();
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);

            if (displayName.equals("§ePrevious Page")) {
                open(player, page - 1);
            } else if (displayName.equals("§eNext Page")) {
                open(player, page + 1);
            }  else if (displayName.equals("§cReturn to Menu")){
                MainMenu.open(player);
            }else {
                // Aquí puedes manejar cuando haga click en un Pokémon para comprarlo u otra acción

                if(clicked.getItemMeta() != null) {
                    if(clicked.getItemMeta().getPersistentDataContainer().has(NameSpaceUtils.owner)){
                        String owner = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.owner, PersistentDataType.STRING);
                        long time = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.buyTime, PersistentDataType.LONG);


                        if(clicked.getItemMeta().getPersistentDataContainer().has(NameSpaceUtils.pokemonBuy)){
                            String specie = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.pokemonBuy, PersistentDataType.STRING);

                            PokemonGTS pokemonGTS = Main.getInstance().getPokemonGTSManager().getPokemonGTSSet()
                                    .stream().filter(p->p.getSpecie().equalsIgnoreCase(specie) && p.getOwner().equals(UUID.fromString(owner)) && p.getPutInMerch() == time)
                                    .findFirst().orElse(null);
                            if(pokemonGTS != null) {
                                Main.getInstance().getPokemonGTSManager().delete(pokemonGTS);
                                CobblemonUtils.addPokemon(player, pokemonGTS);
                                player.sendMessage("§aYou have withdrawn your " + pokemonGTS.getSpecie() + "  shop!");
                                player.sendMessage("§7It was added to your PC");
                                open(player, page);
                            }

                        } else {
                            ItemGTS pokemonGTS = Main.getInstance().getItemGTSManager().getItemGTSSet()
                                    .stream().filter(p -> p.getPutInMerch() == time && p.getOwner().toString().equals(owner)).findFirst().orElse(null);
                            if (pokemonGTS == null) return;

                            String key = pokemonGTS.getItemStack().getTranslationKey();
                            TranslatableComponent itemComp = new TranslatableComponent(
                                    key,
                                    new BaseComponent[]{ new TextComponent(" x") }
                            );
                            itemComp.setColor(ChatColor.GREEN);

                            TextComponent before = new TextComponent("§aYou have withdrawn your ");
                            TextComponent after  = new TextComponent( "§a from the market!");

                            BaseComponent[] message = new BaseComponent[]{ before, itemComp, after };

                            player.spigot().sendMessage(message);
                            Main.getInstance().getItemGTSManager().delete(pokemonGTS);
                            player.getInventory().addItem(pokemonGTS.getItemStack());
                            open(player, page);
                        }

                    }
                }
            }
        }
    }




}
