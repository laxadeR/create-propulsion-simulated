package dev.propulsionteam.propulsionsimulated.content.cable.relay;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CableRelayBlockEntity extends SmartBlockEntity {
    public CableRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
}
