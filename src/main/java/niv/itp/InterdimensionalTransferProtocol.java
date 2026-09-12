package niv.itp;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.level.block.Blocks;

@NullMarked
public class InterdimensionalTransferProtocol implements ModInitializer {

    public static final String MOD_ID = "interdimensional-transfer-protocol";

    public static final String MOD_NAME = "Interdimensional Transfer Protocol";

    @SuppressWarnings("null")
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ItemStorage.SIDED.registerForBlocks(new ObsidianStorageProvider<ItemVariant>(ItemStorage.SIDED), Blocks.OBSIDIAN);
        FluidStorage.SIDED.registerForBlocks(new ObsidianStorageProvider<FluidVariant>(FluidStorage.SIDED), Blocks.OBSIDIAN);

        LOGGER.info("({}) Ready", MOD_NAME);
    }
}
