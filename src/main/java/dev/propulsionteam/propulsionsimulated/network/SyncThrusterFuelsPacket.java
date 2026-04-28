package dev.propulsionteam.propulsionsimulated.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.propulsionteam.propulsionsimulated.thruster.FluidThrusterProperties;
import dev.propulsionteam.propulsionsimulated.thruster.ThrusterFuelManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

public class SyncThrusterFuelsPacket {
    private final Map<ResourceLocation, FluidThrusterProperties> fuelMap;
    private final Set<ResourceLocation> removedFuelIds;

    public static SyncThrusterFuelsPacket create(Map<Fluid, FluidThrusterProperties> mapToSync, Set<ResourceLocation> removedFuelIds) {
        Map<ResourceLocation, FluidThrusterProperties> networkSafeMap = new HashMap<>();
        mapToSync.forEach((fluid, props) -> {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key != null) {
                networkSafeMap.put(key, props);
            }
        });
        return new SyncThrusterFuelsPacket(networkSafeMap, removedFuelIds);
    }

    private SyncThrusterFuelsPacket(Map<ResourceLocation, FluidThrusterProperties> fuelMap, Set<ResourceLocation> removedFuelIds) {
        this.fuelMap = fuelMap;
        this.removedFuelIds = removedFuelIds;
    }

    public static SyncThrusterFuelsPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, FluidThrusterProperties> map = buf.readMap(FriendlyByteBuf::readResourceLocation, FluidThrusterProperties::decode);
        Set<ResourceLocation> removedFuelIds = buf.readCollection(java.util.HashSet::new, FriendlyByteBuf::readResourceLocation);
        return new SyncThrusterFuelsPacket(map, removedFuelIds);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(this.fuelMap, FriendlyByteBuf::writeResourceLocation, (b, props) -> props.encode(b));
        buf.writeCollection(this.removedFuelIds, FriendlyByteBuf::writeResourceLocation);
    }

    public void handle() {
        ThrusterFuelManager.updateClient(this.fuelMap, this.removedFuelIds);
    }
}
