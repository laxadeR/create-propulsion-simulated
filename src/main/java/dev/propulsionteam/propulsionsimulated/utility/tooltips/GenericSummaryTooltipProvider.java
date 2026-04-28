package dev.propulsionteam.propulsionsimulated.utility.tooltips;

import java.util.List;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class GenericSummaryTooltipProvider implements ITooltipProvider {

    @Override
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList) {
        Item item = event.getItemStack().getItem();
        if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(CreatePropulsion.ID)) {
            return;
        }

        String path = CreatePropulsion.ID + "." + BuiltInRegistries.ITEM.getKey(item).getPath();
        String summaryKey = TooltipModifiers.getSummaryKey(item, path + ".tooltip.summary");

        if (!I18n.exists(summaryKey)) {
            return;
        }

        TooltipHandler.wrapShiftHoldText(tooltipList, "create.tooltip.holdForDescription", () -> {
            if (!TooltipModifiers.apply(item, tooltipList)) {
                tooltipList.addAll(TooltipHelper.cutStringTextComponent(
                    Component.translatable(summaryKey).getString(), Palette.STANDARD_CREATE));
            }

            String condition1Key = TooltipModifiers.getCondition1Key(item, path + ".tooltip.condition1");

            if (I18n.exists(condition1Key)) {
                tooltipList.add(Component.empty());
                tooltipList.add(Component.translatable(condition1Key).withStyle(ChatFormatting.GRAY));
                tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.behaviour1").getString(), Palette.STANDARD_CREATE.primary(), Palette.STANDARD_CREATE.highlight(), 1));
                if (I18n.exists(path + ".tooltip.condition2")) {
                    tooltipList.add(Component.translatable(path + ".tooltip.condition2").withStyle(ChatFormatting.GRAY));
                    tooltipList.addAll(TooltipHelper.cutStringTextComponent(Component.translatable(path + ".tooltip.behaviour2").getString(), Palette.STANDARD_CREATE.primary(), Palette.STANDARD_CREATE.highlight(), 1));
                }
            }
        });

    }
}
