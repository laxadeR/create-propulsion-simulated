package dev.propulsionteam.propulsionsimulated.registries;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class PropulsionFluids {
    public static void register() {} //Loads this class

    public static final DummyFluidEntry TURPENTINE = new DummyFluidEntry();

    public static class DummyFluidEntry {
        public Fluid get() {
            return Fluids.WATER;
        }

        public Supplier<Item> getBucket() {
            return () -> Items.WATER_BUCKET;
        }
    }
}
