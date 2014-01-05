package com.koletar.jj.mineresetlite;

/**
 * @author jjkoletar
 */
public class SerializableBlock {
    private int blockId;
    private byte data;

    public SerializableBlock(int blockId) {
        this.blockId = blockId;
        data = 0;
    }

    public SerializableBlock(int blockId, byte data) {
        this.blockId = blockId;
        this.data = data;
    }

    public SerializableBlock(String self) {
        String[] bits = self.split(":");
        if (bits.length != 2) {
            throw new IllegalArgumentException("String form of SerializableBlock didn't have exactly 2 numbers");
        }
        try {
            blockId = Integer.valueOf(bits[0]);
            data = Byte.valueOf(bits[1]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Unable to convert id to integer and data to byte");
        }
    }

    public int getBlockId() {
        return blockId;
    }

    public byte getData() {
        return data;
    }

    public String toString() {
        return blockId + ":" + data;
    }

    public boolean equals(Object o) {
        return o instanceof SerializableBlock && (this.blockId == ((SerializableBlock) o).blockId && this.data == ((SerializableBlock) o).data);
    }
}
