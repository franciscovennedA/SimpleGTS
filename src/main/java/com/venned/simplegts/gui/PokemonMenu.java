package com.venned.simplegts.gui;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.PendingPurchase;
import com.venned.simplegts.build.PokemonGTS;
import com.venned.simplegts.utils.CobblemonUtils;
import com.venned.simplegts.utils.EconomyUtils;
import com.venned.simplegts.utils.MessageUtils;
import com.venned.simplegts.utils.NameSpaceUtils;
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

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class PokemonMenu implements Listener {

    private static final Map<UUID, Integer> playerPages = new HashMap<>(); // Guardar páginas por jugador
    private static final int ITEMS_PER_PAGE = 45; // 5 líneas de 9 ítems, 1 línea reservada para botones

    public PokemonMenu(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Set<PokemonGTS> keys = Main.getInstance().getPokemonGTSManager().getPokemonGTSSet();
        List<PokemonGTS> pokemonList = new ArrayList<>(keys);

        int totalPages = (int) Math.ceil((double) pokemonList.size() / ITEMS_PER_PAGE);

        // 👇 Si no hay Pokémon, forzamos que haya al menos una página
        if (totalPages == 0) {
            totalPages = 1;
        }

        // Asegurar que la página esté en un rango válido
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, "Pokemon Store - Page " + (page + 1));

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, pokemonList.size());

        for (int i = start; i < end; i++) {
            PokemonGTS key = pokemonList.get(i);
            ItemStack item = buildPokemonItem(key);
            inv.addItem(item);
        }

        // Botones de paginación
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

    private static ItemStack buildPokemonItem(PokemonGTS key) {
        String specie = key.getSpecie();
        int level = key.getLevel();
        boolean shiny = key.isShiny();
        String ivs = key.getIvs();
        String evs = key.getEvs();
        String ability = key.getAbility();
        String benchedMoves = key.getBenchedMoves();
        String owner = key.getOwner().toString();
        long timeStart = key.getPutInMerch();

        Instant start = Instant.ofEpochMilli(timeStart);
        Instant now = Instant.now();
        Duration duration = Duration.between(start, now);

        int hoursConfig = Main.getInstance().getConfig().getInt("time-default", 24);

        long maxSeconds = (long) hoursConfig * 60 * 60; // 24 horas
        long elapsedSeconds = duration.getSeconds();
        long remainingSeconds = maxSeconds - elapsedSeconds;

        if (remainingSeconds < 0) remainingSeconds = 0; // Seguridad

        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        String timeRemaining = "§7Remaining Time: §a" + hours + "h " + minutes + "m " + seconds + "s";

        ItemStack item = CobblemonUtils.getItemPokemon(specie, shiny);
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add("§7Owner: §e" + Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName());
        lore.add("§7Specie: §b" + specie);
        lore.add("§7Level: §b" + level);
        lore.add("§7Shiny: §b" + (shiny ? "Sí" : "No"));
        lore.add("§7Ability: §b" + ability);

        if (ivs != null && !ivs.isEmpty()) {
            StringBuilder ivBuilder = new StringBuilder("§7IVs: ");
            String[] ivsArray = ivs.replace("{", "").replace("}", "").replace("\"", "").split(",");
            int count = 0;
            for (String entry : ivsArray) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    ivBuilder.append("§f").append(parts[0].toUpperCase()).append(": §c").append(parts[1]).append(" ");
                    count++;
                    if (count % 3 == 0) {
                        lore.add(ivBuilder.toString().trim());
                        ivBuilder = new StringBuilder();
                    }
                }
            }
            if (ivBuilder.length() > 0) {
                lore.add(ivBuilder.toString().trim());
            }
        }

        if (evs != null && !evs.isEmpty()) {
            StringBuilder evBuilder = new StringBuilder("§7EVs: ");
            String[] evsArray = evs.replace("{", "").replace("}", "").replace("\"", "").split(",");
            int count = 0;
            for (String entry : evsArray) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    evBuilder.append("§f").append(parts[0].toUpperCase()).append(": §c").append(parts[1]).append(" ");
                    count++;
                    if (count % 3 == 0) {
                        lore.add(evBuilder.toString().trim());
                        evBuilder = new StringBuilder();
                    }
                }
            }
            if (evBuilder.length() > 0) {
                lore.add(evBuilder.toString().trim());
            }
        }

        lore.add(" ");
        lore.add("§7Price: §a阿" + formatPrice(key.getPrice()));
        lore.add(timeRemaining);

        meta.getPersistentDataContainer().set(NameSpaceUtils.pokemonBuy, PersistentDataType.STRING, specie);
        meta.getPersistentDataContainer().set(NameSpaceUtils.buyTime, PersistentDataType.LONG, key.getPutInMerch());
        meta.setDisplayName("§a" + specie + (shiny ? " ✨" : ""));
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Pokemon Store - Page")) {
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
            } else if (displayName.equals("§cReturn to Menu")){
                MainMenu.open(player);
            } else {
                // Aquí puedes manejar cuando haga click en un Pokémon para comprarlo u otra acción

                if(clicked.getItemMeta() != null) {
                    if(clicked.getItemMeta().getPersistentDataContainer().has(NameSpaceUtils.pokemonBuy)){
                        String specie = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.pokemonBuy, PersistentDataType.STRING);
                        long time = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.buyTime, PersistentDataType.LONG);

                        PokemonGTS pokemonGTS = Main.getInstance().getPokemonGTSManager().getPokemonGTSSet()
                                .stream().filter(p-> p.getPutInMerch() == time && p.getSpecie().equalsIgnoreCase(specie))
                                .findFirst().orElse(null);

                        if(pokemonGTS == null) return;

                        if(pokemonGTS.getOwner().equals(player.getUniqueId())) return;

                        if(!EconomyUtils.hasMoney(player, pokemonGTS.getPrice())){
                            MessageUtils.send(player, "not-enough-funds");
                            return;
                        }

                        ConfirmationMenu.openConfirm(player, PendingPurchase.of(pokemonGTS));


                        /*
                        EconomyUtils.senMessageBuy(pokemonGTS.getOwner(), "§c§l(!) §a" + player.getName() + " acaba de comprar tu pokemon a la venta!");
                        EconomyUtils.takeMoney(player, pokemonGTS.getPrice());
                        EconomyUtils.addMoney(pokemonGTS.getOwner(), pokemonGTS.getPrice());
                        CobblemonUtils.addPokemon(player, pokemonGTS);
                        Main.getInstance().getPokemonGTSManager().delete(pokemonGTS);
                        player.sendMessage("§c§l(!) §aComprado con exito se añadio a tu pc");
                        open(player, page);

                         */

                    }
                }
            }
        }
    }


    private static String formatPrice(int price) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        return nf.format(price);
    }
}