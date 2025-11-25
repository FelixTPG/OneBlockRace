package eu.felixtpg.oneBlockRace.commands;

import eu.felixtpg.oneBlockRace.Main;
import eu.felixtpg.oneBlockRace.environment.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventCommand implements CommandExecutor {

    private static final int SETUP_TIMEOUT_SECONDS = 60;
    private static final Map<UUID, SetupSession> activeSessions = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("oneblockrace.event")) {
            sender.sendMessage(Main.PREFIX + "§cDu hast keine Berechtigung für diesen Befehl!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setup":
                handleSetup(sender);
                break;
            case "start":
                handleStart(sender);
                break;
            case "stop":
                handleStop(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleSetup(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + "§cDieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return;
        }

        UUID playerId = player.getUniqueId();

        if (activeSessions.containsKey(playerId)) {
            player.sendMessage(Main.PREFIX + "§cDu hast bereits eine laufende Setup-Sitzung! Schreibe 'cancel' um abzubrechen.");
            return;
        }

        if (Main.running) {
            player.sendMessage(Main.PREFIX + "§cDas Event läuft bereits! Stoppe es zuerst mit §e/event stop§c.");
            return;
        }

        SetupSession session = new SetupSession(player);
        activeSessions.put(playerId, session);
        session.start();
    }

    private void handleStart(CommandSender sender) {
        EventManager eventManager = Main.getEventManager();

        if (!eventManager.hasGoal()) {
            sender.sendMessage(Main.PREFIX + "§cEs wurde noch kein Ziel konfiguriert! Führe zuerst §e/event setup §caus.");
            return;
        }

        if (Main.running) {
            sender.sendMessage(Main.PREFIX + "§cDas Event läuft bereits!");
            return;
        }

        eventManager.startEvent();
    }

    private void handleStop(CommandSender sender) {
        EventManager eventManager = Main.getEventManager();

        if (!eventManager.hasGoal()) {
            sender.sendMessage(Main.PREFIX + "§cEs wurde noch kein Ziel konfiguriert! Führe zuerst §e/event setup §caus.");
            return;
        }

        if (!Main.running) {
            sender.sendMessage(Main.PREFIX + "§cDas Event läuft nicht!");
            return;
        }

        eventManager.stopEvent();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage(Main.PREFIX + "§9Event Hilfe:");
        sender.sendMessage("§8» §e/event setup §8- §7Event konfigurieren (interaktiv)");
        sender.sendMessage("§8» §e/event start §8- §7Event starten");
        sender.sendMessage("§8» §e/event stop §8- §7Event stoppen");
        sender.sendMessage(" ");
    }

    public static void removeSession(UUID playerId) {
        SetupSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.cleanup();
        }
    }

    private enum SetupState {
        ASKING_GOAL_TYPE,
        ASKING_BLOCKS_AMOUNT,
        ASKING_TIME_DURATION,
        COMPLETED
    }

    private static class SetupSession implements Listener {
        private final Player player;
        private final UUID playerId;
        private SetupState state;
        private BukkitTask timeoutTask;
        private boolean isTimeGoal;
        private int goalValue;

        public SetupSession(Player player) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.state = SetupState.ASKING_GOAL_TYPE;
        }

        public void start() {
            Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
            askGoalType();
        }

        private void askGoalType() {
            state = SetupState.ASKING_GOAL_TYPE;
            player.sendMessage(" ");
            player.sendMessage(Main.PREFIX + "§9Event Setup gestartet!");
            player.sendMessage(Main.PREFIX + "§7Schreibe §ccancel §7um abzubrechen.");
            player.sendMessage(" ");
            player.sendMessage(Main.PREFIX + "§eWelches Ziel soll das Event haben? (blocks/time)");
            scheduleTimeout();
        }

        private void askBlocksAmount() {
            state = SetupState.ASKING_BLOCKS_AMOUNT;
            player.sendMessage(Main.PREFIX + "§eWie viele Blöcke müssen erreicht werden?");
            scheduleTimeout();
        }

        private void askTimeDuration() {
            state = SetupState.ASKING_TIME_DURATION;
            player.sendMessage(Main.PREFIX + "§eWie viele Minuten soll das Event dauern?");
            scheduleTimeout();
        }

        @EventHandler
        public void onChat(AsyncPlayerChatEvent event) {
            if (!event.getPlayer().getUniqueId().equals(playerId)) {
                return;
            }

            event.setCancelled(true);
            String message = event.getMessage().trim();

            // Cancel check
            if (message.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(Main.getInstance(), this::cancel);
                return;
            }

            // Handle based on current state
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> handleInput(message));
        }

        private void handleInput(String message) {
            cancelTimeout();

            switch (state) {
                case ASKING_GOAL_TYPE:
                    handleGoalTypeInput(message);
                    break;
                case ASKING_BLOCKS_AMOUNT:
                    handleBlocksAmountInput(message);
                    break;
                case ASKING_TIME_DURATION:
                    handleTimeDurationInput(message);
                    break;
                default:
                    break;
            }
        }

        private void handleGoalTypeInput(String message) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.equals("blocks") || lowerMessage.equals("block")) {
                isTimeGoal = false;
                player.sendMessage(Main.PREFIX + "§7Zieltyp: §9Blöcke");
                askBlocksAmount();
            } else if (lowerMessage.equals("time") || lowerMessage.equals("zeit")) {
                isTimeGoal = true;
                player.sendMessage(Main.PREFIX + "§7Zieltyp: §9Zeit");
                askTimeDuration();
            } else {
                player.sendMessage(Main.PREFIX + "§cUngültige Eingabe! Bitte antworte mit §eblocks §coder §etime§c.");
                scheduleTimeout();
            }
        }

        private void handleBlocksAmountInput(String message) {
            try {
                int blocks = Integer.parseInt(message);
                if (blocks <= 0) {
                    player.sendMessage(Main.PREFIX + "§cBitte gib eine positive Zahl ein!");
                    scheduleTimeout();
                    return;
                }
                goalValue = blocks;
                player.sendMessage(Main.PREFIX + "§7Blockziel: §9" + blocks + " Blöcke");
                completeSetup();
            } catch (NumberFormatException e) {
                player.sendMessage(Main.PREFIX + "§cBitte gib eine gültige Zahl ein!");
                scheduleTimeout();
            }
        }

        private void handleTimeDurationInput(String message) {
            try {
                int minutes = Integer.parseInt(message);
                if (minutes <= 0) {
                    player.sendMessage(Main.PREFIX + "§cBitte gib eine positive Zahl ein!");
                    scheduleTimeout();
                    return;
                }
                goalValue = minutes;
                player.sendMessage(Main.PREFIX + "§7Zeitdauer: §9" + minutes + " Minuten");
                completeSetup();
            } catch (NumberFormatException e) {
                player.sendMessage(Main.PREFIX + "§cBitte gib eine gültige Zahl ein!");
                scheduleTimeout();
            }
        }

        private void completeSetup() {
            state = SetupState.COMPLETED;

            EventManager eventManager = Main.getEventManager();

            if (isTimeGoal) {
                eventManager.configureTimeGoal(goalValue);
            } else {
                eventManager.configureBlockGoal(goalValue);
            }

            player.sendMessage(" ");
            player.sendMessage(Main.PREFIX + "§aEvent Setup abgeschlossen!");

            // Broadcast the configured goal
            Bukkit.broadcastMessage(" ");
            if (isTimeGoal) {
                Bukkit.broadcastMessage(Main.PREFIX + "§aEvent konfiguriert: §9" + goalValue + " Minuten §7Zeitlimit");
            } else {
                Bukkit.broadcastMessage(Main.PREFIX + "§aEvent konfiguriert: §9" + goalValue + " Blöcke §7als Ziel");
            }
            Bukkit.broadcastMessage(Main.PREFIX + "§7Starte das Event mit §e/event start");
            Bukkit.broadcastMessage(" ");

            cleanup();
            activeSessions.remove(playerId);
        }

        private void cancel() {
            player.sendMessage(" ");
            player.sendMessage(Main.PREFIX + "§cEvent Setup abgebrochen!");
            player.sendMessage(" ");
            cleanup();
            activeSessions.remove(playerId);
        }

        private void timeout() {
            player.sendMessage(" ");
            player.sendMessage(Main.PREFIX + "§cEvent Setup abgebrochen - Zeit abgelaufen!");
            player.sendMessage(" ");
            cleanup();
            activeSessions.remove(playerId);
        }

        private void scheduleTimeout() {
            cancelTimeout();
            timeoutTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::timeout, 20L * SETUP_TIMEOUT_SECONDS);
        }

        private void cancelTimeout() {
            if (timeoutTask != null && !timeoutTask.isCancelled()) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
        }

        public void cleanup() {
            cancelTimeout();
            HandlerList.unregisterAll(this);
        }
    }
}
