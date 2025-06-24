package io.github.mcengine.common.artificialintelligence.database.mysql;

import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.artificialintelligence.util.MCEngineArtificialIntelligenceApiUtilToken;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * MySQL implementation of the AI API database.
 * Stores and retrieves encrypted player tokens.
 */
public class MCEngineArtificialIntelligenceMySQL implements IMCEngineArtificialIntelligenceDB {

    /** The Bukkit plugin instance. */
    private final Plugin plugin;

    /** Persistent MySQL connection. */
    private final Connection conn;

    /**
     * Constructs a new MySQL database handler using configuration values.
     * Required keys: database.mysql.host, port, name, user, password.
     *
     * @param plugin The Bukkit plugin instance.
     */
    public MCEngineArtificialIntelligenceMySQL(Plugin plugin) {
        this.plugin = plugin;

        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        String port = plugin.getConfig().getString("database.mysql.port", "3306");
        String dbName = plugin.getConfig().getString("database.mysql.name", "mcengine_ai");
        String user = plugin.getConfig().getString("database.mysql.user", "root");
        String pass = plugin.getConfig().getString("database.mysql.password", "");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true";

        Connection tempConn = null;
        try {
            tempConn = DriverManager.getConnection(jdbcUrl, user, pass);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to connect to MySQL: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tempConn;

        createTable();
    }

    /**
     * Creates the 'artificialintelligence' table in MySQL if it doesn't exist.
     */
    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS artificialintelligence (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                platform VARCHAR(64) NOT NULL,
                token TEXT NOT NULL,
                UNIQUE KEY unique_player_platform (player_uuid, platform)
            );
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create MySQL table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the current MySQL database connection.
     *
     * @return Active {@link Connection} to the MySQL database.
     */
    @Override
    public Connection getDBConnection() {
        return conn;
    }

    /**
     * Stores or updates the encrypted token for a player and platform.
     *
     * @param playerUuid The UUID of the player.
     * @param platform   The AI platform.
     * @param token      The raw token to encrypt and save.
     */
    @Override
    public void setPlayerToken(String playerUuid, String platform, String token) {
        String encryptedToken = MCEngineArtificialIntelligenceApiUtilToken.encryptToken(token);

        String sql = """
            INSERT INTO artificialintelligence (player_uuid, platform, token)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE token = VALUES(token);
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, platform);
            stmt.setString(3, encryptedToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save token in MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the encrypted token for a player and platform.
     *
     * @param playerUuid The UUID of the player.
     * @param platform   The platform name.
     * @return The token string, or null if not found.
     */
    @Override
    public String getPlayerToken(String playerUuid, String platform) {
        String sql = "SELECT token FROM artificialintelligence WHERE player_uuid = ? AND platform = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, platform);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("token");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to retrieve token from MySQL: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
