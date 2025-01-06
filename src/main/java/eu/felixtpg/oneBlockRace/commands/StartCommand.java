package eu.felixtpg.oneBlockRace.commands;

import eu.felixtpg.oneBlockRace.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("oneblockrace.start")) return false;

        if (Main.running) {
            Main.running = false;
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage(Main.PREFIX + "§cDas Event wurde angehalten!");
            Bukkit.broadcastMessage(" ");
            return false;
        }

        Main.running = true;

        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(Main.PREFIX + "§aDas Event wurde gestartet!");
        Bukkit.broadcastMessage(" ");
        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1f));

        return false;
    }

}
