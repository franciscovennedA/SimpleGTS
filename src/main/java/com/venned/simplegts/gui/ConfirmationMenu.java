package com.venned.simplegts.gui;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.ItemGTS;
import com.venned.simplegts.build.PendingPurchase;
import com.venned.simplegts.build.PokemonGTS;
import com.venned.simplegts.utils.CobblemonUtils;
import com.venned.simplegts.utils.EconomyUtils;
import com.venned.simplegts.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationMenu implements Listener {
    private static final Map<UUID, PendingPurchase> pending = new HashMap<>();

    public ConfirmationMenu() {
        Main.getInstance().getServer().getPluginManager()
                .registerEvents(this, Main.getInstance());
    }

    public static void openConfirm(Player player, PendingPurchase pp) {
        pending.put(player.getUniqueId(), pp);
        Inventory inv = Bukkit.createInventory(null, 9, "Confirm purchase");
        // Sí (verde)
        ItemStack yes = new ItemStack(Material.GREEN_WOOL);
        ItemMeta my = yes.getItemMeta();
        my.setDisplayName("§a✅ Yes");
        yes.setItemMeta(my);
        // No (rojo)
        ItemStack no = new ItemStack(Material.RED_WOOL);
        ItemMeta mn = no.getItemMeta();
        mn.setDisplayName("§c❌ No");
        no.setItemMeta(mn);

        inv.setItem(3, yes);
        inv.setItem(5, no);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!"Confirm purchase".equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        PendingPurchase pp = pending.remove(p.getUniqueId());
        p.closeInventory();
        if (pp == null) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("✅")) {
            // El jugador confirmó
            if (pp.getType() == PendingPurchase.Type.POKEMON) {
                PokemonGTS poke = pp.getPokemon();
                if (!EconomyUtils.hasMoney(p, poke.getPrice())) {
                    MessageUtils.send(p, "not-enough-funds");
                    return;
                }

                boolean exist = false;
                for(PokemonGTS pokegts : Main.getInstance().getPokemonGTSManager().getPokemonGTSSet()){
                    if (pokegts.equals(poke)) {
                        exist = true;
                        break;
                    }
                }

                if(!exist){
                    p.closeInventory();
                    MessageUtils.send(p, "already-purchased");
                    return;
                }

                EconomyUtils.takeMoney(p, poke.getPrice());
                EconomyUtils.addMoney(poke.getOwner(), poke.getPrice());
                CobblemonUtils.addPokemon(p, poke);
                Main.getInstance().getPokemonGTSManager().delete(poke);

                MessageUtils.send(p, "purchase-success-pokemon");



                String messageBuy = MessageUtils.get("purchase-message-owner-pokemon");
                messageBuy = messageBuy.replace("%player%", p.getName());
                messageBuy = messageBuy.replace("%money%", "" + poke.getPrice());
                messageBuy = messageBuy.replace("%pokemon%", poke.getSpecie());

                EconomyUtils.senMessageBuy(
                        poke.getOwner(),
                        messageBuy
                );

            } else { // ITEM
                ItemGTS item = pp.getItem();
                if (!EconomyUtils.hasMoney(p, item.getMoney())) {
                    MessageUtils.send(p, "not-enough-funds");
                    return;
                }

                boolean exist = false;

                for(ItemGTS itemsGTS : Main.getInstance().getItemGTSManager().getItemGTSSet()){
                    if(itemsGTS.equals(item)){
                        exist = true;
                        break;
                    }
                }

                if(!exist){
                    p.closeInventory();
                    MessageUtils.send(p, "already-purchased");
                    return;
                }

                EconomyUtils.takeMoney(p, item.getMoney());
                EconomyUtils.addMoney(item.getOwner(), item.getMoney());
                p.getInventory().addItem(item.getItemStack());
                Main.getInstance().getItemGTSManager().delete(item);

                MessageUtils.send(p, "purchase-success-item");

                String messageBuy = MessageUtils.get("purchase-message-owner");
                messageBuy = messageBuy.replace("%player%", p.getName());
                messageBuy = messageBuy.replace("%money%", "" + item.getMoney());

                EconomyUtils.senMessageBuy(
                        item.getOwner(),
                        messageBuy
                );
            }

        } else {

            MessageUtils.send(p, "purchase-cancelled");

            if (pp.getType() == PendingPurchase.Type.POKEMON) {
                PokemonMenu.open(p);
            } else {
                ItemMenu.open(p);
            }
        }
    }
}