package eu.felixtpg.oneBlockRace.listeners;

import eu.felixtpg.oneBlockRace.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class EnvironmentListener implements Listener {

    public EnvironmentListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) return;

        if (!Main.running) {
            player.sendMessage(Main.PREFIX + "§cDu kannst nichts platzieren, während das Event pausiert ist!");
            event.setCancelled(true);
            return;
        }

        if (Main.getPlotManager().getPlotSpawn(player.getUniqueId()).getX() == event.getBlock().getLocation().getX() && Main.getPlotManager().getPlotSpawn(player.getUniqueId()).getZ() == event.getBlock().getLocation().getZ()) {
            player.sendMessage(Main.PREFIX + "§cDu darfst keine Blöcke auf deiner Wiederbelebungspunkt platzieren!");
            event.setCancelled(true);
            return;
        }

        if (event.getBlock().getLocation().getX() < 0) {
            player.sendMessage("§cDu darfst in diese Richtung keine Blöcke platzieren!");
            event.setCancelled(true);
            return;
        }

        if (!Main.getPlotManager().canPlace(player, event.getBlock().getLocation())) {
            player.sendMessage(Main.PREFIX + "§cDu darfst hier keine Blöcke platzieren!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) return;

        if (!Main.running) {
            player.sendMessage(Main.PREFIX + "§cDu kannst nichts abbauen, während das Event pausiert ist!");
            event.setCancelled(true);
            return;
        }

        if (!Main.getPlotManager().canBreak(player, event.getBlock().getLocation())) {
            player.sendMessage(Main.PREFIX + "§cDu darfst hier keine Blöcke abbauen!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;
        if (event.getClickedBlock() == null) return;

        if (!Main.getPlotManager().canBreak(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosion(ExplosionPrimeEvent event) {
        event.setCancelled(true);

    }
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;
        if (event.getPlayer().getLocation().getY() > 70 || Main.getPlotManager().canBreak(event.getPlayer(), event.getPlayer().getLocation())) return;

        Main.getPlotManager().teleportToPlot(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player)
            event.getEntity().sendMessage(Main.PREFIX + "§cDu darfst keine Projektilen verwenden!");
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!Main.running)
            event.setCancelled(true);

        if (event.getFoodLevel() <= 4)
            event.setCancelled(true);
    }

}
