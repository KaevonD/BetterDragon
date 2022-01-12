package me.drysu.harderbosses;

import fr.skytasul.guardianbeam.Laser;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MyListener implements Listener {
    Random rand = new Random();     //random variable for lightning strikes

    boolean secondDragon = false;   //false if current dragon is first, true is current dragon is second
    boolean canDamageCrystals = true;   //if false you can not break end crystals
    boolean waterAttack = false;    //if water attack is happening

    SuperDragon dragon = new SuperDragon();
    HarderBosses plugin;

    List<String> explosion = Arrays.asList(
            "Justice rains from above!",
            "This will make you think twice about blast protection",
            "Boom boom boom boom, I want you in my room~");

    List<String> circleAttack = Arrays.asList(
            "Don't get hit ;)");

    List<String> levitate = Arrays.asList(
            "I hope you brought a bucket!",
            "Up you go!",
            "Hows the view up there?");

    List<String> enterWater = Arrays.asList(
            "Get out of that water",
            "You think you're safe in that water?",
            "Water is cringe");


    public MyListener(HarderBosses listener)
    {
        this.plugin = listener;

    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {

        //if second dragon is respawning, prevent players from breaking crystals that would stop the respawn
        if(e.getEntity() instanceof EnderCrystal && !canDamageCrystals) {
            e.setCancelled(true);
        }

        //if on phase two and dragon is attacked, random chance of player being struck by lightning
        if(e.getEntity() instanceof EnderDragon && secondDragon) {
            int randomNum = rand.nextInt(6);

            //if player uses a bow to attack dragon
            if(e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                Projectile projectile = (Projectile)e.getDamager();

                if(((projectile.getShooter() instanceof Player)) && randomNum == 1){
                    ((Player) projectile.getShooter()).getWorld().strikeLightning(((Player) projectile.getShooter()).getLocation());
                }
            }
            //otherwise if player did not use a bow
            else{
                if(randomNum == 1) {
                    e.getEntity().getWorld().strikeLightning(e.getDamager().getLocation());
                }
            }
        }
    }

    @EventHandler
    public void touchPortal(EntityPortalEnterEvent e) {

        //if player attempts to leave the end during the spawn of phase 2 dragon, teleport them but destroy portal to prevent others
        if(e.getEntity() instanceof Player && e.getLocation().getWorld().getName().equals("world_the_end") && secondDragon) {
            //height of end portals
            Double height = e.getEntity().getWorld().getEnderDragonBattle().getEndPortalLocation().getY();
            //cycle through the blocks by the portal and remove the end portal blocks
            for(double i = -2; i <= 2; i++) {
                for(double j = -2; j <= 2; j++) {
                    Block temp = new Location(e.getLocation().getWorld(), i, height, j).getBlock();
                    if(temp.getType().name().equals("END_PORTAL")) {
                        temp.breakNaturally();
                    }
                }
            }
        }
    }

    @EventHandler
    public void dragonPhaseChange(EnderDragonChangePhaseEvent e) {
        //during second phase have 21 pieces of tnt fall from under the dragon during CIRCLING phase
        if(e.getNewPhase().name().equals("CIRCLING") && secondDragon) {
            new BukkitRunnable() {
                private int i = 0;
                @Override
                public void run() {
                    if(i == 21) {
                        cancel();
                    }
                    ++i;
                    TNTPrimed tnt = e.getEntity().getWorld().spawn(e.getEntity().getLocation(), TNTPrimed.class);
                    tnt.setFuseTicks(80);   //tnt goes off after 4 seconds
                }
            }.runTaskTimer(plugin, 20, 20); //one tnt drops every second

            new BukkitRunnable() {
                @Override
                public void run() {

                    Bukkit.broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + explosion.get(rand.nextInt(explosion.size())));
                    this.cancel();
                }
            }.runTaskLater(plugin, 20);

        }


        //when the dragon perches, use the big particle wave attack
        if(e.getCurrentPhase().name().equals("LAND_ON_PORTAL") && secondDragon) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + circleAttack.get(rand.nextInt(circleAttack.size())));
            //sends out a wave of particles around the dragon that leaves players at 1 heart regardless of HP
            for(float i = 0; i < Math.PI * 2; i += (Math.PI/64)) {
                float finalT = i;
                new BukkitRunnable() {
                    Location l = e.getEntity().getLocation().subtract(0,4,0);

                    int counter = 0;
                    public void run() {
                        counter += 1;
                        double x = Math.cos(finalT);
                        double z = Math.sin(finalT);

                        l.add(x,0,z);
                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 40);
                        e.getEntity().getWorld().spawnParticle(Particle.REDSTONE, l, 1, dustOptions);

                        if(l.getBlock().getType().equals(Material.OBSIDIAN)) {
                            this.cancel();
                        }

                        for(Entity b : l.getChunk().getEntities()) {
                            if(b instanceof Player) {
                                Location head = b.getLocation().add(0, 1.5, 0);

                                if(b.getLocation().distance(l) < 1 || head.distance(l) < 1) {
                                    ((Player) b).damage(1);
                                    ((Player) b).setHealth(2);
                                }
                            }
                        }

                        if(counter == 80) {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 4);
            }
        }

        //levitation attack, happens whenever dragon strafes
        if(e.getCurrentPhase().name().equals("STRAFING") && secondDragon){

            //choose 1/3 of players and put them in a list to be attacked
            ArrayList<Laser> lasers = new ArrayList<>();
            List<Player> playerList = (e.getEntity().getWorld().getPlayers());
            ArrayList<Player> chosenList = new ArrayList<>();
            for(int k=0;k<Math.ceil((double)playerList.size()/3.0);k++) {
                chosenList.add(playerList.get((int)(Math.random()*playerList.size())));
            }

            //create lasers for visual effects from the dragon to player
            //give the players affected levitation
            for(int k=0;k<chosenList.size();k++){
                try {
                    lasers.add( new Laser.CrystalLaser(e.getEntity().getLocation(), chosenList.get(k).getLocation(), 5, 1000));
                    lasers.get(k).start(plugin);
                    chosenList.get(k).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 6));
                } catch (ReflectiveOperationException reflectiveOperationException) {
                    reflectiveOperationException.printStackTrace();
                }
            }
            //this moves the lasers to keep tracked on the players being levitated
            new BukkitRunnable() {
                public void run() {
                    try {
                        for(int k=0;k<chosenList.size();k++) {
                            Location elplayer = chosenList.get(k).getLocation();
                            Location eldragon = e.getEntity().getLocation().add(-1,1.2,1);
                            lasers.get(k).moveEnd(elplayer);
                            lasers.get(k).moveStart(eldragon);
                        }

                    } catch (ReflectiveOperationException reflectiveOperationException) {
                        reflectiveOperationException.printStackTrace();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);

            Bukkit.broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + levitate.get(rand.nextInt(levitate.size())));
        }
    }

    @EventHandler
    public void entityDies(EntityDeathEvent e) {

        //if you beat phase two dragon
        if(secondDragon && e.getEntity() instanceof EnderDragon) {
            secondDragon = false;   //reset back so next dragon spawned is phase one again
            Bukkit.broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + "Ahhhh shiet, you got me");
        }

        //if you beat phase one dragon, this sets up phase two
        else if(!secondDragon && e.getEntity() instanceof EnderDragon){
            secondDragon = true;    //sets phase two to true
            DragonBattle db = e.getEntity().getWorld().getEnderDragonBattle();

            //after a 20 second delay, spawns the second dragon
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + "You thought it was over???");
                    canDamageCrystals = false;  //stop players from breaking end crystals

                    //place the end crystals around the portal
                    e.getEntity().getWorld().spawnEntity(e.getEntity().getWorld().getBlockAt(0,db.getEndPortalLocation().getBlockY()+1,db.getEndPortalLocation().getBlockZ()+3).getLocation().add(0.5,0,0.5), EntityType.ENDER_CRYSTAL);
                    e.getEntity().getWorld().spawnEntity(e.getEntity().getWorld().getBlockAt(0,db.getEndPortalLocation().getBlockY()+1,db.getEndPortalLocation().getBlockZ()-3).getLocation().add(0.5,0,0.5), EntityType.ENDER_CRYSTAL);
                    e.getEntity().getWorld().spawnEntity(e.getEntity().getWorld().getBlockAt(db.getEndPortalLocation().getBlockX()+3,db.getEndPortalLocation().getBlockY()+1,0).getLocation().add(0.5,0,0.5), EntityType.ENDER_CRYSTAL);
                    e.getEntity().getWorld().spawnEntity(e.getEntity().getWorld().getBlockAt(db.getEndPortalLocation().getBlockX()-3,db.getEndPortalLocation().getBlockY()+1,0).getLocation().add(0.5,0,0.5), EntityType.ENDER_CRYSTAL);

                    db.initiateRespawn();   //start spawning the second dragon
                    dragon.setRevive(false);
                }
            }, 500L);
        }
    }

    @EventHandler
    public void entitySpawned(EntitySpawnEvent e) {

        //broadcasts that the second dragon has spawned and allows players to break end crystals again
        if(e.getEntity() instanceof EnderDragon && secondDragon) {
            canDamageCrystals = true;
            Bukkit.broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + "I'M BACK BABY");
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    e.getEntity().getWorld().getEnderDragonBattle().getEnderDragon().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(400.0);
                    e.getEntity().getWorld().getEnderDragonBattle().getEnderDragon().setHealth(400.0);
                }
            }, 460L);
        }
    }

    @EventHandler
    public void TNTCheck(EntityExplodeEvent e){
        //stops tnt from damaging blocks during second phase so that dragon doesn't destroy end
        if(e.getEntity() instanceof TNTPrimed && secondDragon){
            e.blockList().clear();
        }
    }

    @EventHandler
    public void inWater(PlayerMoveEvent e) {

        //get what block player moved in
        Material m = e.getPlayer().getLocation().getBlock().getType();

        //if player is in water during second phase, attack them
        if(m == Material.WATER && e.getPlayer().getWorld().getName().equals("world_the_end") && !waterAttack && secondDragon) {
            waterAttack = true;

            //shoots a red beam of particles at player that damages them if hit
            new BukkitRunnable() {

                Location dragonL = e.getPlayer().getWorld().getEnderDragonBattle().getEnderDragon().getLocation();
                Location playerL = e.getPlayer().getLocation();
                Vector dir = (playerL.toVector().subtract(dragonL.toVector())).normalize();

                double counter = 0;
                public void run() {

                    counter += 0.1;
                    int r = 1;
                    double x = r * dir.getX();
                    double y = r * dir.getY();
                    double z = r * dir.getZ();


                    dragonL.add(x,y,z);

                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1000);
                    e.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, dragonL, 1, dustOptions);

                    for(Entity b : dragonL.getChunk().getEntities()) {
                        Location head = b.getLocation().add(0, 1.5, 0);
                        if((head.distance(dragonL) < 2 || b.getLocation().distance(dragonL) < 2) && b instanceof Player) {
                            ((Player) b).damage(13);
                            waterAttack = false;
                            this.cancel();
                        }
                    }
                    if(counter > 8){
                        waterAttack = false;
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 10,2);
            Bukkit.broadcastMessage(ChatColor.GOLD + "EnderDragon: " + ChatColor.RED + enterWater.get(rand.nextInt(enterWater.size())));

        }
    }

}