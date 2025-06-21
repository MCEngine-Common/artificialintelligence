package io.github.mcengine.common.artificialintelligence.command;

import io.github.mcengine.api.artificialintelligence.MCEngineArtificialIntelligenceApi;
import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.mcengine.MCEngineApi;
import io.github.mcengine.common.artificialintelligence.command.MCEngineArtificialIntelligenceCommonCommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor for AI-related operations.
 * <p>
 * Supported commands:
 * - /ai set token {platform} <token>
 * - /ai get platform list
 * - /ai get platform model list
 * - /ai get platform {platform} model list
 * - /ai get addon list
 * - /ai get dlc list
 */
public class MCEngineArtificialIntelligenceCommonCommand implements CommandExecutor {

    /**
     * The main plugin instance used for accessing plugin-specific utilities,
     * such as file operations, configuration, and extension loading.
     */
    private final MCEngineArtificialIntelligenceApi api;

    /**
     * Database instance used to persist and retrieve player tokens.
     */
    private final IMCEngineArtificialIntelligenceDB db;

    /**
     * Constructs the command executor using the provided API instance.
     *
     * @param api The MCEngineArtificialIntelligenceApi instance.
     */
    public MCEngineArtificialIntelligenceCommonCommand(MCEngineArtificialIntelligenceApi api) {
        this.api = api;
        this.db = api.getDB();
    }

    /**
     * Handles command execution for /ai commands.
     *
     * @param sender  The sender of the command.
     * @param command The command that was executed.
     * @param label   The command alias used.
     * @param args    The command arguments.
     * @return true if the command was handled successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command.");
            return true;
        }

        if (args.length < 3) {
            MCEngineArtificialIntelligenceCommonCommandUtil.sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        String target = args[1].toLowerCase();

        switch (action) {
            case "set":
                if ("token".equals(target) && args.length == 4) {
                    String platform = args[2];
                    String token = args[3];
                    db.setPlayerToken(player.getUniqueId().toString(), platform, token);
                    player.sendMessage("§aSuccessfully set your token for platform: " + platform);
                    return true;
                }
                break;

            case "get":
                if ("platform".equals(target)) {
                    if ("list".equalsIgnoreCase(args[2]) && args.length == 3) {
                        return MCEngineArtificialIntelligenceCommonCommandUtil.handlePlatformList(player);
                    }

                    if (args.length == 4 && "model".equalsIgnoreCase(args[2]) && "list".equalsIgnoreCase(args[3])) {
                        return MCEngineArtificialIntelligenceCommonCommandUtil.handleModelList(player);
                    }

                    if (args.length == 5 && MCEngineArtificialIntelligenceCommonCommandUtil.isValidKey(args[2])
                            && "model".equalsIgnoreCase(args[3])
                            && "list".equalsIgnoreCase(args[4])) {
                        return MCEngineArtificialIntelligenceCommonCommandUtil.handleModelListByPlatform(player, args[2]);
                    }
                }

                if (args.length == 3 && "list".equalsIgnoreCase(args[2])
                        && ("addon".equals(target) || "dlc".equals(target))) {
                    return MCEngineApi.handleExtensionList(player, api.getPlugin(), target);
                }
                break;
        }

        MCEngineArtificialIntelligenceCommonCommandUtil.sendUsage(sender);
        return true;
    }
}
