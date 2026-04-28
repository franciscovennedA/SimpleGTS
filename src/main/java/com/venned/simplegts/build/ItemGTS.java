package com.venned.simplegts.build;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ItemGTS {


    ItemStack itemStack;
    int money;
    UUID owner;
    long putInMerch;

    public ItemGTS(ItemStack itemStack, int money, UUID owner, long putInMerch ) {
        this.itemStack = itemStack;
        this.money = money;
        this.owner = owner;
        this.putInMerch = putInMerch;
    }

    public long getPutInMerch() {
        return putInMerch;
    }

    public UUID getOwner() {
        return owner;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getMoney() {
        return money;
    }
}
