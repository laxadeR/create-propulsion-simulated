package dev.propulsionteam.propulsionsimulated.utility.tooltips;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import dev.propulsionteam.propulsionsimulated.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public class TooltipModifiers {
    private static final HashMap<Item, Function<SummaryPayload, String>> tooltipModificationLookup = new HashMap<Item, Function<SummaryPayload, String>>();
    private static final HashMap<Item, String> summaryKeyLookup = new HashMap<>();
    private static final HashMap<Item, String> conditionKeyLookup = new HashMap<>();

    static {
        summaryKeyLookup.put(PropulsionBlocks.THRUSTER_BLOCK.get().asItem(), "createpropulsion.tooltip.shared.thruster_summary");
        summaryKeyLookup.put(PropulsionBlocks.CREATIVE_THRUSTER_BLOCK.get().asItem(), "createpropulsion.tooltip.shared.thruster_summary");

        //Thruster
        tooltipModificationLookup.put(PropulsionBlocks.THRUSTER_BLOCK.get().asItem(), (payload) -> {
            float thrustMultiplier = PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get().floatValue();
            int thrusterStrength = Math.round(ThrusterBlockEntity.BASE_MAX_THRUST / 1000.0f * thrustMultiplier);
            return Component.translatable(getSummaryKey(payload.item(), payload.path() + ".tooltip.summary")).getString().replace("{}", String.valueOf(thrusterStrength));
        });
        //Creative thruster
        tooltipModificationLookup.put(PropulsionBlocks.CREATIVE_THRUSTER_BLOCK.get().asItem(), (payload) -> {
            float thrustMultiplier = PropulsionConfig.CREATIVE_THRUSTER_THRUST_MULTIPLIER.get().floatValue();
            int thrusterStrength = Math.round(1000 * thrustMultiplier);
            return Component.translatable(getSummaryKey(payload.item(), payload.path() + ".tooltip.summary")).getString().replace("{}", String.valueOf(thrusterStrength));
        });
    }

    public static String getSummaryKey(Item item, String defaultKey) {
        return summaryKeyLookup.getOrDefault(item, defaultKey);
    }

    public static String getCondition1Key(Item item, String defaultKey) {
        return conditionKeyLookup.getOrDefault(item, defaultKey);
    }

    public static boolean apply(Item item, List<Component> tooltipList) {
        Function<SummaryPayload, String> summarySupplier = tooltipModificationLookup.get(item);
        if (summarySupplier != null) {
            String path = CreatePropulsion.ID + "." + BuiltInRegistries.ITEM.getKey(item).getPath();
            String summary = summarySupplier.apply(new SummaryPayload(item, path));
            tooltipList.addAll(TooltipHelper.cutStringTextComponent(summary, Palette.STANDARD_CREATE));
            return true;
        }
        return false;
    }

    private record SummaryPayload (Item item, String path) {};
}
