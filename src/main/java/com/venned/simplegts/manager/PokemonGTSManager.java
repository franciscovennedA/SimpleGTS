package com.venned.simplegts.manager;

import com.venned.simplegts.Main;
import com.venned.simplegts.build.PokemonGTS;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PokemonGTSManager {

    private final Set<PokemonGTS> pokemonGTSSet = new HashSet<>();
    private HikariDataSource dataSource;

    public PokemonGTSManager() {
        // Configuración de HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + Main.getPlugin(Main.class).getDataFolder() + "/gts.db");
        dataSource = new HikariDataSource(config);
        createTableIfNotExists();
        load();

        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(Main.class), this::save, 20L, 20L * 60 * 5);
    }

    public Set<PokemonGTS> getPokemonGTSSet() {
        return pokemonGTSSet;
    }

    // Crear la tabla si no existe
    private void createTableIfNotExists() {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "CREATE TABLE IF NOT EXISTS pokemons (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "owner TEXT NOT NULL," +
                    "specie TEXT NOT NULL," +
                    "level INTEGER NOT NULL," +
                    "shiny BOOLEAN NOT NULL," +
                    "ivs TEXT NOT NULL," +
                    "evs TEXT NOT NULL," +
                    "ability TEXT NOT NULL," +
                    "benchedMoves TEXT NOT NULL," +
                    "putInMerch INTEGER NOT NULL," +
                    "price INTEGER NOT NULL," +
                    "UNIQUE(owner, specie, putInMerch));";  // Aseguramos que owner y specie sean únicos para evitar duplicados

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "INSERT INTO pokemons (owner, specie, level, shiny, ivs, evs, ability, benchedMoves, putInMerch, price) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(owner, specie, putInMerch) DO UPDATE SET " +  // Conflicto por owner y specie
                    "level = excluded.level, " +
                    "shiny = excluded.shiny, " +
                    "ivs = excluded.ivs, " +
                    "evs = excluded.evs, " +
                    "ability = excluded.ability, " +
                    "benchedMoves = excluded.benchedMoves, " +
                    "putInMerch = excluded.putInMerch, " +
                    "price = excluded.price;";  // Si hay conflicto, actualizamos todos los demás campos

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (PokemonGTS pokemon : pokemonGTSSet) {
                    statement.setString(1, pokemon.getOwner().toString());
                    statement.setString(2, pokemon.getSpecie());
                    statement.setInt(3, pokemon.getLevel());
                    statement.setBoolean(4, pokemon.isShiny());
                    statement.setString(5, pokemon.getIvs());
                    statement.setString(6, pokemon.getEvs());
                    statement.setString(7, pokemon.getAbility());
                    statement.setString(8, pokemon.getBenchedMoves());
                    statement.setLong(9, pokemon.getPutInMerch());
                    statement.setInt(10, pokemon.getPrice());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cargar los Pokémon desde la base de datos
    public void load() {
        pokemonGTSSet.clear();

        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT * FROM pokemons";
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    UUID owner = UUID.fromString(resultSet.getString("owner"));
                    String specie = resultSet.getString("specie");
                    int level = resultSet.getInt("level");
                    boolean shiny = resultSet.getBoolean("shiny");
                    String ivs = resultSet.getString("ivs");
                    String evs = resultSet.getString("evs");
                    String ability = resultSet.getString("ability");
                    String benchedMoves = resultSet.getString("benchedMoves");
                    long putInMerch = resultSet.getLong("putInMerch");
                    int price = resultSet.getInt("price");

                    PokemonGTS pokemon = new PokemonGTS(owner, specie, level, shiny, ivs, evs, ability, benchedMoves, putInMerch, price);
                    pokemonGTSSet.add(pokemon);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(PokemonGTS pokemon) {
        pokemonGTSSet.remove(pokemon); // Primero lo quitamos del set en memoria

        try (Connection connection = dataSource.getConnection()) {
            String sql = "DELETE FROM pokemons WHERE owner = ? AND specie = ? AND putInMerch = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, pokemon.getOwner().toString());
                statement.setString(2, pokemon.getSpecie());
                statement.setLong(3, pokemon.getPutInMerch());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}
