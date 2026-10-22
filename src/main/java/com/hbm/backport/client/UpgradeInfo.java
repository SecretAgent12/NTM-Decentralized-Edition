// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client;

import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.util.BobMathUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * backport-fix: BF-111 (Discord) what an upgrade does in the machine whose GUI is open, as in 1.7.10: hovering a speed,
 * power, effect, fortune, afterburn or overdrive upgrade inside a machine GUI listed that machine's numbers
 * ("Process time -50%", "Consumption +100%", ...). In 1.7.10 every machine implemented
 * IUpgradeInfoProvider.canProvideInfo/provideInfo and ItemMachineUpgrade.addInformation asked the container's
 * inventory first. NTM: NEXT kept only getValidUpgrades (the GUI's "accepted upgrades" panel), so the upgrade
 * tooltips were empty. The 31 provideInfo methods of 1.7.10 are ported here 1:1 (same numbers, same lang keys,
 * same label), keyed by the block entity of the open menu, so the NEXT machine classes stay untouched. The lines go
 * right under the item name (typed upgrades have no description of their own, as in 1.7.10).
 */
@EventBusSubscriber(modid = "hbm", value = Dist.CLIENT)
public final class UpgradeInfo {

    private static final String KEY_ACID = "upgrade.acid";
    private static final String KEY_BURN = "upgrade.burn";
    private static final String KEY_CONSUMPTION = "upgrade.consumption";
    private static final String KEY_COOLANT_CONSUMPTION = "upgrade.coolantConsumption";
    private static final String KEY_DELAY = "upgrade.delay";
    private static final String KEY_SPEED = "upgrade.speed";
    private static final String KEY_EFFICIENCY = "upgrade.efficiency";
    private static final String KEY_PRODUCTIVITY = "upgrade.productivity";
    private static final String KEY_FORTUNE = "upgrade.fortune";
    private static final String KEY_RANGE = "upgrade.range";

    private UpgradeInfo() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ItemMachineUpgrade upgrade)) return;
        if (upgrade.type == UpgradeType.SPECIAL || event.getToolTip().isEmpty()) return;
        List<Component> lines = new ArrayList<>();
        if (provide(upgrade.type, upgrade.tier, lines::add)) event.getToolTip().addAll(1, lines);
    }

    /** True if the machine whose GUI is open described the upgrade (1.7.10 ItemMachineUpgrade.addInformation). */
    public static boolean provide(UpgradeType type, int level, Consumer<Component> info) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return false;
        if (!(screen.getMenu() instanceof BlockEntityMenu<?> menu)) return false;
        BlockEntity be = menu.blockEntity();
        if (be == null) return false;
        return provide(be, type, level, info);
    }

    /** 1.7.10 IUpgradeInfoProvider.getStandardLabel. */
    private static void label(Consumer<Component> info, String block) {
        String name = I18n.get(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("hbm", block)).getDescriptionId());
        info.accept(Component.literal(ChatFormatting.YELLOW + ">>> " + name + " <<<"));
    }

    private static boolean provide(BlockEntity be, UpgradeType type, int level, Consumer<Component> info) {
        // 1.7.10 TileEntityElectrolyser
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineElectrolyser) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_electrolyser");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityFurnaceIron
        if (be instanceof com.hbm.blockentity.machine.BlockEntityFurnaceIron) {
            if (!(type == UpgradeType.SPEED)) return false;
            label(info, "furnace_iron");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 50 / 3) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineArcFurnaceLarge
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineArcFurnace) {
            if (!(type == UpgradeType.SPEED)) return false;
            label(info, "machine_arc_furnace");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (100 - 100 / (level * 2 + 1)) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + ((int) Math.pow(5, level) * 100 - 100) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineArcWelder
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineArcWelder) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_arc_welder");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 100 / 6) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 100 / 6) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 100 / 3) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineAssemblyFactory
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineAssemblyFactory) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_assembly_factory");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_SPEED, "+" + (level * 100 / 3) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineAssemblyMachine
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineAssemblyMachine) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_assembly_machine");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_SPEED, "+" + (level * 100 / 3) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineCentrifuge
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineCentrifuge) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_centrifuge");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineChemicalFactory
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineChemicalFactory) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_chemical_factory");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_SPEED, "+" + (level * 100 / 3) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineChemicalPlant
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineChemicalPlant) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_chemical_plant");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_SPEED, "+" + (level * 100 / 3) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineCompressorBase
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineCompressor) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_compressor");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + "Generic compression: "+ I18n.get(KEY_DELAY, "-" + (level == 3 ? 90 : level == 2 ? 80 : level == 1 ? 40 : 0) + "%")));
                info.accept(Component.literal(ChatFormatting.GREEN + "Recipe: "+ I18n.get(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineCrystallizer
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineCrystallizer) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.EFFECT || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_crystallizer");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.EFFECT) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_EFFICIENCY, "x" + level)));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 200) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineCyclotron
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineCyclotron) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.EFFECT)) return false;
            label(info, "machine_cyclotron");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_COOLANT_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 10) + "%")));
            }
            if (type == UpgradeType.EFFECT) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_COOLANT_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineEPress
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineEPress) {
            if (!(type == UpgradeType.SPEED)) return false;
            label(info, "machine_epress");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (50 * level / 3) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineElectricFurnace
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineElectricFurnace) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER)) return false;
            label(info, "machine_electric_furnace");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 30) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 10) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineExcavator
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineExcavator) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER)) return false;
            label(info, "machine_excavator");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (100 - 200 / (level + 2)) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineExposureChamber
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineExposureChamber) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_exposure_chamber");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineMiningLaser
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineMiningLaser) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE || type == UpgradeType.EFFECT || type == UpgradeType.FORTUNE)) return false;
            label(info, "machine_mining_laser");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (100 * level / 16) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 * level / 16) + "%")));
            }
            if (type == UpgradeType.EFFECT) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_RANGE, "+" + (2 * level) + "m")));
            }
            if (type == UpgradeType.FORTUNE) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_FORTUNE, "+" + level)));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineMixer
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineMixer) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_mixer");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 300) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineOreSlopper
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineOreSlopper) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.EFFECT)) return false;
            label(info, "machine_ore_slopper");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.EFFECT) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_EFFICIENCY, "+" + (level * 10) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachinePUREX
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachinePUREX) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_purex");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_SPEED, "+" + (level * 100 / 3) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachinePrecAss
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachinePrecAss) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_precass");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_SPEED, "+" + (level * 100 / 3) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 50) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineSolderingStation
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineSolderingStation) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "machine_soldering_station");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 100 / 6) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 100 / 6) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 100 / 3) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineTurbofan
        if (be instanceof com.hbm.blockentity.machine.BlockEntityMachineTurbofan) {
            if (!(type == UpgradeType.AFTERBURN)) return false;
            label(info, "machine_turbofan");
            if (type == UpgradeType.AFTERBURN) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_EFFICIENCY, "+" + (int)(level * 100 * (1 + Math.min(level / 3D, 4D))) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineFrackingTower (canProvideInfo of TileEntityOilDrillBase)
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachineFrackingTower) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE || type == UpgradeType.AFTERBURN)) return false;
            label(info, "machine_fracking_tower");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 25) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 10) + "%")));
            }
            if (type == UpgradeType.AFTERBURN) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_BURN, level * 10, level * 50)));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineGasFlare
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachineGasFlare) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.EFFECT)) return false;
            label(info, "machine_flare");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.EFFECT) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_EFFICIENCY, "+" + (100 * level / 3) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineLiquefactor
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachineLiquefactor) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER)) return false;
            label(info, "machine_liquefactor");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineOilWell (canProvideInfo of TileEntityOilDrillBase)
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachineOilWell) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE || type == UpgradeType.AFTERBURN)) return false;
            label(info, "machine_well");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 25) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 10) + "%")));
            }
            if (type == UpgradeType.AFTERBURN) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_BURN, level * 10, level * 50)));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachinePumpjack (canProvideInfo of TileEntityOilDrillBase)
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachinePumpjack) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE || type == UpgradeType.AFTERBURN)) return false;
            label(info, "machine_pumpjack");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 25) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_DELAY, "+" + (level * 10) + "%")));
            }
            if (type == UpgradeType.AFTERBURN) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_BURN, level * 10, level * 50)));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachinePyroOven
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachinePyroOven) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER)) return false;
            label(info, "machine_pyrooven");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (Math.pow(level + 1, 2) * 100 - 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        // 1.7.10 TileEntityMachineSolidifier
        if (be instanceof com.hbm.blockentity.machine.oil.BlockEntityMachineSolidifier) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.POWER)) return false;
            label(info, "machine_solidifier");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_DELAY, "-" + (level * 25) + "%")));
                info.accept(Component.literal(ChatFormatting.RED + I18n.get(KEY_CONSUMPTION, "+" + (level * 100) + "%")));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")));
            }
            return true;
        }
        // 1.7.10 TileEntityTurretMaxwell
        if (be instanceof com.hbm.blockentity.turret.BlockEntityTurretMaxwell) {
            if (!(type == UpgradeType.SPEED || type == UpgradeType.EFFECT || type == UpgradeType.POWER || type == UpgradeType.AFTERBURN || type == UpgradeType.OVERDRIVE)) return false;
            label(info, "turret_maxwell");
            if (type == UpgradeType.SPEED) {
                info.accept(Component.literal(ChatFormatting.GREEN + "Damage +0." + (level * 25) + "/t"));
            }
            if (type == UpgradeType.POWER) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_CONSUMPTION, "-" + (level * 3) + "%")));
            }
            if (type == UpgradeType.EFFECT) {
                info.accept(Component.literal(ChatFormatting.GREEN + I18n.get(KEY_RANGE, "+" + (level * 3) + "m")));
            }
            if (type == UpgradeType.AFTERBURN) {
                info.accept(Component.literal(ChatFormatting.GREEN + "Afterburn +3s"));
            }
            if (type == UpgradeType.OVERDRIVE) {
                info.accept(Component.literal((BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY) + "YES"));
            }
            return true;
        }
        return false;
    }
}
