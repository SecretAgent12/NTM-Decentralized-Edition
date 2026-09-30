# NTM + CC: Tweaked

With [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) installed, lots of NTM blocks become peripherals.
From a computer you can read reactor temperatures, move RBMK control rods, manage turbines, turrets and much more.

## Connecting

Put the block next to a computer, or connect it with a wired modem. A few blocks are picky about the side:

- **Turrets** only take the connection from below.
- **Multiblocks** connect through their port blocks.
- **RBMK columns** (boiler, cooler, heater, outgasser) connect through the top of the column.

## Peripheral types

| Type | Block(s) |
|---|---|
| `ntm_pa_source` | Particle accelerator: source (momentum, state, recipe, coolant) |
| `ntm_pa_detector` | Particle accelerator: detector (recipe, energy, coolant) |
| `ntm_pa_dipole` | Particle accelerator: dipole magnet (directions, threshold) |
| `ntm_pa_quad` | Particle accelerator: quadrupole magnet |
| `ntm_pa_rfc` | Particle accelerator: RF cavity |
| `ntm_launch_pad` | Launch pad, small and large (energy, fuel, ready to launch) |
| `ntm_custom_launch_pad` | Launch table for custom missiles (contents, target coordinates) |
| `ntm_fusion_torus` | Fusion reactor: torus (plasma, fuel, progress) |
| `ntm_fusion_boiler` | Fusion reactor: boiler |
| `ntm_fusion_breeder` | Fusion reactor: breeder |
| `ntm_fusion_klystron` | Fusion reactor: klystron (power, air) |
| `ntm_fusion_mhdt` | Fusion reactor: MHD generator |
| `breeding_reactor` | Breeding reactor (flux, progress) |
| `ntm_coker` | Coker unit (fluid, heat) |
| `ntm_combustion_engine` | Combustion engine (fuel, power, throttle) |
| `dfc_emitter` / `dfc_receiver` / `dfc_injector` / `dfc_stabilizer` | Dark fusion core parts |
| `ntm_gas_turbine` | Gas turbine (fuel, power, auto mode) |
| `ntm_turbine` | Steam turbines: small, large, Chungus, industrial (the industrial one also has `getFlywheel`) |
| `ntm_geiger` | Geiger counter (radiation) |
| `ntm_icf_reactor` | ICF reactor (heat, power, pellet) |
| `microwave` | Microwave (test device) |
| `ntm_pwr_control` | PWR controller (heat, flux, rods, fuel, coolant) |
| `ntm_pile_control` / `ntm_pile_loader` | Chicago Pile: control rod, loader |
| `ntm_radar` | Radar, small and large (targets, range, settings) |
| `reactor_control` | Reactor control panel (link, automation settings) |
| `research_reactor` | Research reactor (temperature, rods, flux) |
| `ntm_satlink` | Satellite link (frequency, satellite type, response) |
| `watz_reactor` | Watz reactor (heat, flux, waste) |
| `zirnox_reactor` | ZIRNOX reactor (temperature, pressure, water/steam, CO₂, fuel) |
| `ntm_power_gauge` | Power gauge on a cable |
| `ntm_fluid_gauge` | Flow gauge on a pipe |
| `ntm_fluid_pump` | Fluid pump (pressure, flow, priority) |
| `ntm_fluid_counter_valve` | Fluid counter valve |
| `radio_torch` | Radio torch, sender/receiver (channel, custom map) |
| `radio_controller` | Radio controller |
| `radio_reader` | Radio reader (8 channels) |
| `radio_autocal` | Radio AUTOCAL (buffer, script, history) |
| `ntm_telex` | Radio telex (channels, texts) |
| `rbmk_fuel_rod` | RBMK fuel channel (heat, flux, depletion, xenon) |
| `rbmk_control_rod` | RBMK control rod, manual and automatic (level; manual rods also have a colour) |
| `rbmk_boiler` / `rbmk_cooler` / `rbmk_heater` / `rbmk_outgasser` | RBMK columns: boiler, cooler, heater, outgasser |
| `rbmk_console` | RBMK console (column data; `getColumnData(7,7)` is the centre) |
| `rbmk_crane` | RBMK crane console (position, rod under the crane) |
| `rbmk_terminal` | RBMK terminal (OC mode, history) |
| `rbmk_gauge` / `rbmk_graph` / `rbmk_indicator` / `rbmk_numitron` | RBMK panels: gauges, graphs, indicators, numitrons (indices start at 1) |
| `rbmk_keypad` / `rbmk_lever` | RBMK panels: keypads and levers |
| `capacitor` | Copper capacitor |
| `ntm_energy_storage` | Battery socket and REDD (modes, energy; the socket also has `getPackInfo`) |
| `ntm_energy_storage_legacy` | Energy storage block and FEnSU |
| `ntm_fluid_tank` | Barrels, tank, Orbus, large tank |
| `ntm_mass_storage` | Mass storage (fill level, item type) |
| `ntm_turret` | Turrets (Chekhov, Jeremy, Tauon, Richard, Howard, Maxwell, Fritz, Sentry…) |
| `ntm_artillery` | Artillery and HIMARS (target, distance to target) |

To see every method a block offers, run `peripheral.getMethods("<side or name>")` on the computer.

## `ntm_scan`: check everything at once

[`ntm_scan.lua`](ntm_scan.lua) walks through all peripherals connected to a computer and calls every method
that only *reads* something (`get*`, `is*`, `has*`, `canLaunch`, `read`…). It never calls setters, never
launches missiles, never presses AZ-5, never fires turrets. That makes it handy for seeing what a block
exposes, and for checking that nothing is broken.

**Getting it onto the computer:**

- **Drag and drop.** Open the computer's terminal and drag `ntm_scan.lua` into the game window. If nothing
  happens, run `import` and drag it again.
- **Through the world folder.** Copy the file to `saves/<world>/computercraft/computer/<id>/`, or
  `world/computercraft/computer/<id>/` on a server. The computer's `<id>` is shown by the `id` command.

**Running it:**

```
ntm_scan          -- every connected peripheral
ntm_scan back     -- just one (a side, or a name from peripheral.getNames())
```

**Reading the output:**

```
== ntm_turbine_0 [ntm_turbine]          peripheral name and type
  OK  getFluid = 1200, 64000            the method worked; values separated by commas
  OK  getGaugeInfo(1) = {...}           arguments the method was called with are in brackets
  ERR getInfo: <error text>             the method threw an error: worth reporting
  n/a getFlywheel                       this block doesn't have that method (fine, the type is shared)
  not called: setType start stop        methods the script skips on purpose (setters, actions)
-- monitor_0 [monitor] not an NTM peripheral, skipped
== summary: NTM 3, other 1 | OK 25, ERR 0, n/a 2
```

Long values are cut to 200 characters on screen. The full output is also saved to `ntm_scan.log` next to the
script, and you can open it with `edit ntm_scan.log`. If you get any `ERR` lines, please
[open an issue](https://github.com/SecretAgent12/NTM-Decentralized-Edition/issues) and attach the log.
