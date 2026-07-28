package dev.propulsionteam.propulsionsimulated.content.thruster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.network.PropulsionPackets;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import dev.propulsionteam.propulsionsimulated.network.SyncThrusterFuelsPacket;

public class ThrusterFuelManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "thruster_fuels";
    private static final Set<ResourceLocation> EMPTY_SET = new HashSet<>();

    private static Map<Fluid, FluidThrusterProperties> fuelPropertiesMap = new HashMap<>();

    /** Last merged datapack JSON for {@link #rebuildThrusterFuelsAfterCommonConfigReload()}. */
    private static Map<ResourceLocation, JsonElement> cachedThrusterFuelDatapack = null;

    public static Map<Fluid, FluidThrusterProperties> getFuelPropertiesMap() {
        Map<Fluid, FluidThrusterProperties> merged = new HashMap<>(fuelPropertiesMap);
        return merged;
    }

    public ThrusterFuelManager() {
        super(GSON, DIRECTORY);
    }

    @Nullable
    public static FluidThrusterProperties getProperties(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;

        fluid = FluidHelper.convertToStill(fluid);

        return fuelPropertiesMap.get(fluid);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> pObject, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        //Parse datapacks
        profiler.push(CreatePropulsion.ID + ":Loading_thruster_fuels");
        cachedThrusterFuelDatapack = new HashMap<>(pObject);
        fuelPropertiesMap = parseFuelProperties(cachedThrusterFuelDatapack);

        logReloadSummary("datapack_reload");
        profiler.pop();

        syncFuelDataToClients();
    }

    public static void updateClient(Map<ResourceLocation, FluidThrusterProperties> fuelMap, Set<ResourceLocation> removedFuelIdsFromServer) {
        Map<Fluid, FluidThrusterProperties> newClientMap = new HashMap<>();
        fuelMap.forEach((rl, props) -> {
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);
            if (fluid != null) {
                newClientMap.put(fluid, props);
            }
        });
        fuelPropertiesMap = newClientMap;
    }

    /**
     * Re-applies datapack thruster fuels using current common config (efficiency / burn rate / additional lines).
     * Called when {@code createpropulsion-common.toml} reloads without a full datapack reload.
     */
    public static void rebuildThrusterFuelsAfterCommonConfigReload() {
        if (cachedThrusterFuelDatapack == null) {
            return;
        }

        fuelPropertiesMap = parseFuelProperties(cachedThrusterFuelDatapack);

        logReloadSummary("common_config_reload");
        syncFuelDataToClients();
    }

    private static Map<Fluid, FluidThrusterProperties> parseFuelProperties(@Nonnull Map<ResourceLocation, JsonElement> pObject) {
        Map<Fluid, FluidThrusterProperties> newMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            //Parse fuel def
            ThrusterFuelDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> {LOGGER.error("[{}] Failed to parse thruster fuel definition from {}: {}", CreatePropulsion.ID, file, error);})
                .ifPresent(definition -> {
                    //There is a fuel that requires a mod but the mod is not present
                    if (definition.requiredMod().isPresent() && !ModList.get().isLoaded(definition.requiredMod().get())) {
                        return;
                    }
                    Fluid fluid = definition.getFluid();
                    //Fluid is not in registry
                    if (fluid == Fluids.EMPTY) {
                        return;
                    }

                    FluidThrusterProperties properties = new FluidThrusterProperties(
                        definition.thrustMultiplier(), 
                        definition.consumptionMultiplier(),
                        definition.particle(),
                        definition.overrideTextures(),
                        definition.overrideColor().map(ThrusterFuelManager::sanitizeColor).orElse(null),
                        definition.useFluidColor());

                    newMap.put(fluid, properties);
                });
        }

        return newMap;
    }

    private static void logReloadSummary(String context) {
        int totalEntries = fuelPropertiesMap.size();

        LOGGER.info(
            "[{}] Thruster fuel reload ({}) complete: totalEntries={}",
            CreatePropulsion.ID,
            context,
            totalEntries
        );
    }

    private static void syncFuelDataToClients() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            PropulsionPackets.sendToAll(SyncThrusterFuelsPacket.create(getFuelPropertiesMap(), EMPTY_SET));
        }
    }

    private static Integer sanitizeColor(Integer color) {
        if (color == null) {
            return null;
        }
        return color & 0xFFFFFF;
    }
}
