package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.heat.burners.liquid.LiquidBurnerRenderer;
import dev.propulsionteam.propulsionsimulated.heat.burners.liquid.LiquidBurnerVisual;
import dev.propulsionteam.propulsionsimulated.heat.engine.StirlingEngineRenderer;
import dev.propulsionteam.propulsionsimulated.heat.engine.StirlingEngineVisual;
import dev.propulsionteam.propulsionsimulated.ponder.DeltaPonderPlugin;
import dev.propulsionteam.propulsionsimulated.redstone_transmission.RedstoneTransmissionRenderer;
import dev.propulsionteam.propulsionsimulated.redstone_transmission.RedstoneTransmissionVisual;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionInstanceTypes;
import dev.propulsionteam.propulsionsimulated.tilt_adapter.TiltAdapterRenderer;
import dev.propulsionteam.propulsionsimulated.thruster.creative_thruster.CreativeThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.thruster.creative_thruster.CreativeThrusterVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Removed colorized optical lens handling.
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        // Removed assembly gauge overlay.
    }

    @SubscribeEvent
    public static void clientInit(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new DeltaPonderPlugin());
        PropulsionInstanceTypes.register();

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get())
            .factory(StirlingEngineVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get())
            .factory(RedstoneTransmissionVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get())
            .factory(CreativeThrusterVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get())
            .factory(LiquidBurnerVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get(), StirlingEngineRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get(), RedstoneTransmissionRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), CreativeThrusterRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get(), LiquidBurnerRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get(), TiltAdapterRenderer::new);
    }
}
