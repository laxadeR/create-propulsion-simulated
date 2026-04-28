package dev.propulsionteam.propulsionsimulated.ponder;

import javax.annotation.Nonnull;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class DeltaPonderPlugin implements PonderPlugin {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        final PonderSceneRegistrationHelper<Block> HELPER = helper.withKeyFunction(BuiltInRegistries.BLOCK::getKey);
        //Burners
        HELPER.forComponents(PropulsionBlocks.SOLID_BURNER.get()).addStoryBoard("solid_burner", BurnerScenes::solidBurner);
        HELPER.forComponents(PropulsionBlocks.LIQUID_BURNER.get()).addStoryBoard("liquid_burner", BurnerScenes::liquidBurner);
        //Stirling engine
        HELPER.forComponents(PropulsionBlocks.STIRLING_ENGINE_BLOCK.get())
                .addStoryBoard("stirling_engine_solid", StirlingEngineScene::stirlingEngine)
                .addStoryBoard("stirling_engine_liquid", StirlingEngineScene::stirlingEngineLiquid);
        //Transmission
        HELPER.forComponents(PropulsionBlocks.REDSTONE_TRANSMISSION_BLOCK.get())
                .addStoryBoard("redstone_transmission", TransmissionScenes::directControl)
                .addStoryBoard("redstone_transmission", TransmissionScenes::incrementalControl);
        //Tilt adapter
        HELPER.forComponents(PropulsionBlocks.TILT_ADAPTER_BLOCK.get())
                .addStoryBoard("tilt_adapter", TiltAdapterScenes::redstoneControl);
    }

    @Override
	public String getModId() {
		return CreatePropulsion.ID;
	}

	@Override
	public void registerScenes(@Nonnull PonderSceneRegistrationHelper<ResourceLocation> helper) {
		register(helper);
	}

    @Override
    public void registerTags(@Nonnull PonderTagRegistrationHelper<ResourceLocation> helper) {
        final PonderTagRegistrationHelper<Block> HELPER = helper.withKeyFunction(BuiltInRegistries.BLOCK::getKey);
        HELPER.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
            .add(PropulsionBlocks.STIRLING_ENGINE_BLOCK.get())
            .add(PropulsionBlocks.TILT_ADAPTER_BLOCK.get());
    }
}