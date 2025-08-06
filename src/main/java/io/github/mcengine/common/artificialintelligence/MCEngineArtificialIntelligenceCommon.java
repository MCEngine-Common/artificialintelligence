package io.github.mcengine.common.artificialintelligence;

import com.google.gson.JsonObject;
import io.github.mcengine.api.artificialintelligence.MCEngineArtificialIntelligenceApi;
import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.artificialintelligence.model.IMCEngineArtificialIntelligenceApiModel;
import io.github.mcengine.api.core.util.MCEngineCoreApiDispatcher;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Common logic handler for the MCEngine Artificial Intelligence plugin.
 * Handles database initialization, model registration, token management, and AI task execution.
 */
public class MCEngineArtificialIntelligenceCommon {

    /**
     * Singleton instance of this common AI manager.
     */
    private static MCEngineArtificialIntelligenceCommon instance;

    /**
     * Database interface used to persist and retrieve player tokens.
     */
    private final IMCEngineArtificialIntelligenceDB db;

    /**
     * The Bukkit plugin associated with this AI integration.
     */
    private final Plugin plugin;

    /**
     * Instance of the MCEngine AI API handler.
     */
    private final MCEngineArtificialIntelligenceApi api;

    /**
     * Internal command dispatcher used for registering command namespaces and subcommands.
     */
    private final MCEngineCoreApiDispatcher dispatcher;

    /**
     * Constructs a new AI Common handler.
     * Initializes the appropriate database backend, function loader, and prepares model registration.
     *
     * Supported database types (config key: {@code database.type}):
     * <ul>
     *     <li>{@code sqlite}</li>
     *     <li>{@code mysql}</li>
     *     <li>{@code postgresql}</li>
     * </ul>
     *
     * @param plugin The Bukkit plugin instance.
     */
    public MCEngineArtificialIntelligenceCommon(Plugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.api = new MCEngineArtificialIntelligenceApi();
        this.dispatcher = new MCEngineCoreApiDispatcher();

        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        switch (dbType) {
            case "sqlite" -> this.db = new MCEngineArtificialIntelligenceSQLite(plugin);
            case "mysql" -> this.db = new MCEngineArtificialIntelligenceMySQL(plugin);
            case "postgresql" -> this.db = new MCEngineArtificialIntelligencePostgreSQL(plugin);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }

        // Initialize function calling loader using configured folder and default logger
        String folderPath = plugin.getConfig().getString("function-calling.path", "function-calling");
        Logger logger = plugin.getLogger();
        api.initializeFunctionCallingLoader(plugin, folderPath, logger);
    }

    /**
     * Returns the global singleton instance.
     *
     * @return Singleton instance of {@link MCEngineArtificialIntelligenceCommon}.
     */
    public static MCEngineArtificialIntelligenceCommon getApi() {
        return instance;
    }

    /**
     * Gets the associated plugin instance.
     *
     * @return The Bukkit plugin.
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the database handler implementation.
     *
     * @return The database interface.
     */
    public IMCEngineArtificialIntelligenceDB getDB() {
        return db;
    }

    /**
     * Retrieves the active SQL database connection.
     *
     * @return JDBC {@link Connection} used by the plugin.
     */
    public Connection getDBConnection() {
        return db.getDBConnection();
    }

    /**
     * Registers a command namespace (e.g. "plugin1") for this plugin's dispatcher.
     *
     * @param namespace unique namespace for commands
     */
    public void registerNamespace(String namespace) {
        dispatcher.registerNamespace(namespace);
    }

    /**
     * Binds a Bukkit command (like /example1) to the internal dispatcher.
     *
     * @param namespace       the command namespace
     * @param commandExecutor fallback executor
     */
    public void bindNamespaceToCommand(String namespace, CommandExecutor commandExecutor) {
        dispatcher.bindNamespaceToCommand(namespace, commandExecutor);
    }

    /**
     * Registers a subcommand under the specified namespace.
     *
     * @param namespace the command namespace
     * @param name      subcommand label
     * @param executor  subcommand logic
     */
    public void registerSubCommand(String namespace, String name, CommandExecutor executor) {
        dispatcher.registerSubCommand(namespace, name, executor);
    }

    /**
     * Registers a tab completer for a subcommand under the specified namespace.
     *
     * @param namespace    the command namespace
     * @param subcommand   subcommand label
     * @param tabCompleter tab completion logic
     */
    public void registerSubTabCompleter(String namespace, String subcommand, TabCompleter tabCompleter) {
        dispatcher.registerSubTabCompleter(namespace, subcommand, tabCompleter);
    }

    /**
     * Gets the dispatcher instance to assign as command executor and tab completer.
     *
     * @param namespace command namespace
     * @return command executor for Bukkit command registration
     */
    public CommandExecutor getDispatcher(String namespace) {
        return dispatcher.getDispatcher(namespace);
    }

    /**
     * Stores or updates a player's API token for a platform.
     *
     * @param playerUuid UUID of the player as a string.
     * @param platform   Platform name (e.g., {@code openai}).
     * @param token      API token to store.
     */
    public void setPlayerToken(String playerUuid, String platform, String token) {
        db.setPlayerToken(playerUuid, platform, token);
    }

    /**
     * Retrieves a stored token for a given player and platform.
     *
     * @param playerUuid UUID of the player as a string.
     * @param platform   Platform name.
     * @return Token string or {@code null} if not found.
     */
    public String getPlayerToken(String playerUuid, String platform) {
        return db.getPlayerToken(playerUuid, platform);
    }

    /**
     * Registers an AI model under the specified platform.
     *
     * @param platform The platform name (e.g., {@code openai}, {@code customurl}).
     * @param model    The model name or alias.
     */
    public void registerModel(String platform, String model) {
        api.registerModel(plugin, platform, model);
    }

    /**
     * Retrieves an AI model by platform and model name.
     *
     * @param platform The platform name.
     * @param model    The model name.
     * @return Registered model instance.
     */
    public IMCEngineArtificialIntelligenceApiModel getAi(String platform, String model) {
        return api.getAi(platform, model);
    }

    /**
     * Gets all registered AI models grouped by platform.
     *
     * @return Nested map of platform → model → model instance.
     */
    public Map<String, Map<String, IMCEngineArtificialIntelligenceApiModel>> getAiAll() {
        return api.getAiAll();
    }

    /**
     * Sends a direct request to a model using the default configured token and a system prompt.
     *
     * @param platform     The AI platform name.
     * @param model        The model name.
     * @param systemPrompt The system prompt providing context or behavior instructions.
     * @param message      Prompt to send to the AI.
     * @return The full JSON response object from the AI.
     */
    public JsonObject getResponse(String platform, String model, String systemPrompt, String message) {
        return api.getResponse(platform, model, systemPrompt, message);
    }

    /**
     * Sends a direct request to a model using a custom token and a system prompt.
     *
     * @param platform     The AI platform name.
     * @param model        The model name.
     * @param token        API token.
     * @param systemPrompt The system prompt providing context or behavior instructions.
     * @param message      Prompt to send to the AI.
     * @return The full JSON response object from the AI.
     */
    public JsonObject getResponse(String platform, String model, String token, String systemPrompt, String message) {
        return api.getResponse(platform, model, token, systemPrompt, message);
    }

    /**
     * Starts a bot task for a player asynchronously using either a server or player token.
     *
     * @param player    Player initiating the request.
     * @param tokenType Token type: {@code "server"} or {@code "player"}.
     * @param platform  AI platform name.
     * @param model     AI model name.
     * @param message   Prompt message.
     */
    public void runBotTask(Player player, String tokenType, String platform, String model, String message) {
        api.runBotTask(plugin, db, player, tokenType, platform, model, message);
    }

    /**
     * Marks a player as waiting for a response.
     *
     * @param player  Player instance.
     * @param waiting Whether the player is waiting.
     */
    public void setWaiting(Player player, boolean waiting) {
        api.setWaiting(player, waiting);
    }

    /**
     * Checks if a player is currently marked as waiting.
     *
     * @param player Player to check.
     * @return {@code true} if waiting; {@code false} otherwise.
     */
    public boolean checkWaitingPlayer(Player player) {
        return api.checkWaitingPlayer(player);
    }

    /**
     * Extracts the text content from the AI's full JSON response.
     *
     * @param responseJson Full JSON response from the AI API.
     * @return Plain text message content.
     */
    public String getCompletionContent(JsonObject responseJson) {
        return api.getCompletionContent(responseJson);
    }

    /**
     * Extracts the total token usage from the AI's full JSON response.
     *
     * @param responseJson Full JSON response from the AI API.
     * @return Total tokens used, or {@code -1} if unavailable.
     */
    public int getTotalTokenUsage(JsonObject responseJson) {
        return api.getTotalTokenUsage(responseJson);
    }

    /**
     * Matches a player message against predefined rule-based functions and returns the first response match.
     *
     * @param player The player who sent the message.
     * @param msg    The message content to match.
     * @return A resolved response string, or {@code null} if no match is found.
     */
    public String getMessageMatch(Player player, String msg) {
        return api.getMessageMatch(player, msg);
    }

    /**
     * Initializes the function-calling rule loader for chatbot behavior matching.
     *
     * @param folderPath Path to the rules directory (relative to plugin data folder).
     * @param logger     Logger for diagnostic output.
     */
    public void initializeFunctionCallingLoader(String folderPath, Logger logger) {
        api.initializeFunctionCallingLoader(plugin, folderPath, logger);
    }
}
