package eu.felixtpg.oneBlockRace.listeners;

import eu.felixtpg.oneBlockRace.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpawnProtectionListener implements Listener {

    public SpawnProtectionListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isWithinSpawn(event.getBlock(), event.getPlayer())) {
            event.setCancelled(false); // Allow breaking blocks in spawn
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isWithinSpawn(event.getBlock(), event.getPlayer())) {
            event.setCancelled(false); // Allow placing blocks in spawn
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && isWithinSpawn(event.getClickedBlock(), event.getPlayer())) {
            event.setCancelled(false); // Allow interactions in spawn
        }
    }

    private boolean isWithinSpawn(Block block, Player player) {
        return block.getWorld().getSpawnLocation().distance(block.getLocation()) <= Bukkit.getServer().getSpawnRadius() &&
                player.getGameMode() != GameMode.CREATIVE; // Adjust checks as needed
    }

}
