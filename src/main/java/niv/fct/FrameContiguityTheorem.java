package niv.fct;

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
public class FrameContiguityTheorem implements ModInitializer {

    public static final String MOD_ID = "frame-contiguity-theorem";

    public static final String MOD_NAME = "Frame Contiguity Theorem";

    @SuppressWarnings("null")
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static final String OBSIDIAN_FRAME_LOG = "("+ MOD_NAME + ") Obsidian frame corollary proved for {}";

    @Override
    public void onInitialize() {
        ItemStorage.SIDED.registerForBlocks(new ObsidianFrameCorollary<>(ItemStorage.SIDED), OBSIDIAN);
        LOGGER.info(OBSIDIAN_FRAME_LOG, ItemStorage.SIDED.getId());

        FluidStorage.SIDED.registerForBlocks(new ObsidianFrameCorollary<>(FluidStorage.SIDED), OBSIDIAN);
        LOGGER.info(OBSIDIAN_FRAME_LOG, FluidStorage.SIDED.getId());

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
                    sided.registerForBlocks(new ObsidianFrameCorollary<>(sided), OBSIDIAN);
                    LOGGER.info(OBSIDIAN_FRAME_LOG, MOD_NAME, sided.getId());
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
