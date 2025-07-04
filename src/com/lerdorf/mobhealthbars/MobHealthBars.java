package com.lerdorf.mobhealthbars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.Hangable;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobHealthBars extends JavaPlugin {
    
	private final Map<ArmorStand, Integer> healthbarCounters = new ConcurrentHashMap<>();
    private final Map<LivingEntity, ArmorStand> healthBarEntities = new ConcurrentHashMap<>();
    private final Map<LivingEntity, Long> lastLookedAt = new ConcurrentHashMap<>();
    private final int RAYCAST_DISTANCE = 10;
    private final long HIDE_DELAY = 5000; // 5 seconds in milliseconds
    private Team noCollisionTeam;
    float yOffset = 0.1f;
    
    @Override
    public void onEnable() {
        getLogger().info("MobHealthBars plugin enabled!");
        // Create a scoreboard team for non-collision entities
        setupNoCollisionTeam();
        startHealthBarTask();
    }
    
    @Override
    public void onDisable() {
        // Clean up all health bar entitiess
        for (ArmorStand entity : healthBarEntities.values()) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        healthBarEntities.clear();
        lastLookedAt.clear();
        
        // Clean up scoreboard team
        if (noCollisionTeam != null) {
            noCollisionTeam.unregister();
        }
        
        getLogger().info("MobHealthBars plugin disabled!");
    }

	long c = 0;
	
    private void startHealthBarTask() {
    	Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[tag=FancyHealthBar]");
        new BukkitRunnable() {
            @Override
            public void run() {
            	c++;
            	if (healthBarEntities.size() > 0) {
            		ArrayList<LivingEntity> remove = new ArrayList<>();
            		for (LivingEntity le : healthBarEntities.keySet()) {
            			if (le == null || !le.isValid()) {
            				remove.add(le);
            			}
            			else if (healthBarEntities.get(le) != null && healthBarEntities.get(le).isValid()) {
            				//healthBarEntities.get(le).teleport(le.getLocation().add(0, le.getHeight() + 0.5, 0));
            				updateHealthBar(le);
            			}
            		}
            		for (LivingEntity le : remove) {
            			if (healthBarEntities.get(le) != null && healthBarEntities.get(le).isValid()) {
            				healthBarEntities.get(le).remove();
            			}
            			healthBarEntities.remove(le);
            		}
            	}
            	if (c % 5 == 0) {

                	if (healthbarCounters.size() > 0) {
                		ArrayList<ArmorStand> remove = new ArrayList<>();
                		for (ArmorStand text : healthbarCounters.keySet()) {
                			if (text == null || !text.isValid() || healthbarCounters.get(text) > HIDE_DELAY/10) {
                				remove.add(text);
                			}
                			else 
                				healthbarCounters.put(text, healthbarCounters.get(text)+25);
                		}
                		for (ArmorStand text : remove) {
                			if (text != null && text.isValid())
                				text.remove();
                			healthbarCounters.remove(text);
                		}
                	}
            		
	                Set<LivingEntity> currentlyViewed = new HashSet<>();
	                
	                // Check all online players
	                for (Player player : Bukkit.getOnlinePlayers()) {
	                    LivingEntity targetEntity = getTargetEntity(player);
	                    if (targetEntity != null && isValidTarget(targetEntity) && !targetEntity.equals(player)) {
	                        currentlyViewed.add(targetEntity);
	                        lastLookedAt.put(targetEntity, System.currentTimeMillis());
	                        updateHealthBar(targetEntity);
	                    }
	                }
	                
	                // Hide health bars for entities not being looked at
	                hideUnviewedHealthBars(currentlyViewed);
            	}
            }
        }.runTaskTimer(this, 0L, 1L); // Run every 1 tick
    }
    
    private void setupNoCollisionTeam() {
        // Create a scoreboard team that doesn't collide with anything
        if (Bukkit.getScoreboardManager().getMainScoreboard().getTeam("nocollidehb") != null) {
            Bukkit.getScoreboardManager().getMainScoreboard().getTeam("nocollidehb").unregister();
        }
        
        noCollisionTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("nocollidehb");
        noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        noCollisionTeam.setCanSeeFriendlyInvisibles(false);
    }
    
    private LivingEntity getTargetEntity(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        
        RayTraceResult result = player.getWorld().rayTraceEntities(
            eyeLocation, 
            direction, 
            RAYCAST_DISTANCE,
            entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        
        return result != null ? (LivingEntity)result.getHitEntity() : null;
    }
    
    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity.getType() == EntityType.ARMOR_STAND || entity instanceof Hangable || entity.getType() == EntityType.ITEM_FRAME || entity.getType() == EntityType.GLOW_ITEM_FRAME || entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_DISPLAY || entity.getType() == EntityType.OMINOUS_ITEM_SPAWNER || entity instanceof Minecart || entity instanceof Projectile) {
            return false;
        }
        return true;
    }
    
    private void setupNonCollidableDisplay(ArmorStand display) {
        // Method 1: Add to no-collision team
        noCollisionTeam.addEntry(display.getUniqueId().toString());
        
        // Method 2: Make it invisible to collision detection
        display.setVisibleByDefault(true);
        display.setGlowing(false);
        
        // Method 3: Set the display to be non-solid
        display.setPersistent(false);
    }
    
    private void updateHealthBar(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        
        LivingEntity livingEntity = (LivingEntity) entity;
        
        // Get or create health bar
        ArmorStand healthBar = healthBarEntities.get(livingEntity);
        if (healthBar == null || healthBar.isDead()) {
            healthBar = createHealthBar(entity);
            healthBarEntities.put(livingEntity, healthBar);
            healthbarCounters.put(healthBar, 0);
        }
        
        // Update health display
        double health = livingEntity.getHealth();
        double maxHealth = livingEntity.getAttribute(Attribute.MAX_HEALTH).getValue();
        int displayHealth = (int) Math.ceil(health);
        
        ChatColor colorCode = (health > 0.9 * maxHealth ? ChatColor.GREEN : (health > 0.5 * maxHealth ? ChatColor.YELLOW : (health > 0.1 * maxHealth ? ChatColor.GOLD : ChatColor.RED)));
        String healthText = ChatColor.RED + "❤ " + colorCode + displayHealth;
        //healthBar.setText(healthText);
        healthBar.setCustomName(healthText);
        
        // Update position (slightly above the entity's head)
        Location entityLoc = entity.getLocation();
        Location healthBarLoc = entityLoc.clone().add(0, entity.getHeight() + yOffset, 0);
        healthBar.teleport(healthBarLoc);
    }
    
    private ArmorStand createHealthBar(Entity entity) {
        Location spawnLoc = entity.getLocation().add(0, entity.getHeight() + yOffset, 0);
        World world = entity.getWorld();
        
        ArmorStand healthText = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        // Make the ArmorStand non-collidable
        setupNonCollidableDisplay(healthText);
        //ArmorStand.setText("❤ 0");
        healthText.setCustomName("❤ 0");
        healthText.setCustomNameVisible(true);
        healthText.setVisible(false);
        healthText.setGravity(false);
        healthText.setInvulnerable(true);
        healthText.setMarker(true);
        healthText.setSmall(true);
        //healthText.setBillboard(Display.Billboard.CENTER);
        //healthText.setSeeThrough(false);
        //healthText.setDefaultBackground(false);
        //healthText.addScoreboardTag("FancyHealthBar");
        
        // Make it visible to all players
        healthText.setVisibleByDefault(true);
        
        return healthText;
    }
    
    private void hideUnviewedHealthBars(Set<LivingEntity> currentlyViewed) {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<LivingEntity, Long>> iterator = lastLookedAt.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = iterator.next();
            LivingEntity livingEntity = entry.getKey();
            long lastViewed = entry.getValue();
            
            // If entity is not currently being viewed and hasn't been viewed for HIDE_DELAY
            if (!currentlyViewed.contains(livingEntity) && 
                (currentTime - lastViewed) > HIDE_DELAY) {
                
                // Remove health bar
                ArmorStand healthBar = healthBarEntities.get(livingEntity);
                if (healthBar != null && !healthBar.isDead()) {
                    healthBar.remove();
                }
                
                // Clean up maps
                healthBarEntities.remove(livingEntity);
                iterator.remove();
            }
        }
        
        // Also clean up health bars for entities that no longer exist
        Iterator<Map.Entry<LivingEntity, ArmorStand>> healthBarIterator = healthBarEntities.entrySet().iterator();
        while (healthBarIterator.hasNext()) {
            Map.Entry<LivingEntity, ArmorStand> entry = healthBarIterator.next();
            ArmorStand healthBar = entry.getValue();
            
            if (healthBar == null || healthBar.isDead()) {
                healthBarIterator.remove();
                lastLookedAt.remove(entry.getKey());
            }
        }
    }
}