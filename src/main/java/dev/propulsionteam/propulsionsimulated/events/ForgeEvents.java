package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.network.PropulsionPackets;
import dev.propulsionteam.propulsionsimulated.network.SyncThrusterFuelsPacket;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionCommands;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import dev.propulsionteam.propulsionsimulated.thruster.ThrusterFuelManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.GAME)
public class ForgeEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ThrusterFuelManager());
    }

    //Sync thruster fuels for goggles & particles on client side
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)  {
            PropulsionPackets.sendToPlayer(SyncThrusterFuelsPacket.create(ThrusterFuelManager.getFuelPropertiesMap(), ThrusterFuelManager.getRemovedFuelIds()), player);
        }
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        PropulsionCommands.register(event.getDispatcher());
    }

    //Turpentine-lava interaction
    @SubscribeEvent
    public static void onNeighborBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getFluidState().isEmpty()) {
            return;
        }

        boolean isTurpentine = state.getFluidState().is(PropulsionFluids.TURPENTINE.get());
        boolean isLava = state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA);

        if (!isTurpentine && !isLava) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluid = level.getFluidState(neighborPos);

            if (isTurpentine && (neighborFluid.is(Fluids.LAVA) || neighborFluid.is(Fluids.FLOWING_LAVA))) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                return;
            }

            if (isLava && neighborFluid.is(PropulsionFluids.TURPENTINE.get())) {
                level.setBlock(neighborPos, Blocks.STONE.defaultBlockState(), 3);
                return;
            }
        }
    }

}
