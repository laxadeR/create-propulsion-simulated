package dev.propulsionteam.propulsionsimulated.thruster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

public class FluidThrusterProperties {
    public float thrustMultiplier;
    public float consumptionMultiplier;
    public ThrusterParticleType particleType;
    public List<ResourceLocation> overrideTextures;
    public Integer overrideColor;
    public boolean useFluidColor;
    
    public static final FluidThrusterProperties DEFAULT = new FluidThrusterProperties(1, 1, ThrusterParticleType.PLUME, Collections.emptyList(), null, false);

    public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier) {
        this(thrustMultiplier, consumptionMultiplier, ThrusterParticleType.PLUME, Collections.emptyList(), null, false);
    }

    public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier, ThrusterParticleType particleType) {
        this(thrustMultiplier, consumptionMultiplier, particleType, Collections.emptyList(), null, false);
    }

    public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier, ThrusterParticleType particleType, List<ResourceLocation> overrideTextures, Integer overrideColor) {
        this(thrustMultiplier, consumptionMultiplier, particleType, overrideTextures, overrideColor, false);
    }

    public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier, ThrusterParticleType particleType, List<ResourceLocation> overrideTextures, Integer overrideColor, boolean useFluidColor) {
        this.thrustMultiplier = thrustMultiplier;
        this.consumptionMultiplier = consumptionMultiplier;
        this.particleType = particleType;
        this.overrideTextures = overrideTextures == null ? Collections.emptyList() : List.copyOf(overrideTextures);
        this.overrideColor = overrideColor;
        this.useFluidColor = useFluidColor;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.thrustMultiplier);
        buf.writeFloat(this.consumptionMultiplier);
        buf.writeEnum(this.particleType);
        buf.writeCollection(this.overrideTextures, FriendlyByteBuf::writeResourceLocation);
        buf.writeBoolean(this.overrideColor != null);
        if (this.overrideColor != null) {
            buf.writeInt(this.overrideColor);
        }
        buf.writeBoolean(this.useFluidColor);
    }

    public static FluidThrusterProperties decode(FriendlyByteBuf buf) {
        float thrustMultiplier = buf.readFloat();
        float consumptionMultiplier = buf.readFloat();
        ThrusterParticleType particleType = buf.readEnum(ThrusterParticleType.class);
        List<ResourceLocation> overrideTextures = buf.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation);
        Integer overrideColor = buf.readBoolean() ? buf.readInt() : null;
        boolean useFluidColor = buf.readBoolean();
        return new FluidThrusterProperties(thrustMultiplier, consumptionMultiplier, particleType, overrideTextures, overrideColor, useFluidColor);
    }
}