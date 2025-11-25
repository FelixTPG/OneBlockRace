package eu.felixtpg.oneBlockRace.environment;

import eu.felixtpg.oneBlockRace.Main;
import eu.felixtpg.oneBlockRace.PluginUtils;
import eu.felixtpg.oneBlockRace.scoreboard.ScoreboardManager;
import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

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

    // Countdown announcement times in seconds
    private static final int COUNTDOWN_5_MINUTES = 300;
    private static final int COUNTDOWN_1_MINUTE = 60;
    private static final int COUNTDOWN_30_SECONDS = 30;
    private static final int COUNTDOWN_10_SECONDS = 10;
    private static final int COUNTDOWN_FINAL = 5;

    private static SecureRandom secureRandom = new SecureRandom();

    // Goal configuration
    @Getter
    private boolean goalConfigured = false;
    @Getter
    private boolean timeGoal = false;
    @Getter
    private int goalValue = 0;

    // Event monitoring task
    private BukkitTask goalMonitorTask;
    private int remainingSeconds = 0;

    public EventManager() {
        possibleItems.addAll(Arrays.stream(Material.values()).filter(Material::isItem).toList());
        possibleItems.removeAll(blacklistedItems);

        startUpdater();
    }

    /**
     * Configure a time-based goal
     * @param minutes Duration of the event in minutes
     */
    public void configureTimeGoal(int minutes) {
        this.goalConfigured = true;
        this.timeGoal = true;
        this.goalValue = minutes;
        this.remainingSeconds = minutes * 60;
    }

    /**
     * Configure a block-based goal
     * @param blocks Number of blocks to reach
     */
    public void configureBlockGoal(int blocks) {
        this.goalConfigured = true;
        this.timeGoal = false;
        this.goalValue = blocks;
    }

    /**
     * Check if a goal has been configured
     * @return true if a goal is configured
     */
    public boolean hasGoal() {
        return goalConfigured;
    }

    /**
     * Clear the configured goal
     */
    public void clearGoal() {
        this.goalConfigured = false;
        this.timeGoal = false;
        this.goalValue = 0;
        this.remainingSeconds = 0;
        stopGoalMonitor();
    }

    /**
     * Start the event
     */
    public void startEvent() {
        if (!goalConfigured) {
            return;
        }

        Main.running = true;

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(Main.PREFIX + "§aDas Event wurde gestartet!");
        if (timeGoal) {
            Bukkit.broadcastMessage(Main.PREFIX + "§7Ziel: §9" + goalValue + " Minuten §7Zeitlimit");
            remainingSeconds = goalValue * 60;
        } else {
            Bukkit.broadcastMessage(Main.PREFIX + "§7Ziel: §9" + goalValue + " Blöcke §7erreichen");
        }
        Bukkit.broadcastMessage(" ");

        Bukkit.getOnlinePlayers().forEach(player -> 
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1f));

        startGoalMonitor();
    }

    /**
     * Stop the event
     */
    public void stopEvent() {
        Main.running = false;
        stopGoalMonitor();

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(Main.PREFIX + "§cDas Event wurde angehalten!");
        Bukkit.broadcastMessage(" ");
    }

    /**
     * Start monitoring for goal completion
     */
    private void startGoalMonitor() {
        stopGoalMonitor();

        goalMonitorTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (!Main.running) {
                return;
            }

            if (timeGoal) {
                monitorTimeGoal();
            } else {
                monitorBlockGoal();
            }
        }, 20L, 20L);
    }

    /**
     * Stop the goal monitoring task
     */
    private void stopGoalMonitor() {
        if (goalMonitorTask != null && !goalMonitorTask.isCancelled()) {
            goalMonitorTask.cancel();
            goalMonitorTask = null;
        }
    }

    /**
     * Monitor time-based goal
     */
    private void monitorTimeGoal() {
        remainingSeconds--;

        // Countdown announcements
        if (remainingSeconds == COUNTDOWN_5_MINUTES) {
            broadcastCountdown("§e5 Minuten");
        } else if (remainingSeconds == COUNTDOWN_1_MINUTE) {
            broadcastCountdown("§c1 Minute");
        } else if (remainingSeconds == COUNTDOWN_30_SECONDS) {
            broadcastCountdown("§c30 Sekunden");
        } else if (remainingSeconds == COUNTDOWN_10_SECONDS) {
            broadcastCountdown("§c10 Sekunden");
        } else if (remainingSeconds <= COUNTDOWN_FINAL && remainingSeconds > 0) {
            broadcastCountdown("§c" + remainingSeconds + " Sekunde" + (remainingSeconds > 1 ? "n" : ""));
        }

        if (remainingSeconds <= 0) {
            endEvent();
        }
    }

    /**
     * Monitor block-based goal
     */
    private void monitorBlockGoal() {
        LinkedHashMap<UUID, Integer> leaderboard = PlotManager.getLeaderboard();
        
        for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
            if (entry.getValue() >= goalValue) {
                endEvent();
                return;
            }
        }
    }

    /**
     * Broadcast countdown message
     */
    private void broadcastCountdown(String timeText) {
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(Main.PREFIX + "§7Noch §9" + timeText + " §7verbleibend!");
        Bukkit.broadcastMessage(" ");
        Bukkit.getOnlinePlayers().forEach(player ->
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f));
    }

    /**
     * End the event and announce winners
     */
    private void endEvent() {
        Main.running = false;
        stopGoalMonitor();

        // Update leaderboard one final time
        Main.getPlotManager().updateLeaderboard();

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(Main.PREFIX + "§a§lDas Event ist beendet!");
        Bukkit.broadcastMessage(" ");

        // Announce top 3 winners
        LinkedHashMap<UUID, Integer> leaderboard = PlotManager.getLeaderboard();
        int position = 1;
        for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
            if (position > 3) break;
            
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            String prefix = switch (position) {
                case 1 -> "§6§l1.";
                case 2 -> "§7§l2.";
                case 3 -> "§c§l3.";
                default -> "§8" + position + ".";
            };
            
            Bukkit.broadcastMessage(Main.PREFIX + prefix + " §f" + playerName + " §8- §9" + entry.getValue() + " Blöcke");
            position++;
        }

        Bukkit.broadcastMessage(" ");

        // Play victory sound
        Bukkit.getOnlinePlayers().forEach(player ->
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f));

        // Update scoreboards
        Bukkit.getOnlinePlayers().forEach(ScoreboardManager::updateScoreboard);
    }

    /**
     * Get remaining time in seconds (for time-based goals)
     * @return remaining seconds, or 0 if not a time goal
     */
    public int getRemainingSeconds() {
        return timeGoal ? remainingSeconds : 0;
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

                // Show time remaining if time goal, otherwise show next item drop
                if (goalConfigured && timeGoal && remainingSeconds > 0) {
                    int minutes = remainingSeconds / 60;
                    int seconds = remainingSeconds % 60;
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                        new TextComponent("§7Zeit: §9" + String.format("%02d:%02d", minutes, seconds) + 
                            " §8| §7Item in §9" + nextDrop.get(player.getUniqueId()) + "s"));
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                        new TextComponent("§7Nächstes Item in §9" + nextDrop.get(player.getUniqueId()) + " §7Sekunden"));
                }
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
