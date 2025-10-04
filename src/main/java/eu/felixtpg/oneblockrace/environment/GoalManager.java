package eu.felixtpg.oneblockrace.environment;

import eu.felixtpg.oneblockrace.Main;
import eu.felixtpg.oneblockrace.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GoalManager {

    private static BukkitTask goalTask;

    public void startGoalTime(int minutes) {
        AtomicInteger secondsLeft = new AtomicInteger(minutes * 60);

        goalTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (!Main.running) return;

            int seconds = secondsLeft.decrementAndGet();

            switch (seconds) {
                case 3600, 1800, 600, 300:
                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Das Event endet in §9" + (seconds / 60) + " §7Minuten!");
                    Bukkit.broadcastMessage(" ");
                    break;
                case 60, 30, 15, 5, 4, 3, 2, 1:
                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Das Event endet in §9" + seconds + " §7Sekunden!");
                    Bukkit.broadcastMessage(" ");
                    break;
                case 0:
                    Main.running = false;

                    Main.getPlotManager().updateLeaderboard();
                    Bukkit.getOnlinePlayers().forEach(ScoreboardManager::updateScoreboard);

                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Das Event hat geendet!");

                    int most = Main.getPlotManager().getLeaderboard().values().stream().findFirst().orElse(-1);
                    if (most != -1) {
                        List<String> players = Main.getPlotManager().getLeaderboard().entrySet().stream()
                                .filter(entry -> entry.getValue() == most)
                                .map(entry -> Bukkit.getOfflinePlayer(entry.getKey()).getName()).collect(Collectors.toList());

                        if (players.size() == 1) {
                            Bukkit.broadcastMessage(Main.PREFIX + "§7Gewinner ist §9" + String.join(", ", players) + "§7!");
                        } else {
                            Bukkit.broadcastMessage(Main.PREFIX + "§7Gewinner sind §9" + String.join("§7, §9", players) + "§7!");
                        }
                    }

                    Bukkit.broadcastMessage(" ");

                    goalTask.cancel();
                    break;
            }
        }, 20L, 20L);
    }

    public void startGoalBlock(int amount) {
        goalTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (!Main.running) return;

            int most = Main.getPlotManager().getLeaderboard().values().stream().findFirst().orElse(-1);

            if (most >= amount) {
                Main.running = false;

                Main.getPlotManager().updateLeaderboard();
                Bukkit.getOnlinePlayers().forEach(ScoreboardManager::updateScoreboard);

                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage(Main.PREFIX + "§7Das Event hat geendet!");

                List<String> players = Main.getPlotManager().getLeaderboard().entrySet().stream()
                        .filter(entry -> entry.getValue() == most)
                        .map(entry -> Bukkit.getOfflinePlayer(entry.getKey()).getName()).collect(Collectors.toList());

                if (players.size() == 1) {
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Gewinner ist §9" + String.join(", ", players) + "§7!");
                } else {
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Gewinner sind §9" + String.join("§7, §9", players) + "§7!");
                }

                Bukkit.broadcastMessage(" ");

                goalTask.cancel();
            }
        }, 20L, 20L);
    }

    public void clearGoal() {
        if (goalTask != null)
            goalTask.cancel();
        goalTask = null;
    }

}
