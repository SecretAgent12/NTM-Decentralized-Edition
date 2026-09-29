// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * backport: Ponder storyboards. The English texts here are only Ponder's defaults; the game shows
 * the lang entries hbm.ponder.&lt;scene&gt;.header / .text_N (en_us, ru_ru, ...). The firebox texts
 * follow Bob's World-In-A-Jar lesson (cannery.firebox.*).
 */
public final class NTMPonderScenes {
    private NTMPonderScenes() {}

    private static void firebox(SceneBuilder scene, SceneBuildingUtil util, BlockPos firebox) {
        scene.world().showSection(util.select().position(firebox), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("The firebox burns flammable items to generate heat.")
                .pointAt(util.vector().blockSurface(firebox, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(110)
                .text("It can burn any flammable item, although higher quality fuels such as coal, coke and solid fuel burn longer and hotter.")
                .pointAt(util.vector().blockSurface(firebox, Direction.NORTH))
                .placeNearTarget();
        scene.idle(120);
        scene.overlay().showText(110)
                .text("Heat is given off by the copper contact at the top of the firebox. Machines with an identical contact on the bottom can receive heat by being placed on top of the firebox.")
                .pointAt(util.vector().topOf(firebox))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
    }

    public static void fireboxBoiler(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("firebox_boiler", "Heating a Boiler with a Firebox");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8f); // firebox + boiler stand 5 blocks tall: zoom out so the top fits
        scene.showBasePlate();
        scene.idle(10);
        BlockPos firebox = util.grid().at(2, 1, 2);
        BlockPos boiler = util.grid().at(2, 2, 2);

        firebox(scene, util, firebox);

        scene.world().showSection(util.select().position(boiler), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(100)
                .text("The boiler takes that heat through its bottom contact and turns water into steam.")
                .pointAt(util.vector().blockSurface(boiler, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100)
                .text("Feed water in with fluid ducts and pipe the steam out to a turbine to make power.")
                .pointAt(util.vector().centerOf(boiler.above(2)))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(100)
                .text("If heat isn't being used up and the heat buffer becomes full, the firebox will shut off to prevent wasting of fuel.")
                .pointAt(util.vector().blockSurface(firebox, Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.markAsFinished();
    }

    public static void fireboxFurnace(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("firebox_furnace", "Heating a Steel Furnace with a Firebox");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos firebox = util.grid().at(2, 1, 2);
        BlockPos furnace = util.grid().at(2, 2, 2);

        firebox(scene, util, firebox);

        scene.world().showSection(util.select().position(furnace), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(100)
                .text("The steel furnace smelts up to three stacks at once using the heat from below.")
                .pointAt(util.vector().blockSurface(furnace, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(110)
                .text("It only works while it gets enough heat. Ores give a bonus item for every fourth one smelted, logs and tar for every second.")
                .pointAt(util.vector().topOf(furnace.above()))
                .placeNearTarget();
        scene.idle(120);
        scene.markAsFinished();
    }
}
