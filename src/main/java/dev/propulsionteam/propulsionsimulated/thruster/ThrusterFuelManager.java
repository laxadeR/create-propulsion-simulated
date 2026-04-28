package dev.propulsionteam.propulsionsimulated.thruster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
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

    private static Map<Fluid, FluidThrusterProperties> fuelPropertiesMap = new HashMap<>();
    private static Map<Fluid, FluidThrusterProperties> scriptedFuelPropertiesMap = new HashMap<>();
    private static Map<ResourceLocation, FluidThrusterProperties> scriptedFuelPropertiesById = new HashMap<>();
    private static Set<ResourceLocation> removedFuelIds = new HashSet<>();
    public static final TagKey<Fluid> FORGE_FUEL_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("forge", "fuel"));

    public static Map<Fluid, FluidThrusterProperties> getFuelPropertiesMap() {
        Map<Fluid, FluidThrusterProperties> merged = new HashMap<>(fuelPropertiesMap);
        merged.putAll(scriptedFuelPropertiesMap);
        for (ResourceLocation removedFuelId : removedFuelIds) {
            Fluid removedFluid = BuiltInRegistries.FLUID.get(removedFuelId);
            if (removedFluid != null && removedFluid != Fluids.EMPTY) {
                merged.remove(removedFluid);
            }
        }
        return merged;
    }

    public static Set<ResourceLocation> getRemovedFuelIds() {
        return Set.copyOf(removedFuelIds);
    }

    public ThrusterFuelManager() {
        super(GSON, DIRECTORY);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public static FluidThrusterProperties getProperties(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        fluid = FluidHelper.convertToStill(fluid);
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId != null && removedFuelIds.contains(fluidId)) {
            return null;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return FluidThrusterProperties.DEFAULT;
        }
        FluidThrusterProperties props = scriptedFuelPropertiesMap.get(fluid);
        if (props != null) {
            return props;
        }
        props = fuelPropertiesMap.get(fluid);
        if (props != null) {
            return props;
        }
        if (fluidId != null) {
            props = scriptedFuelPropertiesById.get(fluidId);
            if (props != null) {
                return props;
            }
        }
        if (fluid.is(FORGE_FUEL_TAG)) return FluidThrusterProperties.DEFAULT;
        return null;
    }

    public static float getEfficiency(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return PropulsionConfig.FUEL_DEFAULT_EFFICIENCY.get().floatValue();
        }

        // Normalize flowing variants (e.g. flowing_lava) to their source fluid ids.
        fluid = FluidHelper.convertToStill(fluid);

        Map<ResourceLocation, Float> fluidEfficiencyOverrides = getConfiguredEfficiencyOverrides();

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId == null) {
            return PropulsionConfig.FUEL_DEFAULT_EFFICIENCY.get().floatValue();
        }

        return fluidEfficiencyOverrides.getOrDefault(fluidId, PropulsionConfig.FUEL_DEFAULT_EFFICIENCY.get().floatValue());
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> pObject, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        //Parse datapacks
        profiler.push(CreatePropulsion.ID + ":Loading_thruster_fuels");
        fuelPropertiesMap = parseFuelProperties(pObject);
        profiler.pop();
        //Update clients (happens only on /reload as on server start server instance is still null)
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            PropulsionPackets.sendToAll(SyncThrusterFuelsPacket.create(getFuelPropertiesMap(), getRemovedFuelIds()));
        }
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
        removedFuelIds = new HashSet<>(removedFuelIdsFromServer);
    }

    public static void clearScriptedFuels() {
        scriptedFuelPropertiesMap.clear();
        scriptedFuelPropertiesById.clear();
        removedFuelIds.clear();
        syncFuelDataToClients();
    }

    public static boolean registerScriptedFuel(String fluidId, Map<String, Object> settings) {
        if (settings == null) {
            LOGGER.warn("[{}] KubeJS fuel registration failed: settings object is null for '{}'.", CreatePropulsion.ID, fluidId);
            return false;
        }

        float thrustMultiplier = getFloatSetting(settings, "thrustMultiplier", "thrust_multiplier", 1.0f);
        float consumptionMultiplier = getFloatSetting(settings, "consumptionMultiplier", "consumption_multiplier", 1.0f);
        String particleName = getStringSetting(settings, "particle", "particle", "plume");
        List<String> overrideTextureIds = getStringListSetting(settings, "overrideTextures", "override_textures");
        Integer overrideColor = getColorSetting(settings, "overrideColor", "override_color");
        boolean useFluidColor = getBooleanSetting(settings, "useFluidColor", "use_fluid_color", false);

        ResourceLocation fluidLocation = ResourceLocation.tryParse(fluidId);
        if (fluidLocation == null) {
            LOGGER.warn("[{}] KubeJS fuel registration failed: invalid fluid id '{}'.", CreatePropulsion.ID, fluidId);
            return false;
        }
        return registerScriptedFuelInternal(
            fluidLocation,
            thrustMultiplier,
            consumptionMultiplier,
            particleName,
            overrideTextureIds,
            overrideColor,
            useFluidColor
        );
    }

    public static boolean overrideFuel(String fluidIdToOverride, Map<String, Object> settings) {
        return registerScriptedFuel(fluidIdToOverride, settings);
    }

    public static boolean removeFuel(String fuelIdToRemove) {
        ResourceLocation fluidLocation = ResourceLocation.tryParse(fuelIdToRemove);
        if (fluidLocation == null) {
            LOGGER.warn("[{}] KubeJS fuel removal failed: invalid fluid id '{}'.", CreatePropulsion.ID, fuelIdToRemove);
            return false;
        }
        removedFuelIds.add(fluidLocation);
        Fluid removedFluid = BuiltInRegistries.FLUID.get(fluidLocation);
        if (removedFluid != null && removedFluid != Fluids.EMPTY) {
            scriptedFuelPropertiesMap.remove(removedFluid);
        }
        scriptedFuelPropertiesById.remove(fluidLocation);
        syncFuelDataToClients();
        return true;
    }

    private Map<Fluid, FluidThrusterProperties> parseFuelProperties(@Nonnull Map<ResourceLocation, JsonElement> pObject) {
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
                    //Successfully load fuel
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

    private static Map<ResourceLocation, Float> getConfiguredEfficiencyOverrides() {
        Map<ResourceLocation, Float> overrides = new HashMap<>();
        putEfficiencyOverride(overrides, "minecraft:lava", PropulsionConfig.FUEL_EFFICIENCY_LAVA.get());
        putEfficiencyOverride(overrides, "createpropulsion:turpentine", PropulsionConfig.FUEL_EFFICIENCY_TURPENTINE.get());
        putEfficiencyOverride(overrides, "createdieselgenerators:diesel", PropulsionConfig.FUEL_EFFICIENCY_CDG_DIESEL.get());
        putEfficiencyOverride(overrides, "createdieselgenerators:gasoline", PropulsionConfig.FUEL_EFFICIENCY_CDG_GASOLINE.get());
        putEfficiencyOverride(overrides, "createdieselgenerators:ethanol", PropulsionConfig.FUEL_EFFICIENCY_CDG_ETHANOL.get());
        putEfficiencyOverride(overrides, "createdieselgenerators:biodiesel", PropulsionConfig.FUEL_EFFICIENCY_CDG_BIODIESEL.get());
        putEfficiencyOverride(overrides, "createdieselgenerators:plant_oil", PropulsionConfig.FUEL_EFFICIENCY_CDG_PLANT_OIL.get());
        putEfficiencyOverride(overrides, "tfmg:diesel", PropulsionConfig.FUEL_EFFICIENCY_TFMG_DIESEL.get());
        putEfficiencyOverride(overrides, "tfmg:gasoline", PropulsionConfig.FUEL_EFFICIENCY_TFMG_GASOLINE.get());
        putEfficiencyOverride(overrides, "tfmg:kerosene", PropulsionConfig.FUEL_EFFICIENCY_TFMG_KEROSENE.get());
        putEfficiencyOverride(overrides, "tfmg:naphtha", PropulsionConfig.FUEL_EFFICIENCY_TFMG_NAPHTHA.get());
        putEfficiencyOverride(overrides, "immersiveengineering:biodiesel", PropulsionConfig.FUEL_EFFICIENCY_IE_BIODIESEL.get());
        putEfficiencyOverride(overrides, "immersiveengineering:ethanol", PropulsionConfig.FUEL_EFFICIENCY_IE_ETHANOL.get());
        putEfficiencyOverride(overrides, "immersiveengineering:plant_oil", PropulsionConfig.FUEL_EFFICIENCY_IE_PLANT_OIL.get());
        putEfficiencyOverride(overrides, "immersivepetroleum:diesel", PropulsionConfig.FUEL_EFFICIENCY_IP_DIESEL.get());
        putEfficiencyOverride(overrides, "immersivepetroleum:diesel_sulfur", PropulsionConfig.FUEL_EFFICIENCY_IP_DIESEL_SULFUR.get());
        putEfficiencyOverride(overrides, "immersivepetroleum:gasoline", PropulsionConfig.FUEL_EFFICIENCY_IP_GASOLINE.get());
        putEfficiencyOverride(overrides, "mekanism:hydrogen", PropulsionConfig.FUEL_EFFICIENCY_MEKANISM_HYDROGEN.get());
        putEfficiencyOverride(overrides, "mekanismgenerators:bioethanol", PropulsionConfig.FUEL_EFFICIENCY_MEKANISM_GENERATORS_BIOETHANOL.get());
        putEfficiencyOverride(overrides, "northstar:biofuel", PropulsionConfig.FUEL_EFFICIENCY_NORTHSTAR_BIOFUEL.get());
        putEfficiencyOverride(overrides, "northstar:hydrocarbon", PropulsionConfig.FUEL_EFFICIENCY_NORTHSTAR_HYDROCARBON.get());
        putEfficiencyOverride(overrides, "northstar:methane", PropulsionConfig.FUEL_EFFICIENCY_NORTHSTAR_METHANE.get());
        putEfficiencyOverride(overrides, "northstar:liquid_hydrogen", PropulsionConfig.FUEL_EFFICIENCY_NORTHSTAR_LIQUID_HYDROGEN.get());
        putEfficiencyOverride(overrides, "stellaris:fuel", PropulsionConfig.FUEL_EFFICIENCY_STELLARIS_FUEL.get());
        putEfficiencyOverride(overrides, "stellaris:diesel", PropulsionConfig.FUEL_EFFICIENCY_STELLARIS_DIESEL.get());
        return overrides;
    }

    private static void putEfficiencyOverride(Map<ResourceLocation, Float> map, String fluidId, double value) {
        ResourceLocation rl = ResourceLocation.tryParse(fluidId);
        if (rl == null) {
            LOGGER.warn("[{}] Invalid fluid id '{}' in hardcoded config mapping.", CreatePropulsion.ID, fluidId);
            return;
        }
        map.put(rl, (float) value);
    }

    private static void syncFuelDataToClients() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            PropulsionPackets.sendToAll(SyncThrusterFuelsPacket.create(getFuelPropertiesMap(), getRemovedFuelIds()));
        }
    }

    private static Integer sanitizeColor(Integer color) {
        if (color == null) {
            return null;
        }
        return color & 0xFFFFFF;
    }

    private static List<ResourceLocation> parseTextureOverrides(List<String> overrideTextureIds) {
        if (overrideTextureIds == null || overrideTextureIds.isEmpty()) {
            return List.of();
        }
        return overrideTextureIds.stream()
            .map(ResourceLocation::tryParse)
            .filter(rl -> rl != null)
            .toList();
    }

    private static float getFloatSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey, float fallback) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String getStringSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey, String fallback) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return fallback;
        }
        return value.toString();
    }

    private static List<String> getStringListSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private static Integer getColorSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return sanitizeColor(number.intValue());
        }
        String str = value.toString().trim().toLowerCase(Locale.ROOT);
        if (str.startsWith("#")) {
            str = str.substring(1);
        } else if (str.startsWith("0x")) {
            str = str.substring(2);
        }
        try {
            return sanitizeColor(Integer.parseInt(str, 16));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean getBooleanSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey, boolean fallback) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("deprecation")
    private static boolean registerScriptedFuelInternal(ResourceLocation fluidId, float thrustMultiplier, float consumptionMultiplier, String particleName, List<String> overrideTextureIds, Integer overrideColor, boolean useFluidColor) {
        Fluid fluid = FluidHelper.convertToStill(BuiltInRegistries.FLUID.get(fluidId));
        if (fluid == null || fluid == Fluids.EMPTY) {
            LOGGER.warn("[{}] KubeJS fuel registration failed: fluid '{}' is not registered.", CreatePropulsion.ID, fluidId);
            return false;
        }
        ThrusterParticleType particleType = ThrusterParticleType.fromString(particleName);
        List<ResourceLocation> textureOverrides = parseTextureOverrides(overrideTextureIds);
        FluidThrusterProperties properties = new FluidThrusterProperties(thrustMultiplier, consumptionMultiplier, particleType, textureOverrides, sanitizeColor(overrideColor), useFluidColor);
        scriptedFuelPropertiesMap.put(fluid, properties);
        scriptedFuelPropertiesById.put(fluidId, properties);
        removedFuelIds.remove(fluidId);
        syncFuelDataToClients();
        return true;
    }
}
