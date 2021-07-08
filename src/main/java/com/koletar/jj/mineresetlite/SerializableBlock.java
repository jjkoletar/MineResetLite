package com.koletar.jj.mineresetlite;

import com.vk2gpz.vklib.mc.material.MaterialUtil;
import org.bukkit.Material;

/**
 * @author jjkoletar
 * @author vk2gpz
 */
public class SerializableBlock {
    private String blockId;
    private byte data;
    transient private Material type;

    public SerializableBlock(int blockId) {
        this(blockId, (byte) 0);
    }

    public SerializableBlock(int blockId, byte data) {
        this(blockId + ":" + data);
    }
    
    public SerializableBlock(String name, byte data) {
        this(name + ":" + data);
    }
    
    public SerializableBlock(String self) {
        String[] bits = self.split(":");
        if (bits.length < 1) {
            throw new IllegalArgumentException("String form of SerializableBlock didn't have sufficient data");
        }
        
        try {
            this.type = MaterialUtil.getMaterial(bits[0]);
            data = (bits.length > 1) ? Byte.valueOf(bits[1]) : (byte) 0;
            this.blockId = this.type.name();
			
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Unable to convert id to integer and data to byte");
        }
    }

    public String getBlockId() {
        return blockId;
    }

    public byte getData() {
        return data;
    }

    public String toString() {
        return blockId + ":" + data;
    }
    
    Material getBlockType() {
    	return this.type;
	}

    public boolean equals(Object o) {
        return o instanceof SerializableBlock && (this.blockId.equals(((SerializableBlock) o).blockId) && this.data == ((SerializableBlock) o).data);
    }
}
