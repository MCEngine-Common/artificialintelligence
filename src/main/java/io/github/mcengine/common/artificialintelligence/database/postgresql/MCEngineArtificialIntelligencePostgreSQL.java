package io.github.mcengine.common.artificialintelligence.database.postgresql;

import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.artificialintelligence.util.MCEngineArtificialIntelligenceApiUtilToken;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * PostgreSQL implementation of the AI API database.
 * Stores and retrieves encrypted player tokens.
 */
public class MCEngineArtificialIntelligencePostgreSQL implements IMCEngineArtificialIntelligenceDB {

    /** The Bukkit plugin instance. */
    private final Plugin plugin;

    /** Persistent PostgreSQL connection. */
    private final Connection conn;

    /**
     * Constructs a new PostgreSQL database handler using configuration values.
     * Required keys: database.postgresql.host, port, name, user, password.
     *
     * @param plugin The Bukkit plugin instance.
     */
    public MCEngineArtificialIntelligencePostgreSQL(Plugin plugin) {
        this.plugin = plugin;

        String host = plugin.getConfig().getString("database.postgresql.host", "localhost");
        String port = plugin.getConfig().getString("database.postgresql.port", "5432");
        String dbName = plugin.getConfig().getString("database.postgresql.name", "mcengine_ai");
        String user = plugin.getConfig().getString("database.postgresql.user", "postgres");
        String pass = plugin.getConfig().getString("database.postgresql.password", "");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

        Connection tempConn = null;
        try {
            tempConn = DriverManager.getConnection(jdbcUrl, user, pass);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to connect to PostgreSQL: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tempConn;

        createTable();
    }

    /**
     * Creates the 'artificialintelligence' table in PostgreSQL if it doesn't exist.
     */
    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS artificialintelligence (
                id SERIAL PRIMARY KEY,
                player_uuid TEXT NOT NULL,
                platform TEXT NOT NULL,
                token TEXT NOT NULL,
                UNIQUE(player_uuid, platform)
            );
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create PostgreSQL table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the current PostgreSQL database connection.
     *
     * @return Active {@link Connection} to the PostgreSQL database.
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
            ON CONFLICT (player_uuid, platform) DO UPDATE SET token = EXCLUDED.token;
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, platform);
            stmt.setString(3, encryptedToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save token in PostgreSQL: " + e.getMessage());
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
            plugin.getLogger().warning("Failed to retrieve token from PostgreSQL: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
