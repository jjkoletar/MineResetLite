package com.koletar.jj.mineresetlite;

import com.koletar.jj.mineresetlite.commands.MineCommands;
import com.koletar.jj.mineresetlite.commands.PluginCommands;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import com.koletar.jj.mineresetlite.org.mcstats.Metrics;
import org.bukkit.scheduler.BukkitTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class MineResetLite extends JavaPlugin {
    public List<Mine> mines;
    private Logger logger;
    private CommandManager commandManager;
    private WorldEditPlugin worldEdit = null;
    private Metrics metrics = null;
    private int saveTaskId = -1;
    private int resetTaskId = -1;
    private BukkitTask updateTask = null;
    private boolean needsUpdate;
    private boolean isUpdateCritical;

    static {
        ConfigurationSerialization.registerClass(Mine.class);
    }

    private static class IsMineFile implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.contains(".mine.yml");
        }
    }

    private class UpdateWarner implements Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            if (event.getPlayer().hasPermission("mineresetlite.updates") && needsUpdate) {
                event.getPlayer().sendMessage(phrase("updateWarning1"));
                event.getPlayer().sendMessage(phrase("updateWarning2"));
                if (isUpdateCritical) {
                    event.getPlayer().sendMessage(phrase("criticalUpdateWarningDecoration"));
                    event.getPlayer().sendMessage(phrase("criticalUpdateWarning"));
                    event.getPlayer().sendMessage(phrase("criticalUpdateWarningDecoration"));
                }
            }
        }
    }
    public void onEnable() {
        mines = new ArrayList<Mine>();
        logger = getLogger();
        if (!setupConfig()) {
            logger.severe("Since I couldn't setup config files properly, I guess this is goodbye. ");
            logger.severe("Plugin Loading Aborted!");
            return;
        }
        commandManager = new CommandManager();
        commandManager.register(MineCommands.class, new MineCommands(this));
        commandManager.register(CommandManager.class, commandManager);
        commandManager.register(PluginCommands.class, new PluginCommands(this));
        Locale locale = new Locale(Config.getLocale());
        Phrases.getInstance().initialize(locale);
        File overrides = new File(getDataFolder(), "phrases.properties");
        if (overrides.exists()) {
            Properties overridesProps = new Properties();
            try {
                overridesProps.load(new FileInputStream(overrides));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Phrases.getInstance().overrides(overridesProps);
        }
        //Look for worldedit
        if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            worldEdit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        }
        //Metrics
        try {
            metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            logger.warning("MineResetLite couldn't initialize metrics!");
            e.printStackTrace();
        }
        //Load mines
        File[] mineFiles = new File(getDataFolder(), "mines").listFiles(new IsMineFile());
        for (File file : mineFiles) {
            logger.info("Loading mine from file '" + file.getName() + "'...");
            FileConfiguration fileConf = YamlConfiguration.loadConfiguration(file);
            try {
                Object o = fileConf.get("mine");
                if (!(o instanceof Mine)) {
                    logger.severe("Mine wasn't a mine object! Something is off with serialization!");
                    continue;
                }
                Mine mine = (Mine) o;
                mines.add(mine);
            } catch (Throwable t) {
                logger.severe("Unable to load mine!");
            }
        }
        resetTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (Mine mine : mines) {
                    mine.cron();
                }
            }
        }, 60 * 20L, 60 * 20L);
        //Check for updates
        if (!getDescription().getVersion().contains("dev")) {
            updateTask = getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                public void run() {
                    checkUpdates();
                }
            }, 20 * 15);
        }
        getServer().getPluginManager().registerEvents(new UpdateWarner(), this);
        logger.info("MineResetLite version " + getDescription().getVersion() + " enabled!");
    }

    private void checkUpdates() {
        try {
            URL updateFile = new URL("https://api.curseforge.com/servermods/files?projectIds=45520");
            URLConnection conn = updateFile.openConnection();
            conn.addRequestProperty("User-Agent", "MineResetLite/v" + getDescription().getVersion() + " by jjkoletar");
            String rv = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
            JSONArray resp = (JSONArray) JSONValue.parse(rv);
            if (resp.size() == 0) return;
            String name = ((JSONObject) resp.get(resp.size() - 1)).get("name").toString();
            String[] bits = name.split(" ");
            String remoteVer = bits[bits.length - 1];
            int remoteVal = Integer.valueOf(remoteVer.replace(".", ""));
            int localVer = Integer.valueOf(getDescription().getVersion().replace(".", ""));
            if (remoteVal > localVer) {
                needsUpdate = true;

            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void onDisable() {
        getServer().getScheduler().cancelTask(resetTaskId);
        getServer().getScheduler().cancelTask(saveTaskId);
        if (updateTask != null) {
            updateTask.cancel();
        }
        HandlerList.unregisterAll(this);
        save();
        logger.info("MineResetLite disabled");
    }

    public Material matchMaterial(String name) {
        //If anyone can think of a more elegant way to serve this function, let me know. ~jj
        if (name.equalsIgnoreCase("diamondore")) {
            return Material.DIAMOND_ORE;
        } else if (name.equalsIgnoreCase("diamondblock")) {
            return Material.DIAMOND_BLOCK;
        } else if (name.equalsIgnoreCase("ironore")) {
            return Material.IRON_ORE;
        } else if (name.equalsIgnoreCase("ironblock")) {
            return Material.IRON_BLOCK;
        } else if (name.equalsIgnoreCase("goldore")) {
            return Material.GOLD_ORE;
        } else if (name.equalsIgnoreCase("goldblock")) {
            return Material.GOLD_BLOCK;
        } else if (name.equalsIgnoreCase("coalore")) {
            return Material.COAL_ORE;
        } else if (name.equalsIgnoreCase("cake") || name.equalsIgnoreCase("cakeblock")) {
            return Material.CAKE_BLOCK;
        } else if (name.equalsIgnoreCase("emeraldore")) {
            return Material.EMERALD_ORE;
        } else if (name.equalsIgnoreCase("emeraldblock")) {
            return Material.EMERALD_BLOCK;
        } else if (name.equalsIgnoreCase("lapisore")) {
            return Material.LAPIS_ORE;
        } else if (name.equalsIgnoreCase("lapisblock")) {
            return Material.LAPIS_BLOCK;
        } else if (name.equalsIgnoreCase("snowblock") || name.equalsIgnoreCase("snow")) { //I've never seen a mine with snowFALL in it.
            return Material.SNOW_BLOCK;                                                   //Maybe I'll be proven wrong, but it helps 99% of admins.
        } else if (name.equalsIgnoreCase("redstoneore")) {
            return Material.REDSTONE_ORE;
        }
        return Material.matchMaterial(name);
    }

    public Mine[] matchMines(String in) {
        List<Mine> matches = new LinkedList<Mine>();
        for (Mine mine : mines) {
            if (mine.getName().toLowerCase().contains(in.toLowerCase())) {
                matches.add(mine);
            }
        }
        return matches.toArray(new Mine[matches.size()]);
    }

    /**
     * Alert the plugin that changes have been made to mines, but wait 60 seconds before we save.
     * This process saves on disk I/O by waiting until a long string of changes have finished before writing to disk.
     */
    public void buffSave() {
        BukkitScheduler scheduler = getServer().getScheduler();
        if (saveTaskId != -1) {
            //Cancel old task
            scheduler.cancelTask(saveTaskId);
        }
        //Schedule save
        final MineResetLite plugin = this;
        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                plugin.save();
            }
        }, 60 * 20L);
    }

    public void save() {
        for (Mine mine : mines) {
            File mineFile = getMineFile(mine);
            FileConfiguration mineConf = YamlConfiguration.loadConfiguration(mineFile);
            mineConf.set("mine", mine);
            try {
                mineConf.save(mineFile);
            } catch (IOException e) {
                logger.severe("Unable to serialize mine!");
                e.printStackTrace();
            }
        }
    }

    private File getMineFile(Mine mine) {
        return new File(new File(getDataFolder(), "mines"), mine.getName().replace(" ", "") + ".mine.yml");
    }

    public void eraseMine(Mine mine) {
        mines.remove(mine);
        getMineFile(mine).delete();
    }

    public boolean hasWorldEdit() {
        return worldEdit != null;
    }

    public WorldEditPlugin getWorldEdit() {
        return worldEdit;
    }

    private boolean setupConfig() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
            logger.severe("Could not make plugin folder! This won't end well...");
            return false;
        }
        File mineFolder = new File(getDataFolder(), "mines");
        if (!mineFolder.exists() && !mineFolder.mkdir()) {
            logger.severe("Could not make mine folder! Abort! Abort!");
            return false;
        }
        try {
            Config.initConfig(getDataFolder());
        } catch (IOException e) {
            logger.severe("Could not make config file!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mineresetlite")) {
            if (args.length == 0) {
                String[] helpArgs = new String[0];
                commandManager.callCommand("help", sender, helpArgs);
                return true;
            }
            //Spoof args array to account for the initial subcommand specification
            String[] spoofedArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                spoofedArgs[i - 1] = args[i];
            }
            commandManager.callCommand(args[0], sender, spoofedArgs);
            return true;
        }
        return false; //Fallthrough
    }

    public static void broadcast(String message, Mine mine) {
        if (Config.getBroadcastNearbyOnly()) {
            for (Player p : mine.getWorld().getPlayers()) {
                if (mine.isInside(p)) {
                    p.sendMessage(message);
                }
            }
            Bukkit.getLogger().info(message);
        } else if (Config.getBroadcastInWorldOnly()) {
            for (Player p : mine.getWorld().getPlayers()) {
                p.sendMessage(message);
            }
            Bukkit.getLogger().info(message);
        } else {
            for(Player p : Bukkit.getServer().getOnlinePlayers()){
                p.sendMessage(message);
            }
        }
    }
}
