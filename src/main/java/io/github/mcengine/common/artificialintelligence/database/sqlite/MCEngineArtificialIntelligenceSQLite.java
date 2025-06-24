package io.github.mcengine.common.artificialintelligence.database.sqlite;

import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.artificialintelligence.util.MCEngineArtificialIntelligenceApiUtilToken;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;

/**
 * SQLite implementation for the AI API database.
 * Handles encrypted player token storage and retrieval.
 */
public class MCEngineArtificialIntelligenceSQLite implements IMCEngineArtificialIntelligenceDB {

    /** The Bukkit plugin instance. */
    private final Plugin plugin;

    /** The JDBC SQLite database URL. */
    private final String databaseUrl;

    /** SQLite persistent database connection. */
    private final Connection conn;

    /**
     * Constructs a new SQLite database handler from plugin config.
     * Path is retrieved from config key: database.sqlite.path.
     *
     * @param plugin The Bukkit plugin instance.
     */
    public MCEngineArtificialIntelligenceSQLite(Plugin plugin) {
        this.plugin = plugin;
        String fileName = plugin.getConfig().getString("database.sqlite.path", "artificialintelligence.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);

        // Create the file if it doesn't exist
        if (!dbFile.exists()) {
            try {
                boolean created = dbFile.createNewFile();
                if (created) {
                    plugin.getLogger().info("SQLite database file created: " + dbFile.getAbsolutePath());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create SQLite database file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.databaseUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        Connection tempConn = null;
        try {
            tempConn = DriverManager.getConnection(databaseUrl);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to open SQLite connection: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tempConn;

        createTable();
    }

    /**
     * Creates the 'artificialintelligence' table if it does not exist.
     * Columns: id (auto-increment), player_uuid, platform, token.
     */
    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS artificialintelligence (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                platform TEXT NOT NULL,
                token TEXT NOT NULL,
                UNIQUE(player_uuid, platform)
            );
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create SQLite table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the persistent SQLite database connection.
     *
     * @return The active {@link Connection} to the SQLite database.
     */
    @Override
    public Connection getDBConnection() {
        return conn;
    }

    /**
     * Sets or updates the encrypted token for a given player UUID and platform.
     *
     * @param playerUuid The UUID of the player.
     * @param platform   The AI platform.
     * @param token      The raw (unencrypted) token to store.
     */
    @Override
    public void setPlayerToken(String playerUuid, String platform, String token) {
        String encryptedToken = MCEngineArtificialIntelligenceApiUtilToken.encryptToken(token);

        String sql = """
            INSERT INTO artificialintelligence (player_uuid, platform, token)
            VALUES (?, ?, ?)
            ON CONFLICT(player_uuid, platform) DO UPDATE SET token = excluded.token;
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, platform);
            stmt.setString(3, encryptedToken);
            stmt.executeUpdate();
            plugin.getLogger().info("Token saved successfully.");
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save token: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the encrypted token for a player on a specific platform.
     *
     * @param playerUuid The UUID of the player.
     * @param platform   The AI platform.
     * @return The encrypted token or null if not found.
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
            plugin.getLogger().warning("Failed to retrieve token: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
