package io.github.mcengine.common.artificialintelligence.tabcompleter;

import io.github.mcengine.api.artificialintelligence.util.MCEngineArtificialIntelligenceApiUtilAi;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Tab completer for AI commands.
 * Supports auto-completion for:
 * - /ai set token {platform} <token>
 * - /ai get platform list
 * - /ai get platform model list
 * - /ai get platform {platform} model list
 * - /ai get addon list
 * - /ai get dlc list
 */
public class MCEngineArtificialIntelligenceCommonTabCompleter implements TabCompleter {

    /**
     * Reference to the Bukkit plugin instance used to access the configuration.
     */
    private final Plugin plugin;

    /**
     * Top-level command keywords available at /ai <first>.
     * Includes: "set" and "get".
     */
    private static final List<String> FIRST = Arrays.asList("set", "get");

    /**
     * Second-level keywords after "set".
     * Used for: /ai set <second>.
     * Includes: "token".
     */
    private static final List<String> SECOND_SET = Arrays.asList("token");

    /**
     * Second-level keywords after "get".
     * Used for: /ai get <second>.
     * Includes: "platform", "addon", "dlc".
     */
    private static final List<String> SECOND_GET = Arrays.asList("platform", "addon", "dlc");

    /**
     * Third-level keywords for listing models.
     * Used for: /ai get model <third>.
     * Includes: "list".
     */
    private static final List<String> LIST_KEYWORD = Arrays.asList("list");
    /**
     * Supported AI platform identifiers used when setting tokens.
     */
    private static final List<String> PLATFORMS = Arrays.asList("openai", "deepseek", "openrouter");

    /**
     * Constructs a new tab completer using the plugin instance for config access.
     *
     * @param plugin The Bukkit plugin instance.
     */
    public MCEngineArtificialIntelligenceCommonTabCompleter(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles tab completion for /ai commands.
     *
     * @param sender  The source of the command.
     * @param command The command being executed.
     * @param alias   The alias used.
     * @param args    The command arguments.
     * @return A list of tab completion suggestions.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("ai")) return null;

        final String arg0 = args.length > 0 ? args[0].toLowerCase() : "";
        final String arg1 = args.length > 1 ? args[1].toLowerCase() : "";
        final String arg2 = args.length > 2 ? args[2] : "";
        final String arg3 = args.length > 3 ? args[3].toLowerCase() : "";

        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> completions.addAll(FIRST);

            case 2 -> {
                if ("set".equals(arg0)) completions.addAll(SECOND_SET);
                else if ("get".equals(arg0)) completions.addAll(SECOND_GET);
            }

            case 3 -> {
                if ("get".equals(arg0)) {
                    if ("platform".equals(arg1)) {
                        completions.add("list");
                        completions.add("model");
                        completions.addAll(getAllValidPlatforms());
                    } else if ("addon".equals(arg1) || "dlc".equals(arg1)) {
                        completions.add("list");
                    }
                } else if ("set".equals(arg0) && "token".equals(arg1)) {
                    completions.addAll(PLATFORMS);
                    completions.addAll(getCustomServers());
                }
            }

            case 4 -> {
                if ("get".equals(arg0) && "platform".equals(arg1)) {
                    if (getAllValidPlatforms().contains(arg2)) completions.add("model");
                    if ("model".equals(arg2.toLowerCase())) completions.add("list");
                } else if ("set".equals(arg0) && "token".equals(arg1)) {
                    completions.add("<your_token>");
                }
            }

            case 5 -> {
                if ("get".equals(arg0) && "platform".equals(arg1) &&
                    getAllValidPlatforms().contains(arg2) && "model".equals(arg3)) {
                    completions.add("list");
                }
            }
        }

        return completions.isEmpty() ? null : completions;
    }

    /**
     * Retrieves custom server names from the config under ai.custom.
     *
     * @return A list of "customurl:{server}" entries.
     */
    private List<String> getCustomServers() {
        List<String> custom = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ai.custom");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                custom.add("customurl:" + key);
            }
        }
        return custom;
    }

    /**
     * Retrieves all registered platforms for model listing.
     * Used for: /ai get {platform} model list
     *
     * @return A list of valid platform identifiers.
     */
    private List<String> getAllValidPlatforms() {
        Map<String, Map<String, ?>> models = MCEngineArtificialIntelligenceApiUtilAi.getAllModels();
        return new ArrayList<>(models.keySet());
    }
}
