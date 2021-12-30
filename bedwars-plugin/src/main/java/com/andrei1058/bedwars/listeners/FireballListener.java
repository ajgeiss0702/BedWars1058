package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.getAPI;

public class FireballListener implements Listener {

    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;
    private final double fireballHorizontal;
    private final double fireballVertical;

    private final double damageSelf;
    private final double damageEnemy;
    private final double damageTeammates;

    public FireballListener() {
        this.fireballExplosionSize = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE);
        this.fireballMakeFire = config.getYml().getBoolean(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE);
        this.fireballHorizontal = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL) * -1;
        this.fireballVertical = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL);

        this.damageSelf = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF);
        this.damageEnemy = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY);
        this.damageTeammates = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES);
    }

    @EventHandler
    public void fireballHit(ProjectileHitEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        Location location = e.getEntity().getLocation();

        ProjectileSource projectileSource = e.getEntity().getShooter();
        if(!(projectileSource instanceof Player)) return;
        Player source = (Player) projectileSource;

        IArena arena = Arena.getArenaByPlayer(source);

        Vector vector = location.toVector();

        World world = location.getWorld();

        assert world != null;
        Collection<Entity> nearbyEntities = world
                .getNearbyEntities(location, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);
        for(Entity entity : nearbyEntities) {
            if(!(entity instanceof Player)) continue;
            Player player = (Player) entity;
            if(!getAPI().getArenaUtil().isPlaying(player)) continue;


            Vector playerVector = player.getLocation().toVector();
            Vector normalizedVector = vector.subtract(playerVector).normalize();
            Vector horizontalVector = normalizedVector.multiply(fireballHorizontal);
            double y = normalizedVector.getY();
            if(y < 0 ) y += 1.5;
            if(y <= 0.5) {
                y = fireballVertical*1.5; // kb for not jumping
            } else {
                y = y*fireballVertical*1.5; // kb for jumping
            }
            player.setVelocity(horizontalVector.setY(y));

            LastHit lh = LastHit.getLastHit(player);
            if (lh != null) {
                lh.setDamager(source);
                lh.setTime(System.currentTimeMillis());
            } else {
                new LastHit(player, source, System.currentTimeMillis());
            }

            if(player.equals(source)) {
                if(damageSelf > 0) {
                    player.damage(damageSelf); // damage shooter
                }
            } else if(arena.getTeam(player).equals(arena.getTeam(source))) {
                if(damageTeammates > 0) {
                    player.damage(damageTeammates); // damage teammates
                }
            } else {
                if(damageEnemy > 0) {
                    player.damage(damageEnemy); // damage enemies
                }
            }
        }
        for(Block b : getNearbyBlocks(arena, location, ((int)Math.round(fireballExplosionSize)))) {
            b.setType(Material.AIR);
        }
    }

    private List<Block> getNearbyBlocks(IArena arena, Location loc, int radius) {
        List<Block> blocks = new ArrayList<>();
        for(int x = loc.getBlockX() - radius; x < loc.getBlockX() + radius; x++ ) {
            for(int y = loc.getBlockY() - radius; y < loc.getBlockY() + radius; y++ ) {
                for(int z = loc.getBlockZ() - radius; z < loc.getBlockZ() + radius; z++ ) {
                    World world = loc.getWorld();
                    if(world == null) continue;
                    Block block = world.getBlockAt(x, y, z);
                    if(block.getType().name().contains("AIR")) continue;
                    if(block.getType().name().contains("_GLASS")) continue;
                    if(!arena.isBlockPlaced(block)) continue;
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    @EventHandler
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Fireball)) return;
        if(!(e.getEntity() instanceof Player)) return;

        if(Arena.getArenaByPlayer((Player) e.getEntity()) == null) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void fireballExplode(EntityExplodeEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        if(Arena.getArenaByIdentifier(e.getLocation().getWorld().getName()) == null) return;
        e.blockList().clear();
    }
}
