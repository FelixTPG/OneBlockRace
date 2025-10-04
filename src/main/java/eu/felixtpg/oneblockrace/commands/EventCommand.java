package eu.felixtpg.oneblockrace.commands;

import eu.felixtpg.oneblockrace.Main;
import eu.felixtpg.oneblockrace.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EventCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("oneblockrace.event")) return false;

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("start")) {
                if (!Main.running) {
                    Main.getPlotManager().updateLeaderboard();
                    Bukkit.getOnlinePlayers().forEach(ScoreboardManager::updateScoreboard);

                    Main.running = true;

                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§aDas Event wurde gestartet!");
                    Bukkit.broadcastMessage(" ");
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1f));
                } else {
                    sender.sendMessage(Main.PREFIX + "§cDas Event läuft bereits!");
                }
            } else if (args[0].equalsIgnoreCase("pause")) {
                if (Main.running) {
                    Main.running = false;

                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§cDas Event wurde angehalten!");
                    Bukkit.broadcastMessage(" ");
                } else {
                    sender.sendMessage(Main.PREFIX + "§cDas Event hält bereits an!");
                }
            } else {
                sendHelp(sender);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("goal")) {
                if (args[1].equalsIgnoreCase("block")) {
                    int amount;
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Main.PREFIX + "§cDu musst die Anzahl als natürliche ganze Zahl angeben!");
                        return false;
                    }
                    if (amount <= 0) {
                        sender.sendMessage(Main.PREFIX + "§cDu musst die Anzahl als natürliche ganze Zahl angeben!");
                        return false;
                    }

                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Das Ziel des Events ist es als erster eine Anzahl von §9" + amount + " §7Blöcken zu besitzen!");
                    Bukkit.broadcastMessage(" ");

                    Main.getGoalManager().startGoalBlock(amount);
                } else if (args[1].equalsIgnoreCase("time")) {
                    int minutes;
                    try {
                        minutes = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Main.PREFIX + "§cDu musst die Minuten als natürliche ganze Zahl angeben!");
                        return false;
                    }
                    if (minutes <= 0) {
                        sender.sendMessage(Main.PREFIX + "§cDu musst die Minuten als natürliche ganze Zahl angeben!");
                        return false;
                    }

                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(Main.PREFIX + "§7Das Ziel des Events ist es die meisten Blöcke nach Ablauf der Zeit von §9" + minutes + " §7Minuten zu besitzen!");
                    Bukkit.broadcastMessage(" ");

                    Main.getGoalManager().startGoalTime(minutes);
                }
            } else {
                sendHelp(sender);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("goal") && args[1].equalsIgnoreCase("clear")) {
                Main.getGoalManager().clearGoal();

                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage(Main.PREFIX + "§cDas Ziel des Events wurde entfernt!");
                Bukkit.broadcastMessage(" ");
            } else {
                sendHelp(sender);
            }
        } else {
            sendHelp(sender);
        }

        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Main.PREFIX + "§7Nutze: /event start");
        sender.sendMessage(Main.PREFIX + "§7Nutze: /event pause");
        sender.sendMessage(Main.PREFIX + "§7Nutze: /event goal block <Anzahl>");
        sender.sendMessage(Main.PREFIX + "§7Nutze: /event goal time <Minuten>");
        sender.sendMessage(Main.PREFIX + "§7Nutze: /event goal clear");
    }

}
