package com.venned.simplegts.gui;


import com.venned.simplegts.utils.EconomyUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainMenu implements Listener {

    public MainMenu(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, "Menu");

        // Crear un ItemStack para el botón de menú de Pokémon
        ItemStack pokemonMenuItem = new ItemStack(Material.valueOf("COBBLEMON_POKE_BALL")); // Usa un material adecuado aquí
        ItemMeta pokemonMeta = pokemonMenuItem.getItemMeta();
        if (pokemonMeta != null) {
            pokemonMeta.setDisplayName("§aOpen Menu Pokemon");
            pokemonMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            pokemonMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            pokemonMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            pokemonMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            pokemonMenuItem.setItemMeta(pokemonMeta);
        }

        // Crear un ItemStack para el botón de menú de Items
        ItemStack itemMenuItem = new ItemStack(Material.CRAFTING_TABLE); // Usa un material adecuado aquí
        ItemMeta itemMeta = itemMenuItem.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName("§aOpen Menu Items");
            itemMenuItem.setItemMeta(itemMeta);
        }


        // Crear un ItemStack para el botón de menú de Items
        ItemStack itemOwner = new ItemStack(Material.valueOf("COBBLEMON_SACHET")); // Usa un material adecuado aquí
        ItemMeta itemOwnerMeta = itemMenuItem.getItemMeta();
        if (itemOwnerMeta != null) {
            itemOwnerMeta.setDisplayName("§aOpen Sell");
            itemOwnerMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            itemOwnerMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            itemOwnerMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemOwnerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemOwner.setItemMeta(itemOwnerMeta);
        }

        ItemStack moneyMenuItem = new ItemStack(Material.PLAYER_HEAD); // Cambiar a cabeza de jugador
        SkullMeta skullMeta = (SkullMeta) moneyMenuItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player); // Establecer el dueño de la cabeza (para que tenga su skin)
            skullMeta.setDisplayName("§aBalance");

            List<String> lore = new ArrayList<>();
            double balance = EconomyUtils.getMoney(player);
            // Formatear el balance con comas
            String formattedBalance = NumberFormat.getNumberInstance(Locale.US).format(Math.round(balance));

            lore.add(" ");
            lore.add("§7Money : §a阿" + formattedBalance);
            lore.add(" ");

            skullMeta.setLore(lore);
            moneyMenuItem.setItemMeta(skullMeta);
        }

        // Añadir los botones al inventario
        inventory.setItem(1, pokemonMenuItem);  // Coloca en la posición 3
        inventory.setItem(3, moneyMenuItem);
        inventory.setItem(5, itemMenuItem);     // Coloca en la posición 5
        inventory.setItem(7, itemOwner);

        // Abrir el inventario para el jugador
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals("Menu")) {
            event.setCancelled(true);  // Evitar que los jugadores tomen los items del inventario

            // Verificar qué botón fue presionado
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta != null) {
                String displayName = meta.getDisplayName();

                // Comprobar si se ha hecho clic en el menú de Pokémon
                if (displayName.equals("§aOpen Menu Pokemon")) {
                    PokemonMenu.open(player);
                }
                // Comprobar si se ha hecho clic en el menú de Items
                if (displayName.equals("§aOpen Menu Items")) {
                    ItemMenu.open(player);
                }

                if (displayName.equals("§aOpen Sell")){
                    SellMenu.open(player);
                }
            }
        }
    }


}