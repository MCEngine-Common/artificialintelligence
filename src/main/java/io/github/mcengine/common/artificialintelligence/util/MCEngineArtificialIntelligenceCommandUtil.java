package io.github.mcengine.common.artificialintelligence.util;

import io.github.mcengine.api.artificialintelligence.util.MCEngineArtificialIntelligenceApiUtilAi;
import io.github.mcengine.api.core.util.MCEngineApiUtilExtension;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for AI command logic extracted from MCEngineArtificialIntelligenceCommonCommand.
 */
public class MCEngineArtificialIntelligenceCommandUtil {

    /**
     * Displays all registered AI models by platform to the player.
     * Groups models by server if the platform is "customurl".
     *
     * @param player The player to send the model list to.
     * @return true after displaying the list.
     */
    public static boolean handleModelList(Player player) {
        Map<String, Map<String, ?>> models = MCEngineArtificialIntelligenceApiUtilAi.getAllModels();
        if (models.isEmpty()) {
            player.sendMessage("§cNo models are currently registered.");
            return true;
        }

        player.sendMessage("§eRegistered AI Models:");

        for (String platform : getSortedPlatformKeys(models)) {
            Map<String, ?> modelMap = models.get(platform);
            player.sendMessage("§7Platform: §b" + platform);

            if (platform.equalsIgnoreCase("customurl")) {
                Map<String, List<String>> serverModels = new TreeMap<>();

                for (String key : modelMap.keySet()) {
                    String[] parts = key.split(":", 2);
                    if (parts.length == 2 && isValidKey(parts[0]) && isValidKey(parts[1])) {
                        serverModels.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
                    }
                }

                for (Map.Entry<String, List<String>> entry : serverModels.entrySet()) {
                    player.sendMessage("  §7- " + entry.getKey());
                    for (String model : entry.getValue().stream().sorted().toList()) {
                        player.sendMessage("    §7- §f" + model);
                    }
                }

            } else {
                modelMap.keySet().stream()
                        .filter(MCEngineArtificialIntelligenceCommandUtil::isValidKey)
                        .sorted()
                        .forEach(modelName -> player.sendMessage("  §7- §f" + modelName));
            }
        }
        return true;
    }

    /**
     * Displays AI models for a specific platform to the player.
     * If the platform is "customurl", groups models by server.
     *
     * @param player   The player to send the model list to.
     * @param platform The platform to filter models by.
     * @return true after displaying the list.
     */
    public static boolean handleModelListByPlatform(Player player, String platform) {
        Map<String, Map<String, ?>> models = MCEngineArtificialIntelligenceApiUtilAi.getAllModels();
        if (!models.containsKey(platform)) {
            player.sendMessage("§cPlatform not found: " + platform);
            return true;
        }

        Map<String, ?> modelMap = models.get(platform);
        player.sendMessage("§eModels for platform §b" + platform + "§e:");

        if (platform.equalsIgnoreCase("customurl")) {
            Map<String, List<String>> serverModels = new TreeMap<>();
            for (String key : modelMap.keySet()) {
                String[] parts = key.split(":", 2);
                if (parts.length == 2 && isValidKey(parts[0]) && isValidKey(parts[1])) {
                    serverModels.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
                }
            }
            for (Map.Entry<String, List<String>> entry : serverModels.entrySet()) {
                player.sendMessage("  §7- " + entry.getKey());
                for (String model : entry.getValue().stream().sorted().toList()) {
                    player.sendMessage("    §7- §f" + model);
                }
            }
        } else {
            modelMap.keySet().stream()
                    .filter(MCEngineArtificialIntelligenceCommandUtil::isValidKey)
                    .sorted()
                    .forEach(modelName -> player.sendMessage("  §7- §f" + modelName));
        }
        return true;
    }

    /**
     * Displays a clickable list of all available platforms to the player.
     * "customurl" platforms show grouped server entries.
     *
     * @param player The player to send the platform list to.
     * @return true after displaying the list.
     */
    public static boolean handlePlatformList(Player player) {
        Map<String, Map<String, ?>> models = MCEngineArtificialIntelligenceApiUtilAi.getAllModels();
        if (models.isEmpty()) {
            player.sendMessage("§cNo platforms are currently registered.");
            return true;
        }

        player.sendMessage("§eRegistered Platforms:");
        for (String platform : getSortedPlatformKeys(models)) {
            Map<String, ?> entry = models.get(platform);
            if (platform.equalsIgnoreCase("customurl")) {
                player.sendMessage("§7- §b" + platform);

                List<String> servers = entry.keySet().stream()
                        .map(key -> key.split(":", 2)[0])
                        .filter(MCEngineArtificialIntelligenceCommandUtil::isValidKey)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                for (String server : servers) {
                    TextComponent serverComponent = new TextComponent("   §7- §f");
                    TextComponent clickableServer = new TextComponent(server);
                    clickableServer.setClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/ai set token customurl:" + server + " "
                    ));
                    serverComponent.addExtra(clickableServer);
                    player.spigot().sendMessage(serverComponent);
                }

            } else {
                TextComponent platformComponent = new TextComponent("§7- ");
                TextComponent clickablePlatform = new TextComponent("§b" + platform);
                clickablePlatform.setClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        "/ai set token " + platform + " "
                ));
                platformComponent.addExtra(clickablePlatform);
                player.spigot().sendMessage(platformComponent);
            }
        }
        return true;
    }

    /**
     * Sends usage information for the AI command to the sender.
     *
     * @param sender The sender (player or console) to send usage instructions to.
     */
    public static void sendUsage(CommandSender sender) {
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§7/ai set token {platform} <token>");
        sender.sendMessage("§7/ai get addon list");
        sender.sendMessage("§7/ai get dlc list");
        sender.sendMessage("§7/ai get platform list");
        sender.sendMessage("§7/ai get platform model list");
        sender.sendMessage("§7/ai get platform {platform} model list");
    }

    /**
     * Returns sorted platform keys, ensuring "customurl" is placed last.
     *
     * @param map The map whose keys are platform names.
     * @return A list of sorted keys.
     */
    public static List<String> getSortedPlatformKeys(Map<String, ?> map) {
        List<String> keys = map.keySet().stream()
                .filter(k -> isValidKey(k) || k.equalsIgnoreCase("customurl"))
                .sorted()
                .collect(Collectors.toList());

        if (keys.remove("customurl")) {
            keys.add("customurl");
        }

        return keys;
    }

    /**
     * Checks if a string is a valid key (letters, digits, underscores, or dashes).
     *
     * @param key The key string to validate.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidKey(String key) {
        return key.matches("[0-9a-zA-Z_-]+");
    }
}
