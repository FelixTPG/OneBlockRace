package eu.felixtpg.oneBlockRace.environment;

import eu.felixtpg.oneBlockRace.Main;
import eu.felixtpg.oneBlockRace.PluginUtils;
import eu.felixtpg.oneBlockRace.scoreboard.ScoreboardManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.security.SecureRandom;
import java.util.*;

public class EventManager {

    /**
     * The interval in seconds between each item drop
     */
    public static final int DROP_INTERVAL = 20;
    public static final Map<UUID, Integer> nextDrop = new HashMap<>();

    public static final List<Material> possibleItems = new ArrayList<>();
    public static final List<Material> blacklistedItems = Arrays.asList(Material.BARRIER, Material.BEDROCK, Material.COMMAND_BLOCK, Material.DEBUG_STICK, Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.SCULK_SHRIEKER, Material.ENDER_DRAGON_SPAWN_EGG,
            Material.WARDEN_SPAWN_EGG, Material.WITHER_SPAWN_EGG, Material.PHANTOM_SPAWN_EGG);

    private static SecureRandom secureRandom = new SecureRandom();

    public EventManager() {
        possibleItems.addAll(Arrays.stream(Material.values()).filter(Material::isItem).toList());
        possibleItems.removeAll(blacklistedItems);

        startUpdater();
    }

    private void startUpdater() {
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!nextDrop.containsKey(player.getUniqueId()))
                    nextDrop.put(player.getUniqueId(), 10);

                if (!Main.running) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cDas Event ist pausiert!"));
                    return;
                }

                nextDrop.put(player.getUniqueId(), nextDrop.get(player.getUniqueId()) - 1);

                if (nextDrop.get(player.getUniqueId()) <= 0) {
                    dropItem(player);
                    nextDrop.put(player.getUniqueId(), DROP_INTERVAL);
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§7Nächstes Item in §9" + nextDrop.get(player.getUniqueId()) + " §7Sekunden"));
                ScoreboardManager.updateScoreboard(player);
            });
        }, 20L, 20L);

        // updates the leaderboard every 5 seconds
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> Main.getPlotManager().updateLeaderboard(), 0L, 20L * 5);
    }

    public static void dropItem(Player player) {
        Material randomMaterial = possibleItems.get(secureRandom.nextInt(possibleItems.size()));
        ItemStack item = new ItemStack(randomMaterial);

        // check if player has enough space in inventory
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            player.sendMessage(Main.PREFIX + "§7Du hast §91x " + PluginUtils.getReadableName(randomMaterial) + " §7erhalten!");
        } else {
            Bukkit.getWorlds().get(0).dropItem(Main.getPlotManager().getPlotSpawn(player.getUniqueId()).clone().add(0, 1.5, 0), item);
        }
    }

}
