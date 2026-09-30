-- ntm_scan: smoke test of HBM's Nuclear Tech Mod (hbm) CC: Tweaked peripherals.
-- Usage:  ntm_scan            -- every attached peripheral
--         ntm_scan <name>     -- only that one (a side like "back" or a modem name like "ntm_turbine_0")
-- Calls only read-only methods (getters), each under pcall; never setters, launch, AZ-5, fire, press.
-- The full output is also written to ntm_scan.log next to the script.
--
-- GETTERS: peripheral type -> read-only methods, taken from the mod sources
-- (com/hbm/compat/computercraft/**). An entry is either "method" (no arguments) or
-- { "method", arg1, ... } for getters that need an index or coordinates.
-- Several blocks share one type with slightly different method sets (ntm_turbine: getFlywheel only
-- on the industrial turbine; ntm_energy_storage: getPackInfo only on the battery socket;
-- rbmk_control_rod: getColor only on manual rods), so a method is called only when the peripheral
-- really exposes it; the others are listed as "n/a".

local GETTERS = {
  -- Albion particle accelerator
  ntm_pa_detector   = { "getEnergyInfo", "getCoolant", "getCrafting", "getInfo" },
  ntm_pa_dipole     = { "getEnergyInfo", "getCoolant", "getDirLower", "getDirUpper", "getDirRedstone", "getThreshold", "getInfo" },
  ntm_pa_quad       = { "getEnergyInfo", "getCoolant", "getInfo" },
  ntm_pa_rfc        = { "getEnergyInfo", "getCoolant", "getInfo" },
  ntm_pa_source     = { "getEnergyInfo", "getCoolant", "getMomentum", "getState", "getCrafting", "getInfo" },
  -- launch pads
  ntm_launch_pad        = { "getEnergyInfo", "getFluid", "canLaunch", "getTier", "getPos" },
  ntm_custom_launch_pad = { "getEnergyInfo", "getContents", "getLaunchInfo", "getCoords" },
  -- fusion
  ntm_fusion_boiler   = { "getPlasmaEnergy", "getFluid", "getInfo" },
  ntm_fusion_breeder  = { "getNeutronEnergy", "getProgress", "getFluid", "getCrafting", "getInfo" },
  ntm_fusion_klystron = { "getEnergyInfo", "getAir", "getOutput", "getInfo" },
  ntm_fusion_mhdt     = { "getEnergyInfo", "getPlasmaEnergy", "getCoolant", "getInfo" },
  ntm_fusion_torus    = { "getEnergyInfo", "getCoolant", "getFluid", "getKlystronEnergy", "getPlasmaEnergy", "getFuelConsumption", "getRecipeProgress", "getInfo" },
  -- machines and reactors
  breeding_reactor      = { "getFlux", "getProgress", "getInfo" },
  ntm_coker             = { "getTypeStored", "getFluidStored", "getHeat", "getInfo" },
  ntm_combustion_engine = { "getFluid", "getType", "getPower", "getThrottle", "getState", "getEfficiency", "getInfo" },
  dfc_emitter           = { "getEnergyInfo", "getCryogel", "getInput", "getInfo", "isActive" },
  dfc_injector          = { "getFuel", "getTypes", "getInfo" },
  dfc_receiver          = { "getEnergyInfo", "getCryogel", "getInfo" },
  dfc_stabilizer        = { "getEnergyInfo", "getInput", "getDurability", "getInfo" },
  ntm_gas_turbine       = { "getFluid", "getType", "getPower", "getThrottle", "getState", "getAuto", "getInfo" },
  ntm_geiger            = { "getRads" },
  ntm_icf_reactor       = { "getHeat", "getHeatingRate", "getMaxHeat", "getPower", "getFluid", "getPelletStats" },
  ntm_turbine           = { "getFluid", "getType", "getPower", "getFlywheel", "getInfo" },
  microwave             = { "test", "variableget" },
  ntm_pwr_control       = { "getHeat", "getFlux", "getLevel", "getCoolantInfo", "getFuelInfo", "getInfo" },
  ntm_pile_control      = { "getLevel" },
  ntm_pile_loader       = { "getTemp", "getDepletion", "getLifetime", "getType", "getLoadingType", "isLoading" },
  ntm_radar             = { "getSettings", "getRange", "getEnergyInfo", "isJammed", "getAmount",
                            { "isIndexPlayer", 1 }, { "getIndexType", 1 }, { "getEntityAtIndex", 1 }, "getPos" },
  reactor_control       = { "isLinked", "getReactor", "getParams" },
  research_reactor      = { "getTemp", "getLevel", "getTargetLevel", "getFlux", "getInfo" },
  ntm_satlink           = { "isConnected", "getFreq", "getType", "read" },
  watz_reactor          = { "getHeat", "getFlux", "getCoolantInfo", "getWasteInfo", "isOn", "getInfo" },
  zirnox_reactor        = { "getTemp", "getPressure", "getWater", "getSteam", "getCarbonDioxide", "isActive", "getInfo", { "getFuel", 0 } },
  -- networks and radio
  ntm_power_gauge         = { "getTransfer", "getInfo" },
  ntm_fluid_counter_valve = { "getFluid", "getCounter", "getState" },
  ntm_fluid_pump          = { "getFluid", "getPressure", "getFlow", "getPriority", "getInfo" },
  ntm_fluid_gauge         = { "getTransfer", "getFluid", "getInfo" },
  radio_autocal           = { "getBuffer", "getScript", "getState", "getIgnoreError", "getAutoReboot", { "getHistory", 0 } },
  radio_controller        = { "getChannel", "getPolling" },
  radio_reader            = { { "getChannel", 0 }, { "getName", 0 }, "getPolling", { "read", 0 } },
  ntm_telex               = { "getChannels", "getSendingTexts", "getReceivingText" },
  radio_torch             = { "getChannel", "getPolling", "getCustomMap", "getCustomMapValues" },
  -- RBMK
  rbmk_crane       = { "getRodInfo", "getDepletion", "getXenonPoison", "getCranePos" },
  rbmk_boiler      = { "getCoordinates", "getHeat", "getSteam", "getSteamMax", "getWater", "getWaterMax", "getInfo", "getSteamType" },
  rbmk_console     = { { "getColumnData", 7, 7 }, "getRBMKPos" },
  rbmk_control_rod = { "getCoordinates", "getLevel", "getTargetLevel", "getHeat", "getInfo", "getColor" },
  rbmk_cooler      = { "getCoordinates", "getHeat", "getCoolant", "getInfo" },
  rbmk_gauge       = { { "getGaugeInfo", 1 } },
  rbmk_graph       = { { "getGraphInfo", 1 }, { "getGraphMin", 1 }, { "getGraphMax", 1 }, { "getGraphAvg", 1 } },
  rbmk_heater      = { "getCoordinates", "getHeat", "getFill", "getFillMax", "getExport", "getExportMax", "getFillType", "getExportType", "getInfo" },
  rbmk_indicator   = { { "getIndicatorInfo", 1 } },
  rbmk_keypad      = { { "getKeyInfo", 1 }, { "getKeyPressed", 1 } },
  rbmk_lever       = { { "getLeverInfo", 1 } },
  rbmk_numitron    = { { "getDisplayInfo", 1 } },
  rbmk_outgasser   = { "getCoordinates", "getGas", "getGasMax", "getGasType", "getProgress", "getCrafting", "getInfo" },
  rbmk_fuel_rod    = { "getCoordinates", "getHeat", "getFluxQuantity", "getFluxRatio", "getDepletion", "getXenonPoison",
                       "getCoreHeat", "getSkinHeat", "getType", "getInfo", "getModerated" },
  rbmk_terminal    = { "isOCMode", "readInput", "getAllHistory" },
  -- storage
  capacitor                 = { "getEnergy", "getMaxEnergy", "getEnergySent", "getEnergyReceived", "getInfo" },
  ntm_energy_storage        = { "getModeInfo", "getEnergyInfo", "getPackInfo", "getInfo" },
  ntm_energy_storage_legacy = { "getEnergyInfo", "getInfo" },
  ntm_fluid_tank            = { "getFluidStored", "getMaxStored", "getTypeStored", "getInfo" },
  ntm_mass_storage          = { "getFill", "getCapacity", "getType", "getOutputMode" },
  -- turrets (the peripheral is on the bottom face)
  ntm_turret    = { "isActive", "getEnergyInfo", "getWhitelisted", "getTargeting", "hasTarget", "getAngle", "isAligned", "getPos" },
  ntm_artillery = { "isActive", "getEnergyInfo", "getWhitelisted", "getTargeting", "hasTarget", "getAngle", "isAligned", "getPos",
                    "getCurrentTarget", { "getTargetDistance", 0, 64, 0 } },
}

local MAX_LEN = 200 -- one result line is cut to this many characters

local only = ...
local logLines = {}
local okCount, errCount, naCount, ntmCount, otherCount = 0, 0, 0, 0, 0
local color = term.isColor and term.isColor()

local function out(text, col)
  logLines[#logLines + 1] = text
  if color and col then term.setTextColor(col) end
  print(text)
  if color and col then term.setTextColor(colors.white) end
end

-- one-line rendering of any Lua value (tables flattened, nested ones too)
local function flat(v, depth)
  depth = depth or 0
  local t = type(v)
  if t == "string" then return string.format("%q", v) end
  if t ~= "table" then return tostring(v) end
  if depth > 4 then return "{...}" end
  if textutils and textutils.serialize then
    local ok, s = pcall(textutils.serialize, v, { compact = true, allow_repetitions = true })
    if ok and s then return (s:gsub("%s*\n%s*", " ")) end
  end
  local parts, n = {}, #v
  for i = 1, n do parts[#parts + 1] = flat(v[i], depth + 1) end
  for k, x in pairs(v) do
    if not (type(k) == "number" and k >= 1 and k <= n and k % 1 == 0) then
      parts[#parts + 1] = tostring(k) .. "=" .. flat(x, depth + 1)
    end
  end
  return "{" .. table.concat(parts, ",") .. "}"
end

local function results(packed)
  if packed.n == 0 then return "(nothing)" end
  local parts = {}
  for i = 1, packed.n do parts[i] = flat(packed[i]) end
  local s = table.concat(parts, ", ")
  if #s > MAX_LEN then s = s:sub(1, MAX_LEN - 3) .. "..." end
  return s
end

local function callLabel(entry)
  if type(entry) == "string" then return entry, {} end
  local args = {}
  for i = 2, #entry do args[#args + 1] = entry[i] end
  local shown = {}
  for i, a in ipairs(args) do shown[i] = tostring(a) end
  return entry[1] .. "(" .. table.concat(shown, ",") .. ")", args
end

local function methodSet(name)
  local set = {}
  local ok, list = pcall(peripheral.getMethods, name)
  if ok and type(list) == "table" then
    for _, m in ipairs(list) do set[m] = true end
  end
  return set
end

local function scan(name)
  local types = { peripheral.getType(name) }
  local getters, ptype
  for _, ty in ipairs(types) do
    if GETTERS[ty] then getters, ptype = GETTERS[ty], ty break end
  end
  if not getters then
    otherCount = otherCount + 1
    out(("-- %s [%s] not an NTM peripheral, skipped"):format(name, table.concat(types, ",")), colors and colors.lightGray)
    return
  end
  ntmCount = ntmCount + 1
  out(("== %s [%s]"):format(name, ptype), colors and colors.yellow)
  local exposed = methodSet(name)
  local listed = {}
  for _, entry in ipairs(getters) do
    local method = type(entry) == "table" and entry[1] or entry
    listed[method] = true
    local label, args = callLabel(entry)
    if not exposed[method] then
      naCount = naCount + 1
      out(("  n/a %s"):format(label), colors and colors.lightGray)
    else
      local packed = table.pack(pcall(peripheral.call, name, method, table.unpack(args)))
      if packed[1] then
        okCount = okCount + 1
        out(("  OK  %s = %s"):format(label, results(table.pack(table.unpack(packed, 2, packed.n)))), colors and colors.lime)
      else
        errCount = errCount + 1
        out(("  ERR %s: %s"):format(label, tostring(packed[2])), colors and colors.red)
      end
    end
  end
  -- methods the block exposes that the table does not cover (setters/actions are expected here)
  local extra = {}
  for m in pairs(exposed) do if not listed[m] then extra[#extra + 1] = m end end
  table.sort(extra)
  if #extra > 0 then out("  not called: " .. table.concat(extra, " "), colors and colors.gray) end
end

local names
if only then
  if not peripheral.isPresent(only) then
    printError("No peripheral named '" .. only .. "'. Attached: " .. table.concat(peripheral.getNames(), ", "))
    return
  end
  names = { only }
else
  names = peripheral.getNames()
  table.sort(names)
end

out(("ntm_scan: %d peripheral(s): %s"):format(#names, table.concat(names, ", ")))
for _, name in ipairs(names) do
  local ok, err = pcall(scan, name)
  if not ok then
    errCount = errCount + 1
    out(("  ERR scanning %s: %s"):format(name, tostring(err)), colors and colors.red)
  end
end

out(("== summary: NTM %d, other %d | OK %d, ERR %d, n/a %d"):format(ntmCount, otherCount, okCount, errCount, naCount),
  errCount == 0 and colors and colors.lime or colors and colors.red)

local dir = fs.getDir(shell and shell.getRunningProgram() or "")
local logPath = fs.combine(dir, "ntm_scan.log")
local f = fs.open(logPath, "w")
if f then
  f.write(table.concat(logLines, "\n") .. "\n")
  f.close()
  print("Log: " .. logPath)
end
