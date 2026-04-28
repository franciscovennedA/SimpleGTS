package com.venned.simplegts.utils;

import com.Zrips.CMI.CMI;
import com.venned.simplegts.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyUtils {

    public enum MoneyType {
        CMI,
        VAULT
    }

    static MoneyType moneyType;

    public static boolean hasMoney(Player player, double amount) {

        if(moneyType == MoneyType.CMI) {
            try {
                double balanceDouble = CMI.getInstance().getPlayerManager().getUser(player.getUniqueId()).getBalance(); // Convertimos BigDecimal a double
                return balanceDouble >= amount;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            double balanceDouble = Main.getEcon().getBalance(player);
            return balanceDouble >= amount;
        }
    }

    public static void addMoney(UUID uuid, double amount) {
        if(moneyType == MoneyType.CMI) {
            try {
                CMI.getInstance().getPlayerManager().getUser(uuid).deposit(amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Main.getEcon().depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        }
    }

    public static Double getMoney(Player player){
        if(moneyType == MoneyType.CMI) {
            try {
                return CMI.getInstance().getPlayerManager().getUser(player.getUniqueId()).getBalance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return Main.getEcon().getBalance(player);
        }

        return 0.0;
    }

    public static void senMessageBuy(UUID owner, String message){
        Player find = Bukkit.getPlayer(owner);
        if(find == null) return;
        find.sendMessage(message);
    }

    public static void takeMoney(Player player, double amount){
        if(moneyType == MoneyType.CMI) {
            try {
                CMI.getInstance().getPlayerManager().getUser(player.getUniqueId()).withdraw(amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Main.getEcon().withdrawPlayer(player, amount);
        }
    }

    public static void setMoneyType(MoneyType moneyType) {
       EconomyUtils.moneyType = moneyType;
    }

    public static MoneyType getMoneyType() {
        return moneyType;
    }
}
