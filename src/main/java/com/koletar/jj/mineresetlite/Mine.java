package com.koletar.jj.mineresetlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

/**
 * @author jjkoletar
 */
public class Mine implements ConfigurationSerializable {
	private int								minX;
	private int								minY;
	private int								minZ;
	private int								maxX;
	private int								maxY;
	private int								maxZ;
	private World							world;
	private Map<SerializableBlock, Double>	composition;
	private int								resetDelay;
	private List<Integer>					resetWarnings;
	private String							name;
	private SerializableBlock				surface;
	private boolean							fillMode;
	private int								resetClock;
	private boolean							isSilent;
	private boolean							ignoreLadders = false;
	
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
		if (world == null) {
			Logger l = Bukkit.getLogger();
			l.severe("[MineResetLite] Unable to find a world! Please include these logger lines along with the stack trace when reporting this bug!");
			l.severe("[MineResetLite] Attempted to load world named: " + me.get("world"));
			l.severe("[MineResetLite] Worlds listed: " + StringTools.buildList(Bukkit.getWorlds(), "", ", "));
			throw new IllegalArgumentException("World was null!");
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
		if (me.containsKey("surface")) {
			if (!me.get("surface").equals("")) {
				surface = new SerializableBlock((String) me.get("surface"));
			}
		}
		if (me.containsKey("fillMode")) {
			fillMode = (Boolean) me.get("fillMode");
		}
		if (me.containsKey("resetClock")) {
			resetClock = (Integer) me.get("resetClock");
		}
		// Compat for the clock
		if (resetDelay > 0 && resetClock == 0) {
			resetClock = resetDelay;
		}
		if (me.containsKey("isSilent")) {
			isSilent = (Boolean) me.get("isSilent");
		}
		if (me.containsKey("ignoreLadders")) {
			ignoreLadders = (Boolean) me.get("ignoreLadders");
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
		// Make string form of composition
		Map<String, Double> sComposition = new HashMap<String, Double>();
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			sComposition.put(entry.getKey().toString(), entry.getValue());
		}
		me.put("composition", sComposition);
		me.put("name", name);
		me.put("resetDelay", resetDelay);
		List<String> warnings = new LinkedList<String>();
		for (Integer warning : resetWarnings) {
			warnings.add(warning.toString());
		}
		me.put("resetWarnings", warnings);
		if (surface != null) {
			me.put("surface", surface.toString());
		} else {
			me.put("surface", "");
		}
		me.put("fillMode", fillMode);
		me.put("resetClock", resetClock);
		me.put("isSilent", isSilent);
		me.put("ignoreLadders", ignoreLadders);
		return me;
	}
	
	public boolean getFillMode() {
		return fillMode;
	}
	
	public void setFillMode(boolean fillMode) {
		this.fillMode = fillMode;
	}
	
	public List<Integer> getResetWarnings() {
		return resetWarnings;
	}
	
	public void setResetWarnings(List<Integer> warnings) {
		resetWarnings = warnings;
	}
	
	public int getResetDelay() {
		return resetDelay;
	}
	
	public void setResetDelay(int minutes) {
		resetDelay = minutes;
		resetClock = minutes;
	}
	
	/**
	 * Return the length of time until the next automatic reset. The actual
	 * length of time is anywhere between n and n-1 minutes.
	 *
	 * @return clock ticks left until reset
	 */
	public int getTimeUntilReset() {
		return resetClock;
	}
	
	public SerializableBlock getSurface() {
		return surface;
	}
	
	public void setSurface(SerializableBlock surface) {
		this.surface = surface;
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
	
	public double getCompositionTotal() {
		double total = 0;
		for (Double d : composition.values()) {
			total += d;
		}
		return total;
	}
	
	public int getMinX() {
		return minX;
	}
	
	public void setMinX(int minX) {
		this.minX = minX;
	}
	
	public int getMinY() {
		return minY;
	}
	
	public void setMinY(int minY) {
		this.minY = minY;
	}
	
	public int getMinZ() {
		return minZ;
	}
	
	public void setMinZ(int minZ) {
		this.minZ = minZ;
	}
	
	public int getMaxX() {
		return maxX;
	}
	
	public void setMaxX(int maxX) {
		this.maxX = maxX;
	}
	
	public int getMaxY() {
		return maxY;
	}
	
	public void setMaxY(int maxY) {
		this.maxY = maxY;
	}
	
	public int getMaxZ() {
		return maxZ;
	}
	
	public void setMaxZ(int maxZ) {
		this.maxZ = maxZ;
	}
	
	public boolean isSilent() {
		return isSilent;
	}
	
	public void setSilence(boolean isSilent) {
		this.isSilent = isSilent;
	}
	
	public boolean isIgnoreLadders() {
		return ignoreLadders;
	}

	public void setIgnoreLadders(boolean ignoreLadders) {
		this.ignoreLadders = ignoreLadders;
	}

	public boolean isInside(Player p) {
		return isInside(p.getLocation());
	}
	
	public boolean isInside(Location l) {
		return (l.getWorld().getName().equals(getWorld().getName()))
				&& (l.getBlockX() >= minX && l.getBlockX() <= maxX) && (l.getBlockY() >= minY && l.getBlockY() <= maxY)
				&& (l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ);
	}
	
	public void reset() {
		// Get probability map
		List<CompositionEntry> probabilityMap = mapComposition(composition);
		// Pull players out
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			Location l = p.getLocation();
			if (isInside(p)) {
				// make sure we find a safe location above the mine
				Location tp = new Location(world, l.getX(), maxY + 1, l.getZ());
				Block block = tp.getBlock();
				
				// check to make sure we don't suffocate player
				if (block.getType() != Material.AIR || block.getRelative(BlockFace.UP).getType() != Material.AIR) {
					tp = new Location(world, l.getX(), l.getWorld().getHighestBlockYAt(l.getBlockX(), l.getBlockZ()),
							l.getZ());
				}
				p.teleport(tp);
			}
		}
		// Actually reset
		Random rand = new Random();
		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				for (int z = minZ; z <= maxZ; ++z) {
					if (!fillMode || world.getBlockTypeIdAt(x, y, z) == 0) {
						if(world.getBlockTypeIdAt(x, y, z) == 65 & ignoreLadders) {
							continue;
						}
						
						if (y == maxY && surface != null) {
							world.getBlockAt(x, y, z).setTypeIdAndData(surface.getBlockId(), surface.getData(), false);
							continue;
						}
						double r = rand.nextDouble();
						for (CompositionEntry ce : probabilityMap) {
							if (r <= ce.getChance()) {
								world.getBlockAt(x, y, z).setTypeIdAndData(ce.getBlock().getBlockId(),
										ce.getBlock().getData(), false);
								break;
							}
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
		if (resetClock > 0) {
			resetClock--; // Tick down to the reset
		}
		if (resetClock == 0) {
			if (!isSilent) {
				MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", this), this);
			}
			reset();
			resetClock = resetDelay;
			return;
		}
		for (Integer warning : resetWarnings) {
			if (warning == resetClock) {
				MineResetLite.broadcast(Phrases.phrase("mineWarningBroadcast", this, warning), this);
			}
		}
	}
	
	public static class CompositionEntry {
		private SerializableBlock	block;
		private double				chance;
		
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
		// Pad the remaining percentages with air
		if (max < 1) {
			composition.put(new SerializableBlock(0), 1 - max);
			max = 1;
		}
		double i = 0;
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			double v = entry.getValue().doubleValue() / max;
			i += v;
			probabilityMap.add(new CompositionEntry(entry.getKey(), i));
		}
		return probabilityMap;
	}
	
	public void teleport(Player player) {
		Location max = new Location(world, Math.max(this.maxX, this.minX), this.maxY, Math.max(this.maxZ, this.minZ));
		Location min = new Location(world, Math.min(this.maxX, this.minX), this.minY, Math.min(this.maxZ, this.minZ));
		
		Location location = max.add(min).multiply(0.5);
		Block block = location.getBlock();
		
		if (block.getType() != Material.AIR || block.getRelative(BlockFace.UP).getType() != Material.AIR) {
			location = new Location(world, location.getX(), location.getWorld().getHighestBlockYAt(
					location.getBlockX(), location.getBlockZ()), location.getZ());
		}
		
		player.teleport(location);
	}
	
	public void redefine(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, World world) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.world = world;
	}
	
}
