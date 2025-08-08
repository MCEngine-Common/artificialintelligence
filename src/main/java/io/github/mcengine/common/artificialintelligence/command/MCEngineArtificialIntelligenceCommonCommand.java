package io.github.mcengine.common.artificialintelligence.command;

import io.github.mcengine.common.artificialintelligence.MCEngineArtificialIntelligenceCommon;
import io.github.mcengine.api.artificialintelligence.database.IMCEngineArtificialIntelligenceDB;
import io.github.mcengine.api.core.MCEngineCoreApi;
import io.github.mcengine.api.hologram.MCEngineHologramApi;
import io.github.mcengine.common.artificialintelligence.util.MCEngineArtificialIntelligenceCommandUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the <code>/ai default</code> command and subcommands for platform/model queries,
 * token management, and listing addons/DLC.
 * <p>
 * Supported syntaxes:
 * <ul>
 *     <li><code>/ai default set token {platform} &lt;token&gt;</code></li>
 *     <li><code>/ai default get platform list</code></li>
 *     <li><code>/ai default get platform model list</code></li>
 *     <li><code>/ai default get platform {platform} model list</code></li>
 *     <li><code>/ai default get addon list</code></li>
 *     <li><code>/ai default get dlc list</code></li>
 * </ul>
 */
public class MCEngineArtificialIntelligenceCommonCommand implements CommandExecutor {

    /**
     * AI plugin common API used by the command executor to access plugin-level services.
     */
    private final MCEngineArtificialIntelligenceCommon api;

    /**
     * AI database interface responsible for storing and retrieving per-player tokens.
     */
    private final IMCEngineArtificialIntelligenceDB db;

    /**
     * The command prefix suggested to players by the usage hologram. Clicking it will
     * prefill the chat input using {@link ClickEvent.Action#SUGGEST_COMMAND}.
     */
    private static final String SUGGEST_PREFIX = "/ai default ";

    /**
     * Default number of seconds the usage hologram should remain visible.
     * (Provided for parity with the currency command; visibility duration is handled by the API.)
     */
    private static final int DEFAULT_HOLOGRAM_SECONDS = 10;

    /**
     * Canonical usage lines (without the leading "Usage:" label). These are shown in chat
     * and mirrored inside the usage hologram for consistency.
     */
    private static final String[] USAGE_LINES = new String[]{
            "/ai default set token {platform} <token>",
            "/ai default get platform list",
            "/ai default get platform model list",
            "/ai default get platform {platform} model list",
            "/ai default get addon list",
            "/ai default get dlc list"
    };

    /**
     * Constructs the default AI command executor.
     *
     * @param api the plugin's common AI handler
     * @param db  the database handler for tokens
     */
    public MCEngineArtificialIntelligenceCommonCommand(MCEngineArtificialIntelligenceCommon api, IMCEngineArtificialIntelligenceDB db) {
        this.api = api;
        this.db = db;
    }

    /**
     * Handles execution of the <code>/ai default</code> command and its subcommands.
     * Supports argument offset due to the dispatcher passing "default" as the first argument.
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the alias used
     * @param args    the arguments passed, e.g. <code>[default, get, platform, list]</code>
     * @return {@code true} if the command was handled successfully
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
            // Also display the usage hologram for parity with the currency command.
            new MCEngineHologramApi().getUsageHologram(player, SUGGEST_PREFIX, USAGE_LINES);
            return true;
        }

        // First arg is always "default" in this dispatcher setup
        if (!args[0].equalsIgnoreCase("default")) {
            sender.sendMessage("§cUnknown subcommand. Use '/ai default ...'");
            // Mirror usage in a hologram for discoverability.
            new MCEngineHologramApi().getUsageHologram(player, SUGGEST_PREFIX, USAGE_LINES);
            return true;
        }

        // Shift remaining arguments
        if (args.length < 3) {
            MCEngineArtificialIntelligenceCommandUtil.sendUsage(sender);
            new MCEngineHologramApi().getUsageHologram(player, SUGGEST_PREFIX, USAGE_LINES);
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

        // Fallback: show textual usage and the matching hologram for a consistent UX.
        MCEngineArtificialIntelligenceCommandUtil.sendUsage(sender);
        new MCEngineHologramApi().getUsageHologram(player, SUGGEST_PREFIX, USAGE_LINES);
        return true;
    }
}
