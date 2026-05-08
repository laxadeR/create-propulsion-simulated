package dev.propulsionteam.propulsionsimulated.content.cable.relay;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class CableRelayBlock extends Block implements IBE<CableRelayBlockEntity>, IWrenchable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public CableRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        updateCluster(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide) return;
        updateCluster(level, pos);
    }

    /**
     * BFS across all connected relay blocks to determine shared power state.
     * A relay cluster is powered if ANY relay in it has an external (non-relay) neighbour
     * providing a signal. This prevents feedback loops when relays are chained.
     */
    private static void updateCluster(Level level, BlockPos start) {
        // 1. Collect the whole connected relay cluster.
        Set<BlockPos> cluster = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!cluster.add(current)) continue;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (level.getBlockState(neighbor).getBlock() instanceof CableRelayBlock
                        && !cluster.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        // 2. Check if any relay in the cluster has an external signal.
        boolean powered = false;
        outer:
        for (BlockPos relayPos : cluster) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = relayPos.relative(dir);
                // Skip other relays — they are part of the cluster, not an external source.
                if (level.getBlockState(neighborPos).getBlock() instanceof CableRelayBlock) continue;
                if (level.getSignal(neighborPos, dir) > 0) {
                    powered = true;
                    break outer;
                }
            }
        }

        // 3. Apply the shared state to every relay in the cluster.
        for (BlockPos relayPos : cluster) {
            BlockState relayState = level.getBlockState(relayPos);
            if (relayState.getBlock() instanceof CableRelayBlock
                    && relayState.getValue(POWERED) != powered) {
                level.setBlock(relayPos, relayState.setValue(POWERED, powered), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public Class<CableRelayBlockEntity> getBlockEntityClass() {
        return CableRelayBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CableRelayBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableRelayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
