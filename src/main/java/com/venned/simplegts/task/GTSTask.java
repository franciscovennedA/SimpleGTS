package com.venned.simplegts.task;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.ItemGTS;
import com.venned.simplegts.build.PokemonGTS;
import com.venned.simplegts.manager.ItemGTSManager;
import com.venned.simplegts.manager.PokemonGTSManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class GTSTask extends BukkitRunnable {

    private final ItemGTSManager itemGTSManager;
    private final PokemonGTSManager pokemonGTSManager;

    public GTSTask(ItemGTSManager itemGTSManager, PokemonGTSManager pokemonGTSManager) {
        this.itemGTSManager = itemGTSManager;
        this.pokemonGTSManager = pokemonGTSManager;
    }

    @Override
    public void run() {
        int hoursMax = Main.getInstance().getConfig().getInt("time-default", 24);

        Set<ItemGTS> toRemoveItems = new HashSet<>();
        Set<PokemonGTS> toRemovePokemons = new HashSet<>();

        Instant now = Instant.now();

        for (ItemGTS itemGTS : itemGTSManager.getItemGTSSet()) {
            Instant placed = Instant.ofEpochMilli(itemGTS.getPutInMerch());
            Duration duration = Duration.between(placed, now);

            if (duration.toHours() >= hoursMax) {
                toRemoveItems.add(itemGTS);
            }
        }

        for (PokemonGTS pokemonGTS : pokemonGTSManager.getPokemonGTSSet()) {
            Instant placed = Instant.ofEpochMilli(pokemonGTS.getPutInMerch());
            Duration duration = Duration.between(placed, now);

            if (duration.toHours() >= hoursMax) {
                toRemovePokemons.add(pokemonGTS);
            }
        }

        // Eliminar los expirados
        for (ItemGTS itemGTS : toRemoveItems) {
            itemGTSManager.delete(itemGTS);
        }

        for (PokemonGTS pokemonGTS : toRemovePokemons) {
            pokemonGTSManager.delete(pokemonGTS);
        }


    }
}
