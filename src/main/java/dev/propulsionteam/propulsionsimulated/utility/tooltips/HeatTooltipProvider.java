package dev.propulsionteam.propulsionsimulated.utility.tooltips;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class HeatTooltipProvider implements ITooltipProvider {
    private enum HeatAmount {LOW, MODERATE, HIGH}
    private static Map<Integer, HeatInfo> idsToTooltips;

    private static void registerHeatTooltip(Supplier<? extends net.minecraft.world.level.block.Block> blockEntry, HeatInfo info) {
        Item item = blockEntry.get().asItem();
        idsToTooltips.put(Item.getId(item), info);
    }

    private static void populateTooltips() {
        registerHeatTooltip(PropulsionBlocks.SOLID_BURNER, new HeatInfo(true, HeatAmount.MODERATE));
        registerHeatTooltip(PropulsionBlocks.LIQUID_BURNER, new HeatInfo(true, HeatAmount.HIGH));
        registerHeatTooltip(PropulsionBlocks.STIRLING_ENGINE_BLOCK, new HeatInfo(false, HeatAmount.MODERATE));
    }

    @Override
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList) {
        //Resolve map
        if (idsToTooltips == null) {
            idsToTooltips = new HashMap<>();
            populateTooltips();
        }
        Map<Integer, HeatInfo> tooltipMap = idsToTooltips;

        ItemStack stack = event.getItemStack();
        if (!tooltipMap.containsKey(Item.getId(stack.getItem()))) return;

        HeatInfo info = tooltipMap.get(Item.getId(stack.getItem()));
        Component heatEffectLine = Component.translatable(info.getEffectLineKey()).append(Component.literal(":")).withStyle(ChatFormatting.GRAY);
        Component heatLine = Component.literal(TooltipHelper.makeProgressBar(3, info.getHeatAmountOrdinal() + 1))
            .append(Component.literal(" "))
            .append(CreateLang.translate(info.getHeatAmountKey()).component())
            .withStyle(info.getHeatAmountColor());

        tooltipList.add(Component.empty());
        tooltipList.add(heatEffectLine);
        tooltipList.add(heatLine);
    }

    private record HeatInfo(boolean isSource, HeatAmount heatAmount) {
        public ChatFormatting getHeatAmountColor() {
            return switch (heatAmount()) {
                case LOW -> ChatFormatting.GREEN;
                case MODERATE -> ChatFormatting.GOLD;
                case HIGH -> ChatFormatting.RED;
            };
        }

        public int getHeatAmountOrdinal() {
            return switch (heatAmount()) {
                case LOW -> 0;
                case MODERATE -> 1;
                case HIGH -> 2;
            };
        }

        public String getHeatAmountKey() {
            return switch (heatAmount()) {
                case LOW -> "tooltip.stressImpact.low";
                case MODERATE -> "tooltip.stressImpact.medium";
                case HIGH -> "tooltip.stressImpact.high";
            };
        }

        public String getEffectLineKey() {
            if (isSource) {
                return "createpropulsion.tooltip.heat.generation";
            } else {
                return "createpropulsion.tooltip.heat.consumption";
            }
        }
    }
}
