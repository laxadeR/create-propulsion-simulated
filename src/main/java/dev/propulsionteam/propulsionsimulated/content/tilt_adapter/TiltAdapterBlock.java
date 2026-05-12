package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionShapes;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TiltAdapterBlock extends AbstractEncasedShaftBlock implements IBE<TiltAdapterBlockEntity> {
    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
    public static final BooleanProperty ALIGNED_X = BooleanProperty.create("aligned_x");
    public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;

    public TiltAdapterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(DirectionalKineticBlock.FACING, Direction.UP)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, false)
            .setValue(POSITIVE, true)
            .setValue(ALIGNED_X, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Player player = context.getPlayer();
        Direction placeDirection;

        if (player != null && !player.isShiftKeyDown()) {
            placeDirection = baseDirection.getOpposite();
        } else {
            placeDirection = baseDirection;
        }

        Direction.Axis axis = placeDirection.getAxis();
        boolean alignedX = axis == Direction.Axis.Y && context.getHorizontalDirection().getAxis() == Direction.Axis.X;
        return fromFacingAndAlignment(defaultBlockState(), placeDirection, alignedX);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot.ordinal() % 2 == 1) {
            state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
        }
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        return fromFacingAndAlignment(state, rot.rotate(facing), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return rotate(state, mirrorIn.getRotation(state.getValue(DirectionalKineticBlock.FACING)));
    }

    private static BlockState fromFacingAndAlignment(BlockState state, Direction facing, boolean axisAlongFirst) {
        Direction.Axis axis = facing.getAxis();
        boolean isPositive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean alignedX = false;
        if (axis == Direction.Axis.Y) {
            alignedX = axisAlongFirst;
        }

        return state
            .setValue(AXIS, axis)
            .setValue(DirectionalKineticBlock.FACING, facing)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, axisAlongFirst)
            .setValue(POSITIVE, isPositive)
            .setValue(ALIGNED_X, alignedX);
    }

    public static boolean isAxisAlongFirst(BlockState state) {
        if (state.hasProperty(AXIS_ALONG_FIRST_COORDINATE)) {
            return state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        }
        return state.hasProperty(ALIGNED_X) && state.getValue(ALIGNED_X);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        Direction direction = getDirection(pState);
        if (direction.getAxis() == Axis.Y) direction = direction.getOpposite();
        return PropulsionShapes.TILT_ADAPTER.get(direction); 
    }

    public static Direction getDirection(BlockState state) {
        if (state.hasProperty(DirectionalKineticBlock.FACING)) {
            return state.getValue(DirectionalKineticBlock.FACING);
        }
        Direction.Axis axis = state.getValue(AXIS);
        boolean isPositive = state.getValue(POSITIVE);
        
        switch (axis) {
            case X:
                return isPositive ? Direction.EAST : Direction.WEST;
            case Y:
                return isPositive ? Direction.UP : Direction.DOWN;
            case Z:
                return isPositive ? Direction.SOUTH : Direction.NORTH;
            default:
                return Direction.UP;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AXIS, DirectionalKineticBlock.FACING, AXIS_ALONG_FIRST_COORDINATE, POSITIVE, ALIGNED_X);
    }

    @Override
    public Class<TiltAdapterBlockEntity> getBlockEntityClass() {
        return TiltAdapterBlockEntity.class;
    }

    @Override
    public BlockEntityType<TiltAdapterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TiltAdapterBlockEntity(PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }
}
