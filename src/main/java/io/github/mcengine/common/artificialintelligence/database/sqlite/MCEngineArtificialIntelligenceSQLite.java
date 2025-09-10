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

    /** The Bukkit plugin instance used for configuration and logging. */
    private final Plugin plugin;

    /** The JDBC SQLite database URL built from the configured path. */
    private final String databaseUrl;

    /** SQLite persistent database connection created at construction time. */
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
                if (dbFile.getParentFile() != null) dbFile.getParentFile().mkdirs();
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
            try (Statement pragma = tempConn.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
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

    /** {@inheritDoc} */
    @Override
    public void executeQuery(String query) {
        try (Statement st = conn.createStatement()) {
            st.execute(query);
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite AI executeQuery failed: " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(String query, Class<T> type) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                Object v;
                if (type == String.class) v = rs.getString(1);
                else if (type == Integer.class) v = rs.getInt(1);
                else if (type == Long.class) v = rs.getLong(1);
                else if (type == Double.class) v = rs.getDouble(1);
                else if (type == Boolean.class) v = rs.getBoolean(1);
                else throw new IllegalArgumentException("Unsupported return type: " + type);
                return (T) v;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite AI getValue failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns the persistent SQLite database connection.
     *
     * @return Active {@link Connection} to the SQLite database.
     * @deprecated Direct connection access has been removed from the public interface. Use
     *             {@link #executeQuery(String)} and {@link #getValue(String, Class)} instead.
     */
    @Deprecated
    public Connection getDBConnectionLegacy() {
        return conn;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
