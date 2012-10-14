package com.koletar.jj.mineresetlite;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author jjkoletar
 */
public class Mine implements ConfigurationSerializable {
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private World world;
    private Map<SerializableBlock, Double> composition;
    private long resetTime;
    private int resetDelay;
    private List<Integer> resetWarnings;
    private String name;

    public Mine(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String name, World world) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.name = name;
        this.world = world;
        composition = new HashMap<SerializableBlock, Double>();
        resetWarnings = new LinkedList<Integer>();
    }

    public Mine(Map<String, Object> me) {
        try {
            minX = (Integer) me.get("minX");
            minY = (Integer) me.get("minY");
            minZ = (Integer) me.get("minZ");
            maxX = (Integer) me.get("maxX");
            maxY = (Integer) me.get("maxY");
            maxZ = (Integer) me.get("maxZ");
        } catch (Throwable t) {
            throw new IllegalArgumentException("Error deserializing coordinate pairs");
        }
        try {
            world = Bukkit.getServer().getWorld((String) me.get("world"));
        } catch (Throwable t) {
            throw new IllegalArgumentException("Error finding world");
        }
        try {
            Map<String, Double> sComposition = (Map<String, Double>) me.get("composition");
            composition = new HashMap<SerializableBlock, Double>();
            for (Map.Entry<String, Double> entry : sComposition.entrySet()) {
                composition.put(new SerializableBlock(entry.getKey()), entry.getValue());
            }
        } catch (Throwable t) {
            throw new IllegalArgumentException("Error deserializing composition");
        }
        name = (String) me.get("name");
        resetTime = Long.valueOf(me.get("resetTime").toString());
        resetDelay = (Integer) me.get("resetDelay");
        List<String> warnings = (List<String>) me.get("resetWarnings");
        resetWarnings = new LinkedList<Integer>();
        for (String warning : warnings) {
            try {
                resetWarnings.add(Integer.valueOf(warning));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Non-numeric reset warnings supplied");
            }
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> me = new HashMap<String, Object>();
        me.put("minX", minX);
        me.put("minY", minY);
        me.put("minZ", minZ);
        me.put("maxX", maxX);
        me.put("maxY", maxY);
        me.put("maxZ", maxZ);
        me.put("world", world.getName());
        //Make string form of composition
        Map<String, Double> sComposition = new HashMap<String, Double>();
        for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
            sComposition.put(entry.getKey().toString(), entry.getValue());
        }
        me.put("composition", sComposition);
        me.put("name", name);
        me.put("resetTime", resetTime);
        me.put("resetDelay", resetDelay);
        List<String> warnings = new LinkedList<String>();
        for (Integer warning : resetWarnings) {
            warnings.add(warning.toString());
        }
        me.put("resetWarnings", warnings);
        return me;
    }

    public void setResetDelay(int minutes) {
        resetDelay = minutes;
        resetTime = world.getFullTime() + (resetDelay * 60L * 20L);
    }

    public void setResetWarnings(List<Integer> warnings) {
        resetWarnings = warnings;
    }

    public List<Integer> getResetWarnings() {
        return resetWarnings;
    }

    public int getResetDelay() {
        return resetDelay;
    }

    public long getResetTime() {
        return resetTime;
    }

    public World getWorld() {
        return world;
    }

    public String getName() {
        return name;
    }

    public Map<SerializableBlock, Double> getComposition() {
        return composition;
    }

    public void reset() {
        //Get probability map
        List<CompositionEntry> probabilityMap = mapComposition(composition);
        //Pull players out
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            Location l = p.getLocation();
            if ((l.getX() >= minX && l.getX() <= maxX)
                    && (l.getY() >= minY && l.getY() <= maxY)
                    && (l.getZ() >= minZ && l.getZ() <= maxZ)) {
                p.teleport(new Location(world, l.getX(), maxY + 2D, l.getZ()));
            }
        }
        //Actually reset
        Random rand = new Random(world.getTime());
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    double r = rand.nextDouble();
                    for (CompositionEntry ce : probabilityMap) {
                        if (r <= ce.getChance()) {
                            world.getBlockAt(x, y, z).setTypeIdAndData(ce.getBlock().getBlockId(), ce.getBlock().getData(), false);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void cron() {
        if (resetDelay == 0) {
            return;
        }
        long worldTime = world.getFullTime();
        if (worldTime >= resetTime) {
            reset();
            resetTime = world.getFullTime() + (resetDelay * 60L * 20L);
            Bukkit.getServer().broadcastMessage(Phrases.phrase("mineAutoResetBroadcast", this));
        }
        int minutesUntilReset = (int) ((resetTime - worldTime) / (60 * 20));
        for (Integer warning : resetWarnings) {
            if (warning == minutesUntilReset) {
                Bukkit.getServer().broadcastMessage(Phrases.phrase("mineWarningBroadcast", this, warning));
            }
        }
    }

    public static class CompositionEntry {
        private SerializableBlock block;
        private double chance;

        public CompositionEntry(SerializableBlock block, double chance) {
            this.block = block;
            this.chance = chance;
        }

        public SerializableBlock getBlock() {
            return block;
        }

        public double getChance() {
            return chance;
        }
    }

    public static ArrayList<CompositionEntry> mapComposition(Map<SerializableBlock, Double> compositionIn) {
        ArrayList<CompositionEntry> probabilityMap = new ArrayList<CompositionEntry>();
        Map<SerializableBlock, Double> composition = new HashMap<SerializableBlock, Double>(compositionIn);
        double max = 0;
        for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
            max += entry.getValue().doubleValue();
        }
        //Pad the remaining percentages with air
        if (max < 1) {
            composition.put(new SerializableBlock(0), 1 - max);
            max = 1;
        }
        double i = 0;
        for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet())  {
            double v = entry.getValue().doubleValue() / max;
            i += v;
            probabilityMap.add(new CompositionEntry(entry.getKey(), i));
        }
        return probabilityMap;
    }
}
