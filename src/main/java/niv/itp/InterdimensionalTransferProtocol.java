package niv.itp;

import static net.minecraft.world.level.block.Blocks.OBSIDIAN;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

@NullMarked
public class InterdimensionalTransferProtocol implements ModInitializer {

    public static final String MOD_ID = "interdimensional-transfer-protocol";

    public static final String MOD_NAME = "Interdimensional Transfer Protocol";

    @SuppressWarnings("null")
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        ItemStorage.SIDED.registerForBlocks(new ObsidianStorageProvider<>(ItemStorage.SIDED), OBSIDIAN);
        LOGGER.info("({}) Loaded ITP module for {}", MOD_NAME, ItemStorage.SIDED.getId());

        FluidStorage.SIDED.registerForBlocks(new ObsidianStorageProvider<>(FluidStorage.SIDED), OBSIDIAN);
        LOGGER.info("({}) Loaded ITP module for {}", MOD_NAME, FluidStorage.SIDED.getId());

        tryRegisterBurningStorage();

        LOGGER.info("({}) Ready", MOD_NAME);
    }

    private void tryRegisterBurningStorage() {
        try {
            var classBurningStorage = Class.forName("niv.burning.api.BurningStorage");
            var fieldSided = classBurningStorage.getDeclaredField("SIDED");
            if (fieldSided.canAccess(null)) {
                @SuppressWarnings("unchecked")
                var sided = (BlockApiLookup<Storage<Object>, @Nullable Direction>) fieldSided.get(null);
                if (sided != null) {
                    sided.registerForBlocks(new ObsidianStorageProvider<>(sided), OBSIDIAN);
                    LOGGER.info("({}) Loaded ITP module for {}", MOD_NAME, sided.getId());
                }
            }
        } catch (ClassNotFoundException _) {
            // no op
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.warn("({}) Failed to register provider for {}",
                    MOD_NAME, Identifier.parse("burning:sided_storage"), ex);
        }
    }
}
