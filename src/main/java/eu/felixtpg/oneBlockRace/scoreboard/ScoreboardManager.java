package eu.felixtpg.oneBlockRace.scoreboard;

import eu.felixtpg.oneBlockRace.Main;
import eu.felixtpg.oneBlockRace.environment.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import fr.mrmicky.fastboard.FastBoard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ScoreboardManager {

    private static final HashMap<UUID, FastBoard> scoreboards = new HashMap<>();

    public static void updateScoreboard(Player player) {
        if (!scoreboards.containsKey(player.getUniqueId())) {
            scoreboards.put(player.getUniqueId(), new FastBoard(player));
        }

        FastBoard board = scoreboards.get(player.getUniqueId());
        board.updateTitle("§7    §9§lOneBlockRace§7    ");

        List<String> lines = new ArrayList<>();

        lines.add(" ");
        lines.add("§8» §9Item: §f" + EventManager.nextDrop.getOrDefault(player.getUniqueId(), 30) + " Sekunden");
        lines.add(" ");
        lines.add("§8» §9Bestenliste:");
        lines.add(" §x§F§F§D§7§0§01. " + Main.getPlotManager().getLeaderboardPlace(player, 1));
        lines.add(" §x§B§F§8§9§7§02. " + Main.getPlotManager().getLeaderboardPlace(player, 2));
        lines.add(" §x§C§0§C§0§C§03. " + Main.getPlotManager().getLeaderboardPlace(player, 3));

        int self = Main.getPlotManager().getLeaderboardPlaceRaw(player.getUniqueId());
        if (self > 3) {
            lines.add("  §8...");
            lines.add(" " + Main.getPlotManager().getLeaderboardPlace(player, -1));
        }

        lines.add(" ");
        lines.add("§7§oby FelixTPG & FrinshHD");

        board.updateLines(lines);
    }

    public static void removeScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
    }

}
