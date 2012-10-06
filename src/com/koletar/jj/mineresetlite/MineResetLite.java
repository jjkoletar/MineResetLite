package com.koletar.jj.mineresetlite;

import com.koletar.jj.mineresetlite.commands.MineCommands;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.apache.commons.io.IOUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mcstats.Metrics;
import org.yaml.snakeyaml.Yaml;
import sun.net.www.MimeEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author jjkoletar
 */
public class MineResetLite extends JavaPlugin {
    public List<Mine> mines;
    private Logger logger;
    private static final int CONFIG_REVISION = 0;
    private FileConfiguration config;
    private CommandManager commandManager;
    private WorldEditPlugin worldEdit = null;
    private Metrics metrics = null;
    private int saveTaskId = -1;
    private int resetTaskId = -1;

    static {
        ConfigurationSerialization.registerClass(Mine.class);
    }
    public void onEnable() {
        mines = new ArrayList<Mine>();
        logger = getLogger();
        if (!setupConfig()) {
            logger.severe("Since I couldn't setup config files properly, I guess this is goodbye. ");
            logger.severe("Plugin Loading Aborted!");
            return;
        }
        config = getConfig();
        commandManager = new CommandManager();
        commandManager.register(MineCommands.class, new MineCommands(this));
        commandManager.register(CommandManager.class, commandManager);
        Locale locale = Locale.ENGLISH;
        Phrases.getInstance().initialize(locale);
        //Look for worldedit
        if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            worldEdit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        }
        //Metrics
        /*try {       //TODO: Remove dev negation of stats
            metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            logger.warning("MineResetLite couldn't initialize metrics!");
            e.printStackTrace();
        }          */
        //Load mines
        File[] mineFiles = new File(getDataFolder(), "mines").listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.contains(".mine.yml");
            }
        });
        for (File file : mineFiles) {
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
        logger.info("MineResetLite version " + getDescription().getVersion() + " enabled!");
    }

    public void onDisable() {
        getServer().getScheduler().cancelTask(resetTaskId);
        save();
        logger.info("MineResetLite disabled");
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
        File configFile = new File(getDataFolder(), "config.yml");
        try {
            if (!configFile.exists() && !configFile.createNewFile()) {
                InputStream defaultConfig = getResource("config.yml");
                if (defaultConfig == null) {
                    logger.severe("Packaged config.yml not found. Poor building of the jar?");
                    return false;
                }
                OutputStream conf = new FileOutputStream(configFile);
                IOUtils.copy(defaultConfig, conf);
                defaultConfig.close();
                conf.close();
            }
        } catch (IOException e) {
            logger.severe("IOException whilst writing the default config file!");
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
}
