package com.venned.simplegts;

import com.venned.simplegts.build.ItemGTS;
import com.venned.simplegts.commands.MainCommand;
import com.venned.simplegts.gui.*;
import com.venned.simplegts.manager.ItemGTSManager;
import com.venned.simplegts.manager.PokemonGTSManager;
import com.venned.simplegts.task.GTSTask;
import com.venned.simplegts.utils.EconomyUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {


    PokemonGTSManager pokemonGTSManager;
    ItemGTSManager itemGTSManager;
    static Main instance;
    BukkitAudiences audiences;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;
        setupEconomy();

        EconomyUtils.setMoneyType(EconomyUtils.MoneyType.valueOf(getConfig().getString("economy")));
        audiences = BukkitAudiences.create(this);
        saveDefaultConfig();

        pokemonGTSManager = new PokemonGTSManager();
        itemGTSManager = new ItemGTSManager();
        loadMenus();
        getCommand("gts").setExecutor(new MainCommand());

        new GTSTask(itemGTSManager, pokemonGTSManager).runTaskTimerAsynchronously(this, 30, 30);
    }

    @Override
    public void onDisable() {
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEcon() {
        return econ;
    }

    public BukkitAudiences getAudiences() {
        return audiences;
    }

    public ItemGTSManager getItemGTSManager() {
        return itemGTSManager;
    }

    public PokemonGTSManager getPokemonGTSManager() {
        return pokemonGTSManager;
    }

    public static Main getInstance() {
        return instance;
    }

    void loadMenus(){
        new SellMenu(this);
        new MainMenu(this);
        new ItemMenu(this);
        new PokemonMenu(this);
        new ConfirmationMenu();
    }
}
