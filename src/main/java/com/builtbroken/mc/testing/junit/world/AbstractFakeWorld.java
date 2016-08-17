package com.builtbroken.mc.testing.junit.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Cow Pi on 8/24/2015.
 */
public class AbstractFakeWorld extends World
{
    public boolean debugInfo = true;
    public Logger logger;

    public AbstractFakeWorld(ISaveHandler p_i45369_1_, WorldInfo p_i45369_2_, WorldProvider p_i45369_3_, Profiler p_i45369_4_, boolean p_i45369_5_)
    {
        super(p_i45369_1_, p_i45369_2_, p_i45369_3_, p_i45369_4_, p_i45369_5_);
        logger = LogManager.getLogger("FW-" + p_i45369_2_);
        chunkProvider = new ChunkProviderServer(this, new ChunkProviderEmpty(this));
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState block, int flags)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        debug("");
        debug("setBlockState(" + pos + ", " + block + ")");
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            if (y < 0)
            {
                debug("setBlockState() y level is too low");
                return false;
            } else if (y >= 256)
            {
                debug("setBlockState() y level is too high");
                return false;
            } else
            {
                Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
                debug("setBlock() chunk = " + chunk);
                net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;

                if (this.captureBlockSnapshots && !this.isRemote)
                {
                    blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(this, pos, flags);
                    this.capturedBlockSnapshots.add(blockSnapshot);
                }

                boolean flag = chunk.setBlockState(new BlockPos(x & 15, y, z & 15), block) != null;
                debug("setBlockState() flag = " + flag + " BlockSnapshot = " + blockSnapshot);

                if (!flag && blockSnapshot != null)
                {
                    this.capturedBlockSnapshots.remove(blockSnapshot);
                    blockSnapshot = null;
                }

                this.theProfiler.startSection("checkLight");
                this.checkLightFor(EnumSkyBlock.BLOCK, pos);
                this.theProfiler.endSection();

                if (flag && blockSnapshot == null) // Don't notify clients or update physics while capturing blockstates
                {
                    // Modularize client and physic updates
                    this.markAndNotifyBlock(pos, chunk, null, block, flags);
                }

                return flag;
            }
        } else
        {
            debug("setBlock() too far from zero zero");
            return false;
        }
    }

    @Override
    protected IChunkProvider createChunkProvider()
    {
        IChunkProvider provider = new ChunkProviderEmpty(this);
        this.chunkProvider = provider;
        return provider;
    }

    @Override
    public Entity getEntityByID(int p_73045_1_)
    {
        return null;
    }

    protected void debug(String msg)
    {
        if (debugInfo)
            logger.info(msg);
    }

    @Override
    protected int getRenderDistanceChunks() {
        return 0;
    }
}
