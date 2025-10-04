package eu.felixtpg.oneblockrace.listeners;

import eu.felixtpg.oneblockrace.Main;
import eu.felixtpg.oneblockrace.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    public ConnectionListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        event.setJoinMessage("§8[§a+§8] §7" + player.getName());

        Main.getPlotManager().checkPlot(player);
        ScoreboardManager.updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        event.setQuitMessage("§8[§c-§8] §7" + player.getName());

        ScoreboardManager.removeScoreboard(player);
    }

}
