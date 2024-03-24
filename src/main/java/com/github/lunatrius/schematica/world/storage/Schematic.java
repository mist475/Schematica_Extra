package com.github.lunatrius.schematica.world.storage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematic;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

public class Schematic implements ISchematic {

    private static final ItemStack DEFAULT_ICON = new ItemStack(Blocks.grass);
    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

    private ItemStack icon;
    private final short[][][] blocks;
    private final byte[][][] metadata;
    private final List<TileEntity> tileEntities = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final int width;
    private final int height;
    private final int length;

    public Schematic(final ItemStack icon, final int width, final int height, final int length) {
        this.icon = icon;
        this.blocks = new short[width][height][length];
        this.metadata = new byte[width][height][length];

        this.width = width;
        this.height = height;
        this.length = length;
    }

    @Override
    public Block getBlock(final int x, final int y, final int z) {
        if (!isValid(x, y, z)) {
            return Blocks.air;
        }

        return BLOCK_REGISTRY.getObjectById(this.blocks[x][y][z]);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final Block block) {
        return setBlock(x, y, z, block, 0);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final Block block, final int metadata) {
        if (!isValid(x, y, z)) {
            return false;
        }

        final int id = BLOCK_REGISTRY.getId(block);
        if (id == -1) {
            return false;
        }

        this.blocks[x][y][z] = (short) id;
        setBlockMetadata(x, y, z, metadata);
        return true;
    }

    @Override
    public TileEntity getTileEntity(final int x, final int y, final int z) {
        for (final TileEntity tileEntity : this.tileEntities) {
            if (tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z) {
                return tileEntity;
            }
        }

        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return this.tileEntities;
    }

    @Override
    public void setTileEntity(final int x, final int y, final int z, final TileEntity tileEntity) {
        if (!isValid(x, y, z)) {
            return;
        }

        this.removeTileEntity(x, y, z);

        if (tileEntity != null) {
            this.tileEntities.add(tileEntity);
        }
    }

    @Override
    public void removeTileEntity(final int x, final int y, final int z) {

        this.tileEntities
                .removeIf(tileEntity -> tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z);
    }

    @Override
    public int getBlockMetadata(final int x, final int y, final int z) {
        if (!isValid(x, y, z)) {
            return 0;
        }

        return this.metadata[x][y][z];
    }

    @Override
    public boolean setBlockMetadata(final int x, final int y, final int z, final int metadata) {
        if (!isValid(x, y, z)) {
            return false;
        }

        this.metadata[x][y][z] = (byte) (metadata & 0x0F);
        return true;
    }

    @Override
    public List<Entity> getEntities() {
        return this.entities;
    }

    @Override
    public void addEntity(final Entity entity) {
        if (entity == null || entity.getUniqueID() == null || entity instanceof EntityPlayer) {
            return;
        }

        for (final Entity e : this.entities) {
            if (entity.getUniqueID().equals(e.getUniqueID())) {
                return;
            }
        }

        this.entities.add(entity);
    }

    @Override
    public void removeEntity(final Entity entity) {
        if (entity == null || entity.getUniqueID() == null) {
            return;
        }

        this.entities.removeIf(e -> entity.getUniqueID().equals(e.getUniqueID()));
    }

    @Override
    public ItemStack getIcon() {
        return this.icon;
    }

    @Override
    public void setIcon(final ItemStack icon) {
        if (icon != null) {
            this.icon = icon;
        } else {
            this.icon = DEFAULT_ICON.copy();
        }
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getLength() {
        return this.length;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    private boolean isValid(final int x, final int y, final int z) {
        return !(x < 0 || y < 0 || z < 0 || x >= this.width || y >= this.height || z >= this.length);
    }
}
