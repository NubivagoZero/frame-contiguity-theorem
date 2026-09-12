package niv.itp;

import java.util.ArrayList;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup.BlockApiProvider;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.PortalShape;

@NullMarked
public class ObsidianStorageProvider<T> implements BlockApiProvider<Storage<T>, @Nullable Direction> {

    private final BlockApiLookup<Storage<T>, @Nullable Direction> lookup;

    public ObsidianStorageProvider(final BlockApiLookup<Storage<T>, @Nullable Direction> lookup) {
        this.lookup = lookup;
    }

    @Override
    public @Nullable Storage<T> find(Level level, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        if (side == null || !isNetherPortalFrame(level, pos, state, side))
            return null;

        var targetLevel = getTargetLevel(level);
        if (targetLevel == null)
            return new InsertionOnlyDummyStorage<T>();

        var targetPortalPos = getTargetPortalPos(level, targetLevel, pos.relative(side, 2));
        if (targetPortalPos == null)
            return new InsertionOnlyDummyStorage<T>();

        var targetPortalAxis = getTargetPortalAxis(targetLevel, targetPortalPos);
        if (targetPortalAxis == null)
            return new InsertionOnlyDummyStorage<T>();

        var targetBlockPosArray = getTargetStoragePosArray(
                targetLevel, targetPortalPos, targetPortalAxis, side);

        var targetSide = side.getAxis() == Axis.Y ? side
                : Direction.fromAxisAndDirection(targetPortalAxis, side.getAxisDirection());

        var targetStorageParts = new ArrayList<Storage<T>>(targetBlockPosArray.length);
        for (var targetBlockPos : targetBlockPosArray) {
            if (targetBlockPos != null) {
                var storage = lookup.find(targetLevel, targetBlockPos, targetSide);
                if (storage != null)
                    targetStorageParts.add(storage);
            }
        }

        return new CombinedStorage<>(targetStorageParts);
    }

    private static boolean isNetherPortalFrame(
            Level level, BlockPos blockPos, BlockState blockState, Direction side) {
        return (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER)
                && blockState.is(Blocks.OBSIDIAN)
                && Optional
                        .of(level.getBlockState(blockPos.relative(side.getOpposite())))
                        .filter(state -> state.is(Blocks.NETHER_PORTAL))
                        .flatMap(state -> state.getOptionalValue(NetherPortalBlock.AXIS))
                        .filter(axis -> side.getAxis() == Axis.Y || side.getAxis() == axis)
                        .isPresent();
    }

    private static @Nullable ServerLevel getTargetLevel(Level sourceLevel) {
        var server = sourceLevel.getServer();
        var dimension = sourceLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        return server == null ? null : server.getLevel(dimension);
    }

    private static @Nullable BlockPos getTargetPortalPos(
            Level sourceLevel, ServerLevel targetLevel, BlockPos sourcePortalPos) {
        var targetWorldBorder = targetLevel.getWorldBorder();
        var teleportationScale = DimensionType.getTeleportationScale(
                sourceLevel.dimensionType(),
                targetLevel.dimensionType());
        return targetLevel.getPortalForcer().findClosestPortalPosition(
                targetWorldBorder.clampToBounds(
                        sourcePortalPos.getX() * teleportationScale,
                        sourcePortalPos.getY(),
                        sourcePortalPos.getZ() * teleportationScale),
                targetLevel.dimension() == Level.NETHER, targetWorldBorder)
                .orElse(null);
    }

    private static @Nullable Axis getTargetPortalAxis(
            ServerLevel targetLevel, BlockPos targetPortalPos) {
        var targetPortalState = targetLevel.getBlockState(targetPortalPos);
        if (targetPortalState.is(Blocks.NETHER_PORTAL)) {
            return targetPortalState.getOptionalValue(NetherPortalBlock.AXIS).orElse(null);
        } else {
            return null;
        }
    }

    private static BlockPos[] getTargetStoragePosArray(
            ServerLevel targetLevel, BlockPos targetPortalPos, Axis targetPortalAxis, Direction side) {

        @SuppressWarnings("null")
        var rectangle = BlockUtil.getLargestRectangleAround(targetPortalPos,
                targetPortalAxis, PortalShape.MAX_WIDTH,
                Axis.Y, PortalShape.MAX_HEIGHT,
                testPos -> targetLevel.getBlockState(testPos).is(Blocks.NETHER_PORTAL));

        var list = new ArrayList<BlockPos>(Math.max(rectangle.axis1Size, rectangle.axis2Size));

        var offset = side.getAxisDirection() == AxisDirection.NEGATIVE ? rectangle.axis1Size + 1 : -2;

        if (side.getAxis() == Axis.Y) {
            var pos = rectangle.minCorner.relative(Axis.Y, offset);
            for (int i = 0; i < rectangle.axis1Size; i++)
                list.add(pos.relative(targetPortalAxis, i));
        } else {
            var pos = rectangle.minCorner.relative(targetPortalAxis, offset);
            for (int i = 0; i < rectangle.axis2Size; i++)
                list.add(pos.above(i));
        }

        var array = list.toArray(BlockPos[]::new);
        var random = targetLevel.getRandom();

        if (array.length >= 1) {
            // Fisher-Yates Shuffle
            for (var i = array.length - 1; i > 0; i--) {
                var j = random.nextInt(i + 1);
                var dir = array[i];
                array[i] = array[j];
                array[j] = dir;
            }
        }

        return array;
    }

    private static final class InsertionOnlyDummyStorage<T> implements InsertionOnlyStorage<T> {
        @Override
        public long insert(T resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }
    }
}
