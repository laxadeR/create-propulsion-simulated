package dev.propulsionteam.propulsionsimulated.mixin;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.heat.burners.AbstractBurnerBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasinBlockEntity.class)
public class BasinBlockEntityMixin {
    @Inject(method = "getHeatLevelOf", at = @At("RETURN"), cancellable = true)
    private static void createpropulsion$checkCustomBurners(BlockState state, CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> cir) {
        if (cir.getReturnValue() == BlazeBurnerBlock.HeatLevel.NONE 
                && state.getBlock() instanceof AbstractBurnerBlock
                && PropulsionConfig.BURNERS_POWER_HEATED_MIXERS.get()) {
            cir.setReturnValue(state.getValue(AbstractBurnerBlock.HEAT));
        }
    }
}
