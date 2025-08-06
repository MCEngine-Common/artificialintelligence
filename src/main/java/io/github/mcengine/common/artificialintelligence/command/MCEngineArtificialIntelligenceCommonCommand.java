package io.github.mcengine.common.artificialintelligence.command;

import io.github.mcengine.common.artificialintelligence.MCEngineArtificialIntelligenceCommon;
import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.core.MCEngineCoreApi;
import io.github.mcengine.common.artificialintelligence.util.MCEngineArtificialIntelligenceCommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor for AI-related operations under the default "/ai" command.
 * <p>
 * Supports the following command structures:
 * <ul>
 *     <li>/ai default set token {platform} &lt;token&gt;</li>
 *     <li>/ai default get platform list</li>
 *     <li>/ai default get platform model list</li>
 *     <li>/ai default get platform {platform} model list</li>
 *     <li>/ai default get addon list</li>
 *     <li>/ai default get dlc list</li>
 * </ul>
 */
public class MCEngineArtificialIntelligenceCommonCommand implements CommandExecutor {

    /**
     * The main AI plugin API instance.
     */
    private final MCEngineArtificialIntelligenceCommon api;

    /**
     * The AI database interface for token storage.
     */
    private final IMCEngineArtificialIntelligenceDB db;

    /**
     * Constructs the default AI command executor.
     *
     * @param api The plugin's common AI handler.
     * @param db  The database handler for tokens.
     */
    public MCEngineArtificialIntelligenceCommonCommand(MCEngineArtificialIntelligenceCommon api, IMCEngineArtificialIntelligenceDB db) {
        this.api = api;
        this.db = db;
    }

    /**
     * Handles the execution of the default /ai command dispatcher logic.
     * Supports argument offset due to dispatcher passing "default" as the first argument.
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the alias used
     * @param args    the arguments passed, e.g. [default, get, platform, list]
     * @return true if the command was handled successfully
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command.");
            return true;
        }

        // Check minimum required args
        if (args.length < 2) {
            MCEngineArtificialIntelligenceCommandUtil.sendUsage(sender);
            return true;
        }

        // First arg is always "default" in this dispatcher setup
        if (!args[0].equalsIgnoreCase("default")) {
            sender.sendMessage("§cUnknown subcommand. Use '/ai default ...'");
            return true;
        }

        // Shift remaining arguments
        if (args.length < 3) {
            MCEngineArtificialIntelligenceCommandUtil.sendUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        String target = args[2].toLowerCase();

        switch (action) {
            case "set":
                if ("token".equals(target) && args.length == 5) {
                    String platform = args[3];
                    String token = args[4];
                    db.setPlayerToken(player.getUniqueId().toString(), platform, token);
                    player.sendMessage("§aSuccessfully set your token for platform: " + platform);
                    return true;
                }
                break;

            case "get":
                if ("platform".equals(target)) {
                    if (args.length == 4 && "list".equalsIgnoreCase(args[3])) {
                        return MCEngineArtificialIntelligenceCommandUtil.handlePlatformList(player);
                    }

                    if (args.length == 5 && "model".equalsIgnoreCase(args[3]) && "list".equalsIgnoreCase(args[4])) {
                        return MCEngineArtificialIntelligenceCommandUtil.handleModelList(player);
                    }

                    if (args.length == 6 && MCEngineArtificialIntelligenceCommandUtil.isValidKey(args[3])
                            && "model".equalsIgnoreCase(args[4])
                            && "list".equalsIgnoreCase(args[5])) {
                        return MCEngineArtificialIntelligenceCommandUtil.handleModelListByPlatform(player, args[3]);
                    }
                }

                if (args.length == 4 && "list".equalsIgnoreCase(args[3])
                        && ("addon".equals(target) || "dlc".equals(target))) {
                    return MCEngineCoreApi.handleExtensionList(player, api.getPlugin(), target);
                }
                break;
        }

        MCEngineArtificialIntelligenceCommandUtil.sendUsage(sender);
        return true;
    }
}
