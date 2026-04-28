package dev.propulsionteam.propulsionsimulated.utility.tooltips;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public interface ITooltipProvider {
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList);
}
