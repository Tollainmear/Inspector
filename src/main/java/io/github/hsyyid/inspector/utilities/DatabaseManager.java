package io.github.hsyyid.inspector.utilities;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.hsyyid.inspector.Inspector;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.sql.DataSource;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class DatabaseManager {
    private static final String CREATE_PLAYER_TABLE_MYSQL = "CREATE TABLE IF NOT EXISTS `PLAYERS`(`ID` INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, `UUID` TEXT NOT NULL, `NAME` TEXT NOT NULL)";
    private static final String CREATE_PLAYER_TABLE_SQLITE = "CREATE TABLE IF NOT EXISTS `PLAYERS`(`ID` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `UUID` TEXT NOT NULL, `NAME` TEXT NOT NULL)";
    private final Logger LOG = Inspector.instance().getLogger();
    private final Connection connection;
    private final Gson gson = (new GsonBuilder()).create();
    private boolean mysql;

    public DatabaseManager() throws SQLException {
        if ((Boolean) Utils.getConfigValue("database.mysql.enabled")) {
            SqlService sql = Sponge.getServiceManager().provide(SqlService.class).get();
            String host = (String) Utils.getConfigValue("database.mysql.host");
            String port = (String) Utils.getConfigValue("database.mysql.port");
            String username = (String) Utils.getConfigValue("database.mysql.username");
            String password = (String) Utils.getConfigValue("database.mysql.password");
            String database = (String) Utils.getConfigValue("database.mysql.database");
            DataSource datasource = sql.getDataSource("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + username + "&password=" + password);
            this.connection = datasource.getConnection();
            mysql = true;
            LOG.info("Inspector is using Mysql/MariaDB database");
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
                mysql = false;
                LOG.info("Inspector is using SQLite database");
            } catch (ClassNotFoundException var8) {
                LOG.error("Error! You do not have any database software installed. This plugin cannot work correctly!");
            }

            this.connection = DriverManager.getConnection("jdbc:sqlite:Inspector.db");
        }
    }

    public void updateBlockInformation(int x, int y, int z, UUID worldUUID, UUID playerUUID, String playerName, String time, BlockSnapshot oldBlockSnapshot, BlockSnapshot newBlockSnapshot) {
        Sponge.getScheduler().createTaskBuilder().async().execute(() -> {
            try {
                Statement stmt = Objects.requireNonNull(connection).createStatement();
                String sql = "CREATE TABLE IF NOT EXISTS BLOCKINFO(LOCATION TEXT NOT NULL, PLAYERID INT NOT NULL, TIME TEXT NOT NULL, OLDBLOCK TEXT NOT NULL, NEWBLOCK TEXT NOT NULL)";
                stmt.executeUpdate(sql);
                Map<?, ?> serializedBlock = oldBlockSnapshot.toContainer().getMap(DataQuery.of()).get();
                String oldBlock = gson.toJson(serializedBlock);
                serializedBlock = newBlockSnapshot.toContainer().getMap(DataQuery.of()).get();
                String newBlock = gson.toJson(serializedBlock);
                if (mysql) {
                    oldBlock = addBackslashForMysql(oldBlock);
                    newBlock = addBackslashForMysql(newBlock);
                }
                sql = "INSERT INTO BLOCKINFO (LOCATION,PLAYERID,TIME,OLDBLOCK,NEWBLOCK) VALUES ('" + x + ";" + y + ";" + z + ";" + worldUUID.toString() + "'," + this.getPlayerId(playerUUID) + ",'" + time + "','" + oldBlock + "','" + newBlock + "');";
                stmt.executeUpdate(sql);
                stmt.close();
            } catch (SQLException var15) {
                var15.printStackTrace();
            }

        }).submit(Inspector.instance());
    }

    private String addBackslashForMysql(String string) {
        return string.replaceAll("\\\\", "\\\\\\\\");
    }

    public void addPlayerToDatabase(Player player) {
        Sponge.getScheduler().createTaskBuilder().async().execute(() -> {
            try {
                Statement stmt = connection.createStatement();
                String sql = "INSERT INTO PLAYERS (UUID, NAME) VALUES ('" + player.getUniqueId().toString() + "','" + player.getName() + "');";
                stmt.executeUpdate(sql);
                stmt.close();
            } catch (SQLException var5) {
                var5.printStackTrace();
            }

        }).submit(Inspector.instance());
    }

    public boolean isPlayerInDatabase(Player player) {
        boolean isInDatabase = false;

        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(mysql ? CREATE_PLAYER_TABLE_MYSQL : CREATE_PLAYER_TABLE_SQLITE);
            stmt.close();
            PreparedStatement preparedStmt = connection.prepareStatement("SELECT count(*) from PLAYERS WHERE uuid=?");
            preparedStmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                isInDatabase = rs.getInt(1) > 0;
            }

            rs.close();
            preparedStmt.close();
        } catch (SQLException var7) {
            var7.printStackTrace();
        }

        return isInDatabase;
    }

    private int getPlayerId(UUID uniqueId) {
        int id = -1;

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM PLAYERS WHERE uuid=?");
            stmt.setString(1, uniqueId.toString());

            ResultSet rs;
            for (rs = stmt.executeQuery(); rs.next(); id = rs.getInt("id")) {
            }

            rs.close();
            stmt.close();
        } catch (SQLException var6) {
            var6.printStackTrace();
        }

        return id;
    }

    private UUID getPlayerUniqueId(int id) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM PLAYERS WHERE id=?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("uuid"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return null;
    }

    private String getPlayerName(int id) {
        String name = "";

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM PLAYERS WHERE id=?");
            stmt.setInt(1, id);

            ResultSet rs;
            for (rs = stmt.executeQuery(); rs.next(); name = rs.getString("name")) {
            }

            rs.close();
            stmt.close();
        } catch (SQLException var6) {
            var6.printStackTrace();
        }

        return name;
    }

    public List<BlockInformation> getBlockInformationAt(Location<World> location) {
        ArrayList blockInformation = Lists.newArrayList();

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM BLOCKINFO WHERE location=?");
            stmt.setString(1, location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ() + ";" + location.getExtent().getUniqueId().toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BlockSnapshot oldBlockSnapshot = this.deserializeBlockSnapshot(rs.getString("oldBlock"));
                BlockSnapshot newBlockSnapshot = this.deserializeBlockSnapshot(rs.getString("newBlock"));
                int id = rs.getInt("playerid");
                blockInformation.add(new BlockInformation(location, oldBlockSnapshot, newBlockSnapshot, rs.getString("time"), this.getPlayerUniqueId(id), this.getPlayerName(id)));
            }

            rs.close();
            stmt.close();
        } catch (SQLException var9) {
            var9.printStackTrace();
        }

        return blockInformation;
    }

    public List<BlockInformation> getBlockInformationAt(Set<Location<World>> locations, UUID playerUniqueId) {
        List<BlockInformation> blockInformation = Lists.newArrayList();
        int playerId = this.getPlayerId(playerUniqueId);
        Iterator var5 = locations.iterator();

        while (var5.hasNext()) {
            Location location = (Location) var5.next();

            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM BLOCKINFO WHERE location=? AND playerId=?");
                stmt.setString(1, location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ() + ";" + location.getExtent().getUniqueId().toString());
                stmt.setInt(2, playerId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    BlockSnapshot oldBlockSnapshot = this.deserializeBlockSnapshot(rs.getString("oldBlock"));
                    BlockSnapshot newBlockSnapshot = this.deserializeBlockSnapshot(rs.getString("newBlock"));
                    blockInformation.add(new BlockInformation(location, oldBlockSnapshot, newBlockSnapshot, rs.getString("time"), playerUniqueId, this.getPlayerName(playerId)));
                }

                rs.close();
                stmt.close();
            } catch (SQLException var12) {
                var12.printStackTrace();
            }
        }

        blockInformation.sort((o1, o2) -> {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                return format.parse(o1.getTimeEdited()).compareTo(format.parse(o2.getTimeEdited()));
            } catch (ParseException var51) {
                var51.printStackTrace();
                return -1;
            }
        });
        return blockInformation;
    }

    private BlockSnapshot deserializeBlockSnapshot(String json) {
        Map<Object, Object> map = (Map) this.gson.fromJson(json, Map.class);
        DataContainer container = new MemoryDataContainer();
        Iterator var4 = map.entrySet().iterator();

        while (var4.hasNext()) {
            Entry<Object, Object> entry = (Entry) var4.next();
            container.set(DataQuery.of('.', entry.getKey().toString()), entry.getValue());
        }

        return BlockSnapshot.builder().build(container).get();
    }

    public void clearExpiredData(long timeThreshold) {
        LOG.info("Starting purge the expired data...");
        Sponge.getScheduler().createTaskBuilder().async().execute(() -> {
            try {
                Statement stmt = connection.createStatement();
                String sql = "DELETE FROM BLOCKINFO WHERE TIME < " + timeThreshold;
                stmt.executeUpdate(sql);
                stmt.close();
            } catch (SQLException var5) {
                var5.printStackTrace();
            }

        }).submit(Inspector.instance());
        LOG.info("clean up!");
    }
}
