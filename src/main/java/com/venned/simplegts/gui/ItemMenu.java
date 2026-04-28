package com.venned.simplegts.gui;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.ItemGTS;
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

public class ItemMenu implements Listener {



    private static final Map<UUID, Integer> playerPages = new HashMap<>(); // Guardar páginas por jugador
    private static final int ITEMS_PER_PAGE = 45; // 5 líneas de 9 ítems, 1 línea reservada para botones

    public ItemMenu(Plugin plugin) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        public static void open(Player player) {
            open(player, 0);
        }

        public static void open(Player player, int page) {
            Set<ItemGTS> keys = Main.getInstance().getItemGTSManager().getItemGTSSet();
            List<ItemGTS> pokemonList = new ArrayList<>(keys);

            int totalPages = (int) Math.ceil((double) pokemonList.size() / ITEMS_PER_PAGE);

            // 👇 Si no hay Pokémon, forzamos que haya al menos una página
            if (totalPages == 0) {
                totalPages = 1;
            }

            // Asegurar que la página esté en un rango válido
            if (page < 0) page = 0;
            if (page >= totalPages) page = totalPages - 1;

            playerPages.put(player.getUniqueId(), page);

            Inventory inv = Bukkit.createInventory(null, 54, "Shop Items - Page " + (page + 1));

            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, pokemonList.size());

            for (int i = start; i < end; i++) {
                ItemGTS key = pokemonList.get(i);
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

        private static ItemStack buildPokemonItem(ItemGTS key) {

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

            String timeRemaining = "§7Time remaining: §a" + hours + "h " + minutes + "m " + seconds + "s";
            ItemStack item = key.getItemStack().clone();
            ItemMeta meta = item.getItemMeta();

            List<String> lore;

            if(item.getItemMeta() != null ){
                if(item.getItemMeta().getLore() != null){
                    lore = new ArrayList<>(item.getItemMeta().getLore());
                } else {
                    lore = new ArrayList<>();
                }
            } else {
                lore = new ArrayList<>();
            }

            lore.add(" ");
            lore.add("§7Owner: §e" + Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName());
            lore.add(" ");
            lore.add("§7Price: §a阿" + formatPrice(key.getMoney()));
            lore.add(timeRemaining);
            meta.getPersistentDataContainer().set(NameSpaceUtils.owner, PersistentDataType.STRING, owner);
            meta.getPersistentDataContainer().set(NameSpaceUtils.buyTime, PersistentDataType.LONG, key.getPutInMerch());
            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (event.getView().getTitle().startsWith("Shop Items - Page")) {
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
                }else {
                    // Aquí puedes manejar cuando haga click en un Pokémon para comprarlo u otra acción

                    if(clicked.getItemMeta() != null) {
                        if(clicked.getItemMeta().getPersistentDataContainer().has(NameSpaceUtils.owner)){
                            String owner = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.owner, PersistentDataType.STRING);
                            long time = clicked.getItemMeta().getPersistentDataContainer().get(NameSpaceUtils.buyTime, PersistentDataType.LONG);

                            ItemGTS pokemonGTS = Main.getInstance().getItemGTSManager().getItemGTSSet()
                                    .stream().filter(p-> p.getPutInMerch() == time && p.getOwner().toString().equals(owner)).findFirst().orElse(null);


                            if(pokemonGTS == null) return;

                            if(pokemonGTS.getOwner().equals(player.getUniqueId())) return;

                            if(!EconomyUtils.hasMoney(player, pokemonGTS.getMoney())){
                                MessageUtils.send(player, "not-enough-funds");
                                return;
                            }

                            ConfirmationMenu.openConfirm(player, PendingPurchase.of(pokemonGTS));


                            /*

                            player.getInventory().addItem(pokemonGTS.getItemStack());
                            Main.getInstance().getItemGTSManager().delete(pokemonGTS);

                            EconomyUtils.senMessageBuy(UUID.fromString(owner), "§c§l(!) §a" + player.getName() + " acaba de comprar tu articulo!");
                            EconomyUtils.addMoney(UUID.fromString(owner), pokemonGTS.getMoney());
                            EconomyUtils.takeMoney(player, pokemonGTS.getMoney());
                            player.sendMessage("§c§l(!) §aComprado con exito se añadio a tu inventario");
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
