package com.venned.simplegts.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.data.ShowdownIdentifiable;
import com.cobblemon.mod.common.api.moves.BenchedMoves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.venned.simplegts.Main;
import com.venned.simplegts.build.PokemonGTS;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class CobblemonUtils {


    public static PokemonGTS savePokemon(Player player, int slot, int price) {
        Object partyObj = getPlayerParty(player.getName());
        if (partyObj == null || !(partyObj instanceof PartyStore partyStore)) {
            player.sendMessage("No se pudo obtener tu party.");
            return null;
        }

        Pokemon pokemon = partyStore.get(slot);
        if (pokemon == null) {
            player.sendMessage("No tienes un Pokémon en ese slot.");
            return null;
        }

        partyStore.remove(pokemon);

        String specie = pokemon.getSpecies().getName();
        int level = pokemon.getLevel();
        boolean shiny = pokemon.getShiny();
        IVs ivs = pokemon.getIvs();
        EVs evs = pokemon.getEvs();
        Ability ability = pokemon.getAbility();
        BenchedMoves benchedMoves = pokemon.getBenchedMoves();

        JsonObject ivJson = ivs.saveToJSON(new JsonObject());
        JsonObject evJson = evs.saveToJSON(new JsonObject());
        JsonArray benchedJson = benchedMoves.saveToJSON(new JsonArray());


        PokemonGTS pokemonGTS = new PokemonGTS(player.getUniqueId(),
                specie,
                level,
                shiny,
                ivJson.toString(),
                evJson.toString(),
                ability.getName(),
                benchedJson.toString(),
                System.currentTimeMillis(),
                price);

        Main.getInstance().getPokemonGTSManager().getPokemonGTSSet().add(pokemonGTS);

        /*
        // Buscar el siguiente índice global disponible
        int index = config.getInt("index", 0);

        String path = "saved." + index;
        config.set(path + ".owner", player.getUniqueId().toString());
        config.set(path + ".specie", specie);
        config.set(path + ".level", level);
        config.set(path + ".shiny", shiny);
        config.set(path + ".ivs", ivJson.toString());
        config.set(path + ".evs", evJson.toString());
        config.set(path + ".ability", ability.getName());
        config.set(path + ".benchedMoves", benchedJson.toString());

        // Incrementar el índice para el próximo guardado
        config.set("index", index + 1);
        plugin.saveConfig();

         */
        return pokemonGTS;
    }

    public static boolean addPokemon(Player player, PokemonGTS pokemonGTS) {

        /*
        Plugin plugin = Main.getPlugin(Main.class);
        FileConfiguration config = plugin.getConfig();

        String path = "saved." + player.getUniqueId() + "." + name;
        if (!config.contains(path)) {
            player.sendMessage("No se encontró un Pokémon guardado con ese nombre.");
            return false;
        }

         */

        String specie = pokemonGTS.getSpecie();
        int level = pokemonGTS.getLevel();
        String ivsString = pokemonGTS.getIvs();
        String evsString = pokemonGTS.getEvs();
        String abilityString =  pokemonGTS.getAbility();
        String benchedString = pokemonGTS.getBenchedMoves();
        boolean shiny = pokemonGTS.isShiny();

        JsonObject ivsJson = JsonParser.parseString(ivsString).getAsJsonObject();
        JsonObject evsJson = JsonParser.parseString(evsString).getAsJsonObject();

        JsonArray benchedJson = JsonParser.parseString(benchedString).getAsJsonArray();

        IVs ivs = new IVs();
        ivs.loadFromJSON(ivsJson);

        EVs evs = new EVs();
        evs.loadFromJSON(evsJson);


        BenchedMoves benchedMoves = new BenchedMoves();
        benchedMoves.loadFromJSON(benchedJson);

        Collection<Species> speciesList = PokemonSpecies.INSTANCE.getSpecies();
        Species targetSpecies = null;
        for (Species s : speciesList) {
            if (s.getName().equalsIgnoreCase(specie)) {
                targetSpecies = s;
                break;
            }
        }

        if (targetSpecies == null) {
            return false;
        }

        Pokemon pokemon = targetSpecies.create(level);
        pokemon.setIvs$common(ivs);
        pokemon.setEvs$common(evs);
        pokemon.setShiny(shiny);
        AbilityTemplate ability = Abilities.INSTANCE.get(abilityString);
        pokemon.setAbility$common(ability.create(true, Priority.HIGHEST));
        pokemon.setBenchedMoves$common(benchedMoves);

        PCStore pcStore = (PCStore) getPCPlayer(player.getName());
        pcStore.add(pokemon);

        return true;
    }

    public static org.bukkit.inventory.ItemStack getItemPokemon(String species, boolean shiny) {
        Collection<Species> speciesList = PokemonSpecies.INSTANCE.getSpecies();
        Species targetSpecies = null;

        for (Species s : speciesList) {
            if (s.getName().equalsIgnoreCase(species)) {
                targetSpecies = s;
                break;
            }
        }

        if (targetSpecies == null) {
            System.out.println("Error: No se encontró la especie " + species);
            return null;
        }

        Pokemon pokemon = targetSpecies.create(1);
        if (shiny) {
            pokemon.setShiny(true);
        }

        try {

            Class<?> pokemonItemClass = Class.forName("com.cobblemon.mod.common.item.PokemonItem");
            Method fromMethod = pokemonItemClass.getMethod("from", Pokemon.class);
            Object nmsItemStack = fromMethod.invoke(null, pokemon);
            if (nmsItemStack instanceof net.minecraft.world.item.ItemStack itemStack) {
                return CraftItemStack.asBukkitCopy(itemStack);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
    public static Object getPCPlayer(String playerName) {
        Object entityPlayer = getEntityPlayer(playerName);
        if (entityPlayer == null) {
            return null;
        }

        try {
            // Obtener la clase PokemonStoreManager
            Class<?> storeManagerClass = Class.forName("com.cobblemon.mod.common.api.storage.PokemonStoreManager");

            // Obtener la instancia de Cobblemon
            Object cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                    .getDeclaredField("INSTANCE").get(null); // Cobblemon.INSTANCE

            // Llamar a Cobblemon.INSTANCE.getStorage()
            Object storageManager = cobblemonInstance.getClass()
                    .getMethod("getStorage").invoke(cobblemonInstance);

            // Buscar el método "getParty"
            Method getPartyMethod = storeManagerClass.getMethod("getPC", entityPlayer.getClass());

            // Invocar el método con el EntityPlayer
            return getPartyMethod.invoke(storageManager, entityPlayer);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static Object getPlayerParty(String playerName) {
        Object entityPlayer = getEntityPlayer(playerName);
        if (entityPlayer == null) {
            return null;
        }

        try {
            // Obtener la clase PokemonStoreManager
            Class<?> storeManagerClass = Class.forName("com.cobblemon.mod.common.api.storage.PokemonStoreManager");

            // Obtener la instancia de Cobblemon
            Object cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                    .getDeclaredField("INSTANCE").get(null); // Cobblemon.INSTANCE

            // Llamar a Cobblemon.INSTANCE.getStorage()
            Object storageManager = cobblemonInstance.getClass()
                    .getMethod("getStorage").invoke(cobblemonInstance);

            // Buscar el método "getParty"
            Method getPartyMethod = storeManagerClass.getMethod("getParty", entityPlayer.getClass());

            // Invocar el método con el EntityPlayer
            return getPartyMethod.invoke(storageManager, entityPlayer);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getEntityPlayer(String playerName) {
        Player bukkitPlayer = Bukkit.getPlayerExact(playerName);
        if (bukkitPlayer == null) return null;

        try {
            // Bukkit → CraftPlayer → EntityPlayer
            Method getHandle = bukkitPlayer.getClass().getMethod("getHandle");
            return getHandle.invoke(bukkitPlayer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



}
