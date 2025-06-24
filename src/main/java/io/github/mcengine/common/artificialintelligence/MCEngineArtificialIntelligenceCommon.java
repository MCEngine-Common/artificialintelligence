package io.github.mcengine.common.artificialintelligence;

import io.github.mcengine.api.artificialintelligence.MCEngineArtificialIntelligenceApi;
import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.artificialintelligence.model.IMCEngineArtificialIntelligenceApiModel;
import io.github.mcengine.common.artificialintelligence.database.mysql.MCEngineArtificialIntelligenceMySQL;
import io.github.mcengine.common.artificialintelligence.database.postgresql.MCEngineArtificialIntelligencePostgreSQL;
import io.github.mcengine.common.artificialintelligence.database.sqlite.MCEngineArtificialIntelligenceSQLite;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.util.Map;

/**
 * Common logic handler for the MCEngine Artificial Intelligence plugin.
 * Handles database initialization, model registration, token management, and AI task execution.
 */
public class MCEngineArtificialIntelligenceCommon {

    /**
     * Singleton instance of the API.
     */
    private static MCEngineArtificialIntelligenceCommon instance;

    /**
     * Database handler instance for storing and retrieving player tokens.
     */
    private final IMCEngineArtificialIntelligenceDB db;

    /**
     * The Bukkit plugin instance associated with this AI API.
     */
    private final Plugin plugin;

    /**
     * Instance of the main AI API handler.
     */
    private final MCEngineArtificialIntelligenceApi api;

    /**
     * Constructs a new AI Common handler.
     * Initializes the appropriate database backend and prepares the environment for model registration and AI tasks.
     *
     * Supported database types (config key: {@code database.type}): {@code sqlite}, {@code mysql}, {@code postgresql}.
     *
     * @param plugin The Bukkit plugin instance.
     */
    public MCEngineArtificialIntelligenceCommon(Plugin plugin) {
        this.plugin = plugin;
        this.api = new MCEngineArtificialIntelligenceApi();

        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        switch (dbType) {
            case "sqlite" -> this.db = new MCEngineArtificialIntelligenceSQLite(plugin);
            case "mysql" -> this.db = new MCEngineArtificialIntelligenceMySQL(plugin);
            case "postgresql" -> this.db = new MCEngineArtificialIntelligencePostgreSQL(plugin);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    /**
     * Returns the global API singleton instance.
     *
     * @return The {@link MCEngineArtificialIntelligenceCommon} instance.
     */
    public static MCEngineArtificialIntelligenceCommon getApi() {
        return instance;
    }

    /**
     * Returns the Bukkit plugin instance linked to this API.
     *
     * @return The plugin instance.
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the database handler implementation.
     *
     * @return The database API implementation.
     */
    public IMCEngineArtificialIntelligenceDB getDB() {
        return db;
    }

    /**
     * Retrieves the active database connection used by the AI plugin.
     *
     * @return The {@link Connection} instance for the configured database.
     */
    public Connection getDBConnection() {
        return db.getDBConnection();
    }

    /**
     * Stores or updates a player's API token for a given AI platform.
     *
     * @param playerUuid The UUID of the player.
     * @param platform   The AI platform (e.g., {@code openai}).
     * @param token      The raw API token to store.
     */
    public void setPlayerToken(String playerUuid, String platform, String token) {
        db.setPlayerToken(playerUuid, platform, token);
    }

    /**
     * Retrieves the stored token for a given player and platform.
     *
     * @param playerUuid The UUID of the player.
     * @param platform   The AI platform name.
     * @return The stored token, or {@code null} if none exists.
     */
    public String getPlayerToken(String playerUuid, String platform) {
        return db.getPlayerToken(playerUuid, platform);
    }

    /**
     * Registers a model under the specified platform.
     *
     * @param platform The platform name (e.g., {@code openai}, {@code customurl}).
     * @param model    The model identifier (e.g., {@code gpt-4}, {@code server:my-custom-model}).
     */
    public void registerModel(String platform, String model) {
        api.registerModel(plugin, platform, model);
    }

    /**
     * Retrieves an AI model instance by platform and model name.
     *
     * @param platform The platform name.
     * @param model    The model name.
     * @return The model interface, or {@code null} if not registered.
     */
    public IMCEngineArtificialIntelligenceApiModel getAi(String platform, String model) {
        return api.getAi(platform, model);
    }

    /**
     * Returns all registered AI models grouped by platform and model name.
     *
     * @return A nested map: platform → model → model instance.
     */
    public Map<String, Map<String, IMCEngineArtificialIntelligenceApiModel>> getAiAll() {
        return api.getAiAll();
    }

    /**
     * Gets a direct response from a registered AI model.
     *
     * @param platform The AI platform.
     * @param model    The model name.
     * @param message  The prompt message to send.
     * @return The AI-generated response.
     */
    public String getResponse(String platform, String model, String message) {
        return api.getResponse(platform, model, message);
    }

    /**
     * Gets a direct response from an AI model using a specific API token.
     *
     * @param platform The AI platform.
     * @param model    The model name.
     * @param token    The API token to use.
     * @param message  The prompt message.
     * @return The AI-generated response.
     */
    public String getResponse(String platform, String model, String token, String message) {
        return api.getResponse(platform, model, token, message);
    }

    /**
     * Executes an AI bot task asynchronously with the given input and context.
     *
     * @param player    The player who initiated the task.
     * @param tokenType The type of token to use: {@code "server"} or {@code "player"}.
     * @param platform  The AI platform name.
     * @param model     The AI model name.
     * @param message   The prompt message.
     */
    public void runBotTask(Player player, String tokenType, String platform, String model, String message) {
        api.runBotTask(plugin, db, player, tokenType, platform, model, message);
    }

    /**
     * Sets the waiting status of a player during AI interaction.
     *
     * @param player  The player.
     * @param waiting {@code true} if the player is waiting for a response; otherwise {@code false}.
     */
    public void setWaiting(Player player, boolean waiting) {
        api.setWaiting(player, waiting);
    }

    /**
     * Checks whether a player is currently waiting for an AI response.
     *
     * @param player The player to check.
     * @return {@code true} if waiting; otherwise {@code false}.
     */
    public boolean checkWaitingPlayer(Player player) {
        return api.checkWaitingPlayer(player);
    }
}
