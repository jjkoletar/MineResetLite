package com.koletar.jj.mineresetlite;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author jjkoletar
 */
public class Config {
    private Config() {}
    private static boolean broadcastInWorldOnly = false;
    private static boolean checkForUpdates = true;

    public static boolean getBroadcastInWorldOnly() {
        return broadcastInWorldOnly;
    }

    protected static void setBroadcastInWorldOnly(boolean broadcastInWorldOnly) {
        Config.broadcastInWorldOnly = broadcastInWorldOnly;
    }

    public static void writeBroadcastInWorldOnly(BufferedWriter out) throws IOException {
        out.write("# If you have multiple worlds, and wish for only the worlds in which your mine resides to receive");
        out.newLine();
        out.write("# reset notifications, and automatic reset warnings, set this to true.");
        out.newLine();
        out.write("broadcast-in-world-only: false");
        out.newLine();
    }

    public static boolean getCheckForUpdates() {
        return checkForUpdates;
    }

    protected static void setCheckForUpdates(boolean checkForUpdates) {
        Config.checkForUpdates = checkForUpdates;
    }

    public static void writeCheckForUpdates(BufferedWriter out) throws IOException {
        out.write("# When true, this config option enables update alerts. I do not send any extra information along when ");
        out.newLine();
        out.write("# checking, and query a static file hosted on Dropbox. ");
        out.newLine();
        out.write("check-for-updates: true");
        out.newLine();
    }

    public static void initConfig(File dataFolder) throws IOException {
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            configFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
            out.write("# MineResetLite Configuration File");
            out.newLine();
            Config.writeBroadcastInWorldOnly(out);
            Config.writeCheckForUpdates(out);
            out.close();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        BufferedWriter out = new BufferedWriter(new FileWriter(configFile, true));
        if (config.contains("broadcast-in-world-only")) {
            Config.setBroadcastInWorldOnly(config.getBoolean("broadcast-in-world-only"));
        } else {
            Config.writeBroadcastInWorldOnly(out);
        }
        if (config.contains("check-for-updates")) {
            Config.setCheckForUpdates(config.getBoolean("check-for-updates"));
        } else {
            Config.writeCheckForUpdates(out);
        }
        out.close();
    }
}
