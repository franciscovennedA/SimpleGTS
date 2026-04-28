package com.venned.simplegts.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.venned.simplegts.Main;
import com.venned.simplegts.build.ItemGTS;
import com.venned.simplegts.build.PokemonGTS;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ItemGTSManager {

    private final Set<ItemGTS> itemGTSSet = new HashSet<>();
    private HikariDataSource dataSource;

    public ItemGTSManager() {
        // Configuración de HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + Main.getPlugin(Main.class).getDataFolder() + "/item_gts.db");
        dataSource = new HikariDataSource(config);

        createTableIfNotExists();
        load();

        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(Main.class), this::save, 20L, 20L * 60 * 5);
    }

    public Set<ItemGTS> getItemGTSSet() {
        return itemGTSSet;
    }

    private void createTableIfNotExists() {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "CREATE TABLE IF NOT EXISTS items (\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "    item TEXT NOT NULL,\n" +
                    "    money INTEGER NOT NULL,\n" +
                    "    owner TEXT NOT NULL,\n" +
                    "    put_in_merch BIGINT NOT NULL,\n" +
                    "    UNIQUE(owner, put_in_merch)  -- Ensure that owner and put_in_merch are unique together\n" +
                    ");";

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (Connection connection = dataSource.getConnection()) {
            // SQL para insertar o actualizar elementos si hay conflicto en owner, specie, y putInMerch
            String sql = "INSERT INTO items (item, money, owner, put_in_merch) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(owner, put_in_merch) DO UPDATE SET " + // Conflicto por owner y put_in_merch
                    "item = excluded.item, " +
                    "money = excluded.money;"; // Si hay conflicto, se actualizan los valores del item y money

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (ItemGTS item : itemGTSSet) {
                    Gson gson = new Gson();
                    String itemJson = gson.toJson(serializeItemStack(item.getItemStack())); // Convertir el Map en un String JSON
                    statement.setString(1, itemJson);
                    statement.setInt(2, item.getMoney());
                    statement.setString(3, item.getOwner().toString());
                    statement.setLong(4, item.getPutInMerch());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        itemGTSSet.clear();
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT * FROM items";
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {

                    int money = resultSet.getInt("money");
                    UUID owner = UUID.fromString(resultSet.getString("owner"));
                    long putInMerch = resultSet.getLong("put_in_merch");
                    String jsonData = resultSet.getString("item");
                    Gson gson = new Gson();

                    Map<String, Object> data = gson.fromJson(jsonData, new TypeToken<Map<String, Object>>(){}.getType());
                    ItemStack itemStack = deserializeItemStack(data);

                    if (itemStack != null) {
                        itemGTSSet.add(new ItemGTS(itemStack, money, owner, putInMerch));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(ItemGTS itemGTS) {
        itemGTSSet.remove(itemGTS);

        try (Connection connection = dataSource.getConnection()) {
            String sql = "DELETE FROM items WHERE owner = ? AND put_in_merch = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, itemGTS.getOwner().toString());
                statement.setLong(2, itemGTS.getPutInMerch());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> serializeItemStack(ItemStack item) {
        if (item == null) return null;

        Map<String, Object> data = new HashMap<>(item.serialize()); // Convertir a HashMap para modificar

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            Map<String, Object> metaData = new HashMap<>(meta.serialize()); // Convertir a HashMap para modificar

            if (meta.hasDisplayName()) {
                metaData.put("display-name", meta.getDisplayName().replace("§", "&"));
            }

            if (meta.hasLore()) {
                List<String> formattedLore = meta.getLore().stream()
                        .map(lore -> lore.replace("§", "&"))
                        .collect(Collectors.toList());
                metaData.put("lore", formattedLore);
            }

            data.put("meta", metaData); // Reemplazar los metadatos en la estructura original
        }

        return data;
    }

    private ItemStack deserializeItemStack(Map<String, Object> data) {
        if (data == null) return null;

        // Deserializar el ItemStack
        ItemStack item = ItemStack.deserialize(data);

        if (data.containsKey("meta")) {
            Map<String, Object> metaData = (Map<String, Object>) data.get("meta");
            ItemMeta meta = item.getItemMeta();

            if (metaData.containsKey("display-name")) {
                meta.setDisplayName(((String) metaData.get("display-name")).replace("&", "§"));
            }

            if (metaData.containsKey("lore")) {
                List<String> formattedLore = ((List<String>) metaData.get("lore")).stream()
                        .map(lore -> lore.replace("&", "§"))
                        .collect(Collectors.toList());
                meta.setLore(formattedLore);
            }

            if (metaData.containsKey("enchants")) {
                Map<String, Double> enchants = (Map<String, Double>) metaData.get("enchants");
                for (Map.Entry<String, Double> entry : enchants.entrySet()) {
                    // Convertir el nombre del encantamiento a un formato adecuado para NamespacedKey
                    String enchantmentKey = entry.getKey();
                    String[] parts = enchantmentKey.split(":");
                    // Crear NamespacedKey con el formato "minecraft:<enchantment>"
                    Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(parts[1]));
                    if (enchant != null) {
                        meta.addEnchant(enchant, entry.getValue().intValue(), true);
                    }
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}
