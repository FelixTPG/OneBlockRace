package eu.felixtpg.oneBlockRace;

import eu.felixtpg.oneBlockRace.commands.StartCommand;
import eu.felixtpg.oneBlockRace.environment.EventManager;
import eu.felixtpg.oneBlockRace.environment.PlotManager;
import eu.felixtpg.oneBlockRace.listeners.BarrierListener;
import eu.felixtpg.oneBlockRace.listeners.ConnectionListener;
import eu.felixtpg.oneBlockRace.listeners.EnvironmentListener;
import eu.felixtpg.oneBlockRace.listeners.SpawnProtectionListener;
import eu.felixtpg.oneBlockRace.worldCreation.VoidWorldGenerator;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin {

    public static boolean running = false;

    public static final String PREFIX = "§8» §9OneBlock §8| §7";

    @Getter private static Main instance;
    @Getter private static PlotManager plotManager;

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidWorldGenerator();
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Check if the timer has already started
        if (getConfig().get("hasStarted") == null) {
            getConfig().set("hasStarted", running);
            saveConfig();
        } else {
            running = getConfig().getBoolean("hasStarted");
        }

        plotManager = new PlotManager();
        new EnvironmentListener();
        new ConnectionListener();
        new BarrierListener();
        new SpawnProtectionListener();

        new EventManager();

        getCommand("start").setExecutor(new StartCommand());

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            setUpWorlds();
            startFastDaylightCycle();
        });
    }

    @Override
    public void onDisable() {
        getConfig().set("hasStarted", running);
        saveConfig();
    }

    public void setUpWorlds() {
        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.FIRE_DAMAGE, false);
            world.setGameRule(GameRule.DROWNING_DAMAGE, false);
            world.setGameRule(GameRule.FREEZE_DAMAGE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        });
    }

    private void startFastDaylightCycle() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    long currentTime = world.getTime();
                    long newTime = currentTime + 2;
                    world.setTime(newTime);
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

}
