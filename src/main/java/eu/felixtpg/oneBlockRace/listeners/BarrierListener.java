package eu.felixtpg.oneBlockRace.listeners;

import eu.felixtpg.oneBlockRace.Main;
import eu.felixtpg.oneBlockRace.environment.PlotManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BarrierListener implements Listener {

    private final int range = 3;
    private final Map<Player, List<Block>> playerBlocks = new HashMap<>();
    private BukkitTask runnable = null;

    public BarrierListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onBarrierInRange(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Location from = event.getFrom().getBlock().getLocation();
        if (event.getTo() == null) {
            return;
        }
        Location to = event.getTo().getBlock().getLocation();
        if (from.equals(to)) {
            return;
        }

        Location playerLocation = player.getLocation();

        PlotManager plotManager = Main.getPlotManager();
        Location plotSpawn = plotManager.getPlotSpawn(player.getUniqueId());

        if (playerLocation.getX() - plotSpawn.getX() > 2) {
            setPlayerBlocks(player, new ArrayList<>());
            return;
        }

        // if the player has already placed blocks, don't show the barrier
        if (Main.getPlotManager().getLeaderboard().getOrDefault(player.getUniqueId(), 0) != 0) return;

        Location barrierLocation = plotSpawn.clone().add(-1, 0, 0);
        List<Block> playerBlocks = new ArrayList<>();

        // Check a spherical range around the player
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    // Calculate the squared distance from the center to avoid costly sqrt calls
                    double distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared > range * range) {
                        continue; // Skip blocks outside the spherical range
                    }

                    Location blockLocation = barrierLocation.clone().add(x, y, z);

                    int behindX = (plotSpawn.getBlockX() - 1);

                    if (blockLocation.getBlockX() == behindX) {
                        Block block = blockLocation.getBlock();
                        if (blockLocation.getY() >= 100 - (double) range / 2) {
                            playerBlocks.add(block);
                            spawnParticle(player, block);
                        }
                    }
                }
            }
        }

        setPlayerBlocks(player, playerBlocks);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerBlocks.remove(event.getPlayer());
    }

    private void setPlayerBlocks(Player player, List<Block> blocks) {
        if (blocks.isEmpty()) {
            playerBlocks.remove(player);
            return;
        }

        playerBlocks.put(player, blocks);

        if (runnable == null || runnable.isCancelled()) startRunnable();
    }

    private void startRunnable() {
        runnable = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (playerBlocks.isEmpty()) {
                runnable.cancel();
                runnable = null;
            }

            playerBlocks.forEach((p, blocks) -> {
                blocks.forEach(b -> {
                    spawnParticle(p, b);
                });
            });

        }, 0L, 10L);
    }

    private void spawnParticle(Player player, Block block) {
        Location blockLocation = block.getLocation().clone();
        // Spawn a barrier particle at the center of the block
        player.spawnParticle(Particle.BLOCK_MARKER, blockLocation.add(0.5, 0.5, 0.5), 1, Bukkit.createBlockData(Material.BARRIER));
    }
}
