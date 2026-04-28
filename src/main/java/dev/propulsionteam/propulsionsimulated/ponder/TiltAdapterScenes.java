package dev.propulsionteam.propulsionsimulated.ponder;

import dev.propulsionteam.propulsionsimulated.tilt_adapter.TiltAdapterBlockEntity;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;

public class TiltAdapterScenes {
    public static void redstoneControl(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("tilt_adapter", "Controlling Tilt with Redstone");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        Selection rearCasing = util.select().position(2, 1, 3);
        Selection creativeMotor = util.select().position(2, 2, 3);
        Selection adapter = util.select().position(2, 2, 2);
        Selection adapterSupport = util.select().position(2, 1, 2);
        Selection leverSupports = util.select().fromTo(1, 1, 2, 1, 1, 2)
            .add(util.select().fromTo(3, 1, 2, 3, 1, 2));
        Selection leftLever = util.select().position(1, 2, 2);
        Selection rightLever = util.select().position(3, 2, 2);
        Selection drivePath = rearCasing.copy().add(creativeMotor).add(adapter).add(adapterSupport);

        scene.world().setKineticSpeed(drivePath, 96);
        scene.world().showSection(rearCasing, Direction.DOWN);
        scene.world().showSection(creativeMotor, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(adapterSupport, Direction.DOWN);
        scene.world().showSection(adapter, Direction.DOWN);
        scene.idle(15);
        scene.effects().indicateSuccess(util.grid().at(2, 2, 2));
        scene.idle(5);

        scene.overlay().showText(80)
            .sharedText("tilt_adapter.intro")
            .placeNearTarget()
            .pointAt(util.vector().topOf(util.grid().at(2, 2, 2)));
        scene.idle(90);

        scene.world().showSection(leverSupports, Direction.DOWN);
        scene.world().showSection(leftLever, Direction.DOWN);
        scene.world().showSection(rightLever, Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("tilt_adapter.equal_signals")
            .placeNearTarget()
            .pointAt(util.vector().centerOf(util.grid().at(2, 2, 2)));
        scene.world().modifyBlockEntityNBT(adapter, TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneLeft", 8);
            nbt.putInt("redstoneRight", 8);
            nbt.putFloat("targetAngle", 0f);
            nbt.putFloat("currentAngle", 0f);
            nbt.putInt("activeMoveDirection", 0);
        });
        scene.world().modifyBlockEntityNBT(leftLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 8));
        scene.world().modifyBlockEntityNBT(rightLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 8));
        scene.effects().indicateRedstone(util.grid().at(1, 2, 2));
        scene.effects().indicateRedstone(util.grid().at(3, 2, 2));
        scene.idle(90);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("tilt_adapter.left_higher")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST));
        scene.world().modifyBlockEntityNBT(leftLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 15));
        scene.world().modifyBlockEntityNBT(rightLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 3));
        scene.world().modifyBlockEntityNBT(adapter, TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneLeft", 15);
            nbt.putInt("redstoneRight", 3);
            nbt.putFloat("targetAngle", 36f);
            nbt.putFloat("networkTargetAngle", 36f);
            nbt.putFloat("currentAngle", 36f);
            nbt.putInt("activeMoveDirection", 1);
        });
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 2, 2));
        scene.idle(100);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("tilt_adapter.right_higher")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.EAST));
        scene.world().modifyBlockEntityNBT(leftLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 2));
        scene.world().modifyBlockEntityNBT(rightLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 14));
        scene.world().modifyBlockEntityNBT(adapter, TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneLeft", 2);
            nbt.putInt("redstoneRight", 14);
            nbt.putFloat("targetAngle", -36f);
            nbt.putFloat("networkTargetAngle", -36f);
            nbt.putFloat("currentAngle", -36f);
            nbt.putInt("activeMoveDirection", -1);
        });
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 2, 2));
        scene.idle(100);
    }
}
