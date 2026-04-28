package dev.propulsionteam.propulsionsimulated;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionDefaultStress;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PropulsionConfig {
    public static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    //Thruster
    public static final ModConfigSpec.ConfigValue<Double>  THRUSTER_THRUST_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<Double>  THRUSTER_CONSUMPTION_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<Integer> THRUSTER_MAX_SPEED;
    public static final ModConfigSpec.ConfigValue<Integer> THRUSTER_TICKS_PER_UPDATE;
    public static final ModConfigSpec.ConfigValue<Boolean> THRUSTER_DAMAGE_ENTITIES;
    public static final ModConfigSpec.ConfigValue<Double>  THRUSTER_PARTICLE_OFFSET_INCOMING_VEL_MODIFIER;
    public static final ModConfigSpec.ConfigValue<Double>  THRUSTER_PARTICLE_COUNT_MULTIPLIER;
    
    //Creative Thruster
    public static final ModConfigSpec.ConfigValue<Double> CREATIVE_THRUSTER_THRUST_MULTIPLIER;
    
    //Optical sensors
    public static final ModConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_TICKS_PER_UPDATE;
    public static final ModConfigSpec.ConfigValue<Integer> INLINE_OPTICAL_SENSOR_MAX_DISTANCE;
    public static final ModConfigSpec.ConfigValue<Integer> OPTICAL_SENSOR_MAX_DISTANCE;
        
    //Wings
    public static final ModConfigSpec.ConfigValue<Double> BASE_WING_LIFT;
    public static final ModConfigSpec.ConfigValue<Double> BASE_WING_DRAG;
    
    //Stirling engine
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_GENERATED_SU;
    public static final ModConfigSpec.ConfigValue<Double> TILT_ADAPTER_ANGLE_RANGE;

    public static final ModConfigSpec.ConfigValue<Double> STIRLING_REVOLUTION_PERIOD;
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_CRANK_RADIUS;
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_CONROD_LENGTH;
    
    //Burners
    public static final ModConfigSpec.ConfigValue<Boolean> BURNERS_POWER_HEATED_MIXERS;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_DEFAULT_EFFICIENCY;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_LAVA;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_TURPENTINE;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_CDG_DIESEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_CDG_GASOLINE;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_CDG_ETHANOL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_CDG_BIODIESEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_CDG_PLANT_OIL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_TFMG_DIESEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_TFMG_GASOLINE;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_TFMG_KEROSENE;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_TFMG_NAPHTHA;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_IE_BIODIESEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_IE_ETHANOL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_IE_PLANT_OIL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_IP_DIESEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_IP_DIESEL_SULFUR;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_IP_GASOLINE;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_MEKANISM_HYDROGEN;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_MEKANISM_GENERATORS_BIOETHANOL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_NORTHSTAR_BIOFUEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_NORTHSTAR_HYDROCARBON;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_NORTHSTAR_METHANE;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_NORTHSTAR_LIQUID_HYDROGEN;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_STELLARIS_FUEL;
    public static final ModConfigSpec.ConfigValue<Double> FUEL_EFFICIENCY_STELLARIS_DIESEL;

    static {
        //#region Server
        SERVER_BUILDER.push("Thruster");
            THRUSTER_THRUST_MULTIPLIER = SERVER_BUILDER.comment("Thrust is multiplied by that.")
                .define("Thrust multiplier", 1.0);
            THRUSTER_CONSUMPTION_MULTIPLIER = SERVER_BUILDER.comment("Fuel consumption is multiplied by that.")
                .define("Fuel consumption", 1.0);
            THRUSTER_MAX_SPEED = SERVER_BUILDER.comment("Thrusters stop accelerating ships upon reaching this speed. Defined in blocks per second.")
                .defineInRange("Thruster speed limit", 100, 10, 200);
            THRUSTER_TICKS_PER_UPDATE = SERVER_BUILDER.comment("Thruster tick rate. Lower values make fluid consumption a little more precise.")
                .defineInRange("Thruster tick rate", 10, 1, 100);
            THRUSTER_DAMAGE_ENTITIES = SERVER_BUILDER.comment("If true - thrusters will damage entities. May have negative effect on performance if a lot of thrusters are used.")
                .define("Thrusters damage entities", true);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Creative Thruster");
            CREATIVE_THRUSTER_THRUST_MULTIPLIER = SERVER_BUILDER.comment("Thrust is multiplied by that.")
                .define("Creative thrust multiplier", 1.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Optical sensors");
            OPTICAL_SENSOR_TICKS_PER_UPDATE = SERVER_BUILDER.comment("How many ticks between casting a ray. Lower values are more precise, but can have negative effect on performance.")
                .defineInRange("Optical sensor tick rate", 2, 1, 100);
            INLINE_OPTICAL_SENSOR_MAX_DISTANCE = SERVER_BUILDER.comment("Length of the raycast ray.")
                .defineInRange("Inline optical sensor max raycast distance", 16, 4, 32);
            OPTICAL_SENSOR_MAX_DISTANCE = SERVER_BUILDER.comment("Length of the raycast ray. Very high values may degrade performance. Change with caution!")
                .defineInRange("Optical sensor max raycast distance", 32, 8, 64);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Wing");
            BASE_WING_LIFT = SERVER_BUILDER.comment("Wing's lift force is multiplied by this number.")
                .define("Base lift", 150.0);
            BASE_WING_DRAG = SERVER_BUILDER.comment("Wing's drag force is multiplied by this number.")
                .define("Base drag", 150.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Stirling Engine");
            STIRLING_GENERATED_SU = SERVER_BUILDER.comment("Change this value to modify the amount of stress units produced by stirling engine. Value of 16 corresponds to 4096 SU.")
                .defineInRange("Generated stress units", 16.0, 1.0, 64.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Tilt Adapter");
            TILT_ADAPTER_ANGLE_RANGE = SERVER_BUILDER.comment("Maximum absolute output angle in degrees, reached at full redstone differential.")
                .defineInRange("Maximum angle range", 45.0, 0.0, 180.0);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Burners");
            BURNERS_POWER_HEATED_MIXERS = SERVER_BUILDER.comment("If true - both solid and liquid burners can provide heat to heated mixers allowing for pre-nether brass.")
                .define("Burners power heated mixers", true);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Fuel Efficiency");
            FUEL_DEFAULT_EFFICIENCY = SERVER_BUILDER.comment("Default fluid efficiency multiplier. 1.0 = 100%.")
                .defineInRange("Default efficiency", 1.0, 0.01, 10.0);
            SERVER_BUILDER.push("Fluid Overrides");
                FUEL_EFFICIENCY_LAVA = SERVER_BUILDER.comment("minecraft:lava")
                    .defineInRange("Lava", 0.75, 0.01, 10.0);
                FUEL_EFFICIENCY_TURPENTINE = SERVER_BUILDER.comment("createpropulsion:turpentine")
                    .defineInRange("Turpentine", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_CDG_DIESEL = SERVER_BUILDER.comment("createdieselgenerators:diesel")
                    .defineInRange("CDG Diesel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_CDG_GASOLINE = SERVER_BUILDER.comment("createdieselgenerators:gasoline")
                    .defineInRange("CDG Gasoline", 1.1, 0.01, 10.0);
                FUEL_EFFICIENCY_CDG_ETHANOL = SERVER_BUILDER.comment("createdieselgenerators:ethanol")
                    .defineInRange("CDG Ethanol", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_CDG_BIODIESEL = SERVER_BUILDER.comment("createdieselgenerators:biodiesel")
                    .defineInRange("CDG Biodiesel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_CDG_PLANT_OIL = SERVER_BUILDER.comment("createdieselgenerators:plant_oil")
                    .defineInRange("CDG Plant Oil", 0.9, 0.01, 10.0);
                FUEL_EFFICIENCY_TFMG_DIESEL = SERVER_BUILDER.comment("tfmg:diesel")
                    .defineInRange("TFMG Diesel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_TFMG_GASOLINE = SERVER_BUILDER.comment("tfmg:gasoline")
                    .defineInRange("TFMG Gasoline", 1.1, 0.01, 10.0);
                FUEL_EFFICIENCY_TFMG_KEROSENE = SERVER_BUILDER.comment("tfmg:kerosene")
                    .defineInRange("TFMG Kerosene", 1.05, 0.01, 10.0);
                FUEL_EFFICIENCY_TFMG_NAPHTHA = SERVER_BUILDER.comment("tfmg:naphtha")
                    .defineInRange("TFMG Naphtha", 0.95, 0.01, 10.0);
                FUEL_EFFICIENCY_IE_BIODIESEL = SERVER_BUILDER.comment("immersiveengineering:biodiesel")
                    .defineInRange("IE Biodiesel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_IE_ETHANOL = SERVER_BUILDER.comment("immersiveengineering:ethanol")
                    .defineInRange("IE Ethanol", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_IE_PLANT_OIL = SERVER_BUILDER.comment("immersiveengineering:plant_oil")
                    .defineInRange("IE Plant Oil", 0.9, 0.01, 10.0);
                FUEL_EFFICIENCY_IP_DIESEL = SERVER_BUILDER.comment("immersivepetroleum:diesel")
                    .defineInRange("IP Diesel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_IP_DIESEL_SULFUR = SERVER_BUILDER.comment("immersivepetroleum:diesel_sulfur")
                    .defineInRange("IP Sulfur Diesel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_IP_GASOLINE = SERVER_BUILDER.comment("immersivepetroleum:gasoline")
                    .defineInRange("IP Gasoline", 1.1, 0.01, 10.0);
                FUEL_EFFICIENCY_MEKANISM_HYDROGEN = SERVER_BUILDER.comment("mekanism:hydrogen")
                    .defineInRange("Mekanism Hydrogen", 1.2, 0.01, 10.0);
                FUEL_EFFICIENCY_MEKANISM_GENERATORS_BIOETHANOL = SERVER_BUILDER.comment("mekanismgenerators:bioethanol")
                    .defineInRange("Mekanism Generators Bioethanol", 1.35, 0.01, 10.0);
                FUEL_EFFICIENCY_NORTHSTAR_BIOFUEL = SERVER_BUILDER.comment("northstar:biofuel")
                    .defineInRange("Northstar Biofuel", 1.0, 0.01, 10.0);
                FUEL_EFFICIENCY_NORTHSTAR_HYDROCARBON = SERVER_BUILDER.comment("northstar:hydrocarbon")
                    .defineInRange("Northstar Hydrocarbon", 1.1, 0.01, 10.0);
                FUEL_EFFICIENCY_NORTHSTAR_METHANE = SERVER_BUILDER.comment("northstar:methane")
                    .defineInRange("Northstar Methane", 1.05, 0.01, 10.0);
                FUEL_EFFICIENCY_NORTHSTAR_LIQUID_HYDROGEN = SERVER_BUILDER.comment("northstar:liquid_hydrogen")
                    .defineInRange("Northstar Liquid Hydrogen", 1.25, 0.01, 10.0);
                FUEL_EFFICIENCY_STELLARIS_FUEL = SERVER_BUILDER.comment("stellaris:fuel")
                    .defineInRange("Stellaris Fuel", 1.2, 0.01, 10.0);
                FUEL_EFFICIENCY_STELLARIS_DIESEL = SERVER_BUILDER.comment("stellaris:diesel")
                    .defineInRange("Stellaris Diesel", 1.0, 0.01, 10.0);
            SERVER_BUILDER.pop();
        SERVER_BUILDER.pop();

        PropulsionDefaultStress.INSTANCE.registerAll(SERVER_BUILDER);

        SERVER_SPEC = SERVER_BUILDER.build();
        //#endregion
    
        //#region Client
        CLIENT_BUILDER.push("Thruster");
            THRUSTER_PARTICLE_OFFSET_INCOMING_VEL_MODIFIER = CLIENT_BUILDER.comment("Particle additional velocity modifier when ship is moving in the same direction as exhaust.")
                    .define("Particle velocity offset", 0.15);
            THRUSTER_PARTICLE_COUNT_MULTIPLIER = CLIENT_BUILDER.comment("The higher this number is - the more particles are spawned.")
                    .define("Particle count multiplier", 1.0);
        CLIENT_BUILDER.pop();
        CLIENT_BUILDER.push("Stirling Engine");
            STIRLING_REVOLUTION_PERIOD = CLIENT_BUILDER.comment("Revolution period of the simulated shaft (affects only piston movement).")
                .define("Revolution period", 0.2);
            STIRLING_CRANK_RADIUS = CLIENT_BUILDER.comment("Radius of the simulated crank.")
                .define("Crank radius", 0.125);
            STIRLING_CONROD_LENGTH = CLIENT_BUILDER.comment("Length of the simulated conrod.")
                .define("Conrod length", 0.5);
        CLIENT_BUILDER.pop();

        CLIENT_SPEC = CLIENT_BUILDER.build();
        //#endregion
    }
}
