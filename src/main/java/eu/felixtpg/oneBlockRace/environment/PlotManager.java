package eu.felixtpg.oneBlockRace.environment;

import eu.felixtpg.oneBlockRace.Main;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlotManager {

    private final int PLOT_WIDTH = 7;

    private static File file;
    private static FileConfiguration config;

    @Getter
    public static LinkedHashMap<UUID, Integer> leaderboard = new LinkedHashMap<>();

    public PlotManager() {
        file = new File("./plugins/OneBlockRace", "data.yml");
        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Checks if the player has already a plot and if not, generates one for him
     *
     * @param player The player to check the plot for
     */
    public void checkPlot(Player player) {
        final UUID playerId = player.getUniqueId();

        // Check if player has a plot
        if (config.getConfigurationSection("plots." + playerId) == null) {
            generatePlot(player);
            Bukkit.getConsoleSender().sendMessage("Generating plot for " + player.getName() + "...");
        } else {
            Bukkit.getConsoleSender().sendMessage(player.getName() + " already has a plot - skip generating!");
        }
    }

    /**
     * Method is designed to assign a location to a player where he will be spawning and playing his "OneBlockRace"
     *
     * @param player The player to assign the location to
     */
    public void generatePlot(Player player) {
        // determine the next available plot location (first plot 0, 100, 0; second plot 0, 100, PLOT_WIDTH * PLOT_COUNT; etc...)
        int plotCount = 0;
        if (config.getConfigurationSection("plots") != null) {
            plotCount = config.getConfigurationSection("plots").getKeys(false).size();
        }

        int x = 0;
        int y = 100;
        int z = (PLOT_WIDTH + 2) * plotCount;

        Location spawnLocation = new Location(player.getWorld(), x, y, z);

        // Set the spawn location to a bedrock block
        player.getWorld().getBlockAt(x, y, z).setType(Material.BEDROCK);
        player.setRespawnLocation(new Location(player.getWorld(), x + 0.5, y + 1, z + 0.5), true);


        int range = (int) Math.ceil((double) PLOT_WIDTH / 2);
        World world = player.getWorld();
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();

        final List<Location> locationsToUpdate = new ArrayList<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int yBarrier = minHeight; yBarrier <= maxHeight; yBarrier++) {
                    for (int zBarrier = -range; zBarrier <= range; zBarrier++) {
                        Location blockLocation = spawnLocation.clone().add(x - 1, yBarrier, zBarrier);
                        locationsToUpdate.add(blockLocation);
                    }
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Location loc : locationsToUpdate) {
                            loc.getBlock().setType(Material.BARRIER);
                        }
                    }
                }.runTask(Main.getInstance());
            }
        }.runTaskAsynchronously(Main.getInstance());

        teleportToPlot(player);

        // Save the plot information to the config
        config.set("plots." + player.getUniqueId() + ".x", x);
        config.set("plots." + player.getUniqueId() + ".y", y);
        config.set("plots." + player.getUniqueId() + ".z", z);

        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    public void teleportToPlot(Player player) {
        Location plotSpawn = getPlotSpawn(player.getUniqueId());

        player.teleport(new Location(player.getWorld(), plotSpawn.getX(), plotSpawn.getY() + 1, plotSpawn.getZ(), -90, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    public Location getPlotSpawn(UUID uuid) {
        int plotX = config.getInt("plots." + uuid + ".x");
        int plotY = config.getInt("plots." + uuid + ".y");
        int plotZ = config.getInt("plots." + uuid + ".z");

        return new Location(Bukkit.getWorlds().getFirst(), plotX + 0.5, plotY, plotZ + 0.5);
    }

    public boolean canPlace(Player player, Location location) {
        Location plotSpawn = getPlotSpawn(player.getUniqueId());
        int plotX = plotSpawn.getBlockX();
        int plotZ = plotSpawn.getBlockZ();

        int locX = location.getBlockX();
        int locY = location.getBlockY();
        int locZ = location.getBlockZ();

        // if player tries to place on top of the plot spawn, deny
        if (locX == plotX && locZ == plotZ) return false;

        // Check if the location is within the allowed building area
        return locX >= plotX &&
                locY >= 100 && locY <= 105 &&
                locZ == plotZ;
    }

    public boolean canBreak(Player player, Location location) {
        Location plotSpawn = getPlotSpawn(player.getUniqueId());
        int plotX = plotSpawn.getBlockX();
        int plotZ = plotSpawn.getBlockZ();

        int locX = location.getBlockX();
        int locY = location.getBlockY();
        int locZ = location.getBlockZ();

        // Check if the location is within the player's plot
        return locX >= plotX &&
                locY >= 100 &&
                Math.abs(locZ - plotZ) <= PLOT_WIDTH / 2;
    }

    public void updateLeaderboard() {
        LinkedHashMap<UUID, Integer> tempLeaderboard = new LinkedHashMap<>();

        // get list of all plots
        List<String> plotList = config.getConfigurationSection("plots").getKeys(false).stream().toList();
        plotList.forEach(plot -> {
            UUID playerId = UUID.fromString(plot);
            Location plotSpawn = Main.getPlotManager().getPlotSpawn(playerId);

            // count blocks to x coordinate of the plot until block is air
            int blockCount = 0;
            int emptyBlocks = 0;

            while (emptyBlocks < 10) {
                if (plotSpawn.clone().add(blockCount, 0, 0).getBlock().getType() == Material.AIR) {
                    emptyBlocks++;
                } else {
                    blockCount++;
                }
            }

            blockCount--; // subtract the start block
            if (blockCount <= 0) return;
            tempLeaderboard.put(playerId, blockCount);
        });

        // sort the leaderboard by block count
        leaderboard = tempLeaderboard.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    /**
     * Get the player at a specific place in the leaderboard, if place applied is -1, search for the player!
     *
     * @param player The player to get the leaderboard place for
     * @param place  The place in the leaderboard
     * @return The
     */
    public String getLeaderboardPlace(Player player, int place) {
        LinkedHashMap<UUID, Integer> leaderboard = getLeaderboard();

        int i = 0;
        for (UUID playerId : leaderboard.keySet()) {
            i++;

            if (place == -1) {
                if (player.getUniqueId().equals(playerId)) {
                    // §9x. §f§lPlayerName §8- §6BlockCount
                    return "§8" + i + ". §9" + Bukkit.getOfflinePlayer(playerId).getName() + " §8- §9" + leaderboard.get(playerId);
                }
            } else {
                if (i == place) {
                    return (player.getUniqueId().equals(playerId) ? "§l" : "") + Bukkit.getOfflinePlayer(playerId).getName() + "§8 - §9" + leaderboard.get(playerId);
                }
            }
        }

        return "§cN/A";
    }

    public int getLeaderboardPlaceRaw(UUID playerId) {
        LinkedHashMap<UUID, Integer> leaderboard = getLeaderboard();

        int i = 1;
        for (UUID id : leaderboard.keySet()) {
            if (playerId.equals(id)) {
                return i;
            }
            i++;
        }

        return -1;
    }

}