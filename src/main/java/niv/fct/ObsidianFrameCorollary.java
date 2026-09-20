package niv.fct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup.BlockApiProvider;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
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
public class ObsidianFrameCorollary<T> implements BlockApiProvider<Storage<T>, @Nullable Direction> {

    private final BlockApiLookup<Storage<T>, @Nullable Direction> lookup;

    public ObsidianFrameCorollary(final BlockApiLookup<Storage<T>, @Nullable Direction> lookup) {
        this.lookup = lookup;
    }

    @Override
    public @Nullable Storage<T> find(Level level, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        if (side == null || !isNetherPortalFrame(level, pos, state, side))
            return null;

        var targetLevel = getTargetLevel(level);
        if (targetLevel == null)
            return Storage.empty();

        var targetPos = getTargetPortalPos(level, targetLevel, pos.relative(side, 2));
        if (targetPos == null)
            return Storage.empty();

        var axis = getTargetPortalAxis(targetLevel, targetPos);
        if (axis == null)
            return Storage.empty();

        var parts = getTargetStorageList(targetLevel, targetPos, axis, side);
        return parts.isEmpty() ? Storage.empty() : new CombinedStorage<>(parts);
    }

    private boolean isNetherPortalFrame(
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

    private @Nullable ServerLevel getTargetLevel(Level sourceLevel) {
        var server = sourceLevel.getServer();
        var dimension = sourceLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        return server == null ? null : server.getLevel(dimension);
    }

    private @Nullable BlockPos getTargetPortalPos(
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

    private @Nullable Axis getTargetPortalAxis(
            ServerLevel targetLevel, BlockPos targetPortalPos) {
        var targetPortalState = targetLevel.getBlockState(targetPortalPos);
        if (targetPortalState.is(Blocks.NETHER_PORTAL)) {
            return targetPortalState.getOptionalValue(NetherPortalBlock.AXIS).orElse(null);
        } else {
            return null;
        }
    }

    private List<Storage<T>> getTargetStorageList(
            ServerLevel level, BlockPos pos, Axis axis, Direction side) {

        var rectangle = BlockUtil.getLargestRectangleAround(pos,
                axis, PortalShape.MAX_WIDTH,
                Axis.Y, PortalShape.MAX_HEIGHT,
                testPos -> testPos != null && level.getBlockState(testPos).is(Blocks.NETHER_PORTAL));

        var list = new ArrayList<Storage<T>>(Math.max(rectangle.axis1Size, rectangle.axis2Size));

        if (side.getAxis() == Axis.Y) {
            var zeroPos = rectangle.minCorner.relative(Axis.Y,
                    side.getAxisDirection() == AxisDirection.NEGATIVE ? rectangle.axis1Size + 1 : -2);
            for (int i = 0; i < rectangle.axis1Size; i++)
                this.tryFindAndAddStorage(level, zeroPos.relative(axis, i), side, list);

        } else {
            var leftPos = rectangle.minCorner.relative(axis.getNegative(), 2);
            var rightPos = rectangle.minCorner.relative(axis.getPositive(), rectangle.axis1Size + 1);
            for (int i = 0; i < rectangle.axis2Size; i++) {
                this.tryFindAndAddStorage(level, leftPos.above(i), axis.getPositive(), list);
                this.tryFindAndAddStorage(level, rightPos.above(i), axis.getNegative(), list);
            }
        }

        var seed = level.getGameTime() * pos.hashCode();
        if (seed != 0 && list.size() > 1)
            Collections.shuffle(list, new Random(seed));
        return list;
    }

    private void tryFindAndAddStorage(ServerLevel level, BlockPos pos, Direction side, List<Storage<T>> list) {
        var rightStorage = this.lookup.find(level, pos, side);
        if (rightStorage != null)
            list.add(rightStorage);
    }
}
