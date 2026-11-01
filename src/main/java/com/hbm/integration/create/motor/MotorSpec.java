// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.hbm.data.DataGroups;
import com.hbm.data.MachineData;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): the electric motors and what each is
 * rated for. The numbers live in the world's data ({@code data/hbm/hbm/machine_config/}, see
 * {@link MachineData}), so datapacks and KubeJS can change them.
 *
 * <p>A motor turns at one fixed speed (other speeds come from Create's gears) and provides
 * {@code stressPerRpm × rpm} SU. It pays HE for what the shaft actually carries, see {@link
 * BlockEntityCreateMotor#draw}.
 */
public enum MotorSpec {
    /** Starter motor: one Create press at 128 RPM through a 1:2 gear. */
    STARTER(
            MachineData.CREATE_MOTOR_RPM,
            MachineData.CREATE_MOTOR_STRESS_PER_RPM),
    /** Desh motor: a small Create workshop at 128 RPM without gears. */
    DESH(
            MachineData.CREATE_MOTOR_DESH_RPM,
            MachineData.CREATE_MOTOR_DESH_STRESS_PER_RPM);

    private final DataGroups.Param<Integer> rpm;
    private final DataGroups.Param<Integer> stressPerRpm;

    MotorSpec(DataGroups.Param<Integer> rpm, DataGroups.Param<Integer> stressPerRpm) {
        this.rpm = rpm;
        this.stressPerRpm = stressPerRpm;
    }

    /** Speed of the shaft while the motor runs. */
    public int rpm() {
        return Math.min(rpm.get(), 0x100);
    }

    /** Stress capacity per RPM, as Create counts it. */
    public int stressPerRpm() {
        int perRpm = stressPerRpm.get();
        return perRpm > 0 ? perRpm : 1;
    }

    /** SU the motor provides at its speed. */
    public long ratedStress() {
        return (long) stressPerRpm() * rpm();
    }

    /**
     * HE the motor holds: a few ticks of full load, just enough to bridge the order in which the
     * network and the motor tick. A motor has no store of its own; cut the cable and it stops.
     */
    public long buffer() {
        return (long) Math.ceil(fullLoadHe() * MachineData.CREATE_MOTORS_BUFFER_TICKS.get());
    }

    /** HE per tick for {@code stress} SU on the shaft, losses included. */
    public static double heFor(double stress) {
        return stress * suTariff();
    }

    /** HE per tick for one SU on the shaft: NTM's price of an SU over the motor's efficiency. */
    private static double suTariff() {
        return MachineData.CREATE_MOTORS_HE_PER_SU.get()
                / MachineData.CREATE_MOTORS_EFFICIENCY.get();
    }

    /** HE per tick the motor spends just turning, whatever the load. */
    public double idleHe() {
        return heFor(ratedStress()) * MachineData.CREATE_MOTORS_IDLE.get();
    }

    /** HE per tick at full load. */
    public double fullLoadHe() {
        return heFor(ratedStress()) + idleHe();
    }
}
