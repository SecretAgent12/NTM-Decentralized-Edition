// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.hbm.lib.Library;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): the electric motors' Ponder guides.
 * Reached from {@code NTMPonderPlugin} only with Create loaded.
 *
 * <p>Each motor has its own basics scene (schematics create_motor.nbt / create_motor_desh.nbt, the
 * same layout with that motor in it: cable, motor, speedometer, stressometer, shaft), and both
 * share the comparison scene (create_motor_power.nbt)
 * where the basic motor runs out of capacity and the desh motor takes over. The English texts here
 * are Ponder's defaults; the game shows the lang entries hbm.ponder.&lt;scene&gt;.header / .text_N.
 *
 * <p>Stress numbers follow Create's default config: a mechanical press takes 8 su per RPM, so at
 * 64 RPM two presses fill the basic motor (16 × 64 su), and at 128 RPM five presses use 5/8 of the
 * desh motor (64 × 128 su).
 */
public final class CreateMotorScenes {

    private static final ResourceLocation MOTOR = Library.id("create_motor");
    private static final ResourceLocation MOTOR_DESH = Library.id("create_motor_desh");

    private CreateMotorScenes() {}

    public static void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // each motor's own basics first, then the shared comparison, registered once for both
        helper.forComponents(MOTOR)
                .addStoryBoard("create_motor", (scene, util) -> basics(scene, util, false));
        helper.forComponents(MOTOR_DESH)
                .addStoryBoard("create_motor_desh", (scene, util) -> basics(scene, util, true));
        helper.forComponents(MOTOR, MOTOR_DESH)
                .addStoryBoard("create_motor_power", CreateMotorScenes::power);
    }

    /** Next to Create's own motors in its "Kinetic Sources" index. */
    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.addToTag(AllCreatePonderTags.KINETIC_SOURCES).add(MOTOR).add(MOTOR_DESH);
    }

    /** Power in, the shaft out, the speedometer, the redstone switch and the undervoltage relay. */
    public static void basics(SceneBuilder builder, SceneBuildingUtil util, boolean desh) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        if (desh)
            scene.title(
                    "create_motor_desh", "Generating Rotational Force with a Desh Electric Motor");
        else scene.title("create_motor", "Generating Rotational Force with Electric Motors");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(0xA);

        float rpm = desh ? 128 : 64;
        BlockPos cable = util.grid().at(0, 1, 2);
        BlockPos motor = util.grid().at(1, 1, 2);
        BlockPos lever = util.grid().at(1, 2, 2);
        BlockPos gauge = util.grid().at(2, 1, 2);
        BlockPos stressometer = util.grid().at(3, 1, 2);
        Selection shafts = util.select().fromTo(4, 1, 2, 2, 1, 2);
        Selection kinetic = util.select().fromTo(1, 1, 2, 4, 1, 2);

        scene.world().showSection(util.select().position(motor), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("Electric motors turn HE from an NTM power network into rotational force.")
                .pointAt(util.vector().topOf(motor))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(cable), Direction.EAST);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("Power comes in through cables on any side.")
                .pointAt(util.vector().centerOf(cable))
                .placeNearTarget();
        scene.idle(80);

        {
            scene.world().showSection(shafts, Direction.WEST);
            scene.idle(10);
            scene.world().setKineticSpeed(kinetic, rpm);
            scene.effects().rotationSpeedIndicator(motor);
        }
        scene.idle(20);
        scene.overlay().showText(90)
                .text(desh
                        ? "The desh motor turns its shaft at 128 RPM and gives 8192 su of stress capacity. Use gears and gearboxes to change the speed."
                        : "The shaft turns at one fixed speed, 64 RPM, and the motor gives 1024 su of stress capacity. Use gears and gearboxes to change the speed.")
                .pointAt(util.vector().topOf(gauge))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(100)
                .text("The stressometer shows the stress on the network, and the motor draws HE in proportion to it. A free-running shaft costs only a small idle loss.")
                .pointAt(util.vector().topOf(stressometer))
                .placeNearTarget();
        scene.idle(110);

        scene.world().showSection(util.select().position(lever), Direction.DOWN);
        scene.idle(15);
        switchLever(scene, util, lever, kinetic, 0);
        scene.overlay().showText(70)
                .text("A redstone signal switches the motor off.")
                .pointAt(util.vector().centerOf(lever))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);
        switchLever(scene, util, lever, kinetic, rpm);
        scene.idle(20);

        scene.overlay().showText(110)
                .text("If the power supply fails, an undervoltage relay stops the motor. It starts again on its own shortly after power returns.")
                .pointAt(util.vector().centerOf(cable))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        scene.markAsFinished();
    }

    private static void switchLever(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            BlockPos lever,
            Selection kinetic,
            float rpm) {
        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.effects().indicateRedstone(lever);
        scene.world().setKineticSpeed(kinetic, rpm);
    }

    /**
     * The basic motor with a speedometer and a stressometer, presses added until it overstresses,
     * then the desh motor swapped in, running all of them with room to spare.
     */
    public static void power(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("create_motor_power", "Basic and Desh Electric Motors under Load");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.7f); // a long shaft line: zoom out so all nine blocks fit
        scene.showBasePlate();
        scene.idle(10);

        BlockPos cable = util.grid().at(0, 2, 4);
        BlockPos motor = util.grid().at(1, 2, 4);
        BlockPos speedometer = util.grid().at(2, 2, 4);
        BlockPos stressometer = util.grid().at(3, 2, 4);
        Selection gauge = util.select().position(stressometer);

        scene.world().showSection(util.select().fromTo(0, 1, 4, 3, 2, 4), Direction.DOWN);
        scene.idle(15);
        press(scene, util, 4);
        scene.world().setKineticSpeed(kinetic(util, 4), 64);
        stress(scene, gauge, 0.5f);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("The speedometer shows the motor's speed, the stressometer how much of its stress capacity the machines use.")
                .pointAt(util.vector().topOf(stressometer))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("One press at 64 RPM takes 512 su, half of the basic motor's 1024 su.")
                .pointAt(util.vector().topOf(util.grid().at(4, 2, 4)))
                .placeNearTarget();
        scene.idle(80);

        press(scene, util, 5);
        scene.world().setKineticSpeed(kinetic(util, 5), 64);
        stress(scene, gauge, 1f);
        scene.overlay().showText(60)
                .text("Two presses take all 1024 su.")
                .pointAt(util.vector().topOf(stressometer))
                .placeNearTarget();
        scene.idle(70);

        press(scene, util, 6);
        scene.world().setKineticSpeed(kinetic(util, 6), 0);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("A third one overstresses the network, and everything stops.")
                .pointAt(util.vector().topOf(util.grid().at(6, 2, 4)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setBlock(
                motor,
                CreateMotors.MOTOR_DESH.get().defaultBlockState()
                        .setValue(DirectionalKineticBlock.FACING, Direction.EAST),
                true);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetic(util, 6), 128);
        stress(scene, gauge, 0.375f);
        scene.effects().rotationSpeedIndicator(motor);
        scene.overlay().showText(100)
                .text("Swap in a desh motor: 128 RPM and 8192 su. At the doubled speed the same three presses take 3072 su, little more than a third.")
                .pointAt(util.vector().topOf(speedometer))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        press(scene, util, 7);
        press(scene, util, 8);
        scene.world().setKineticSpeed(kinetic(util, 8), 128);
        stress(scene, gauge, 0.625f);
        scene.overlay().showText(70)
                .text("Five presses take 5120 su out of 8192: there is still room for more machines.")
                .pointAt(util.vector().topOf(stressometer))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(100)
                .colored(PonderPalette.BLUE)
                .text("The motors pay HE for the load: a heavily loaded desh motor draws far more power than an idle one.")
                .pointAt(util.vector().centerOf(cable))
                .placeNearTarget();
        scene.idle(110);
        scene.markAsFinished();
    }

    /** Brings in the press at {@code x} together with the depot under it. */
    private static void press(CreateSceneBuilder scene, SceneBuildingUtil util, int x) {
        scene.world().showSection(util.select().fromTo(x, 1, 4, x, 2, 4), Direction.DOWN);
        scene.idle(15);
    }

    /** Everything turning, from the motor up to the last press shown. */
    private static Selection kinetic(SceneBuildingUtil util, int lastPress) {
        return util.select().fromTo(1, 2, 4, lastPress, 2, 4);
    }

    /** The stressometer's dial, 0…1 of the network's capacity. */
    private static void stress(CreateSceneBuilder scene, Selection gauge, float fraction) {
        scene.world().modifyBlockEntityNBT(
                gauge, StressGaugeBlockEntity.class, nbt -> nbt.putFloat("Value", fraction));
    }
}
