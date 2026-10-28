-- Example script. Load it with ".script load example", edit it, then ".script reload example".
-- The module shows up in the ClickGui's Scripts panel like any other module.

local module = modules.register("Script Demo", "Shows what the scripting API can do")
local Minecraft = java.import("net.minecraft.client.Minecraft")
local mc = Minecraft.getInstance()

-- settings appear under the module in the ClickGui
local announce = module:bool("Announce", true)
local threshold = module:number("Speed", 5.0, 1.0, 20.0)

module:on("enable", function()
    print("Demo enabled! User: " .. client.username())
end)

local ticks = 0
module:on("game_tick", function(event)
    ticks = ticks + 1
    local player = mc.player
    local move = player.input:getMoveVector()
    -- once a second while walking, debug() goes to the log file
    if ticks % 20 == 0 and player:onGround() and (move.x ~= 0 or move.y ~= 0) then
        local pos = player:position()
        debug("Position: " .. pos.x .. ", " .. pos.y .. ", " .. pos.z)
    end
end)

module:on("render_2d", function(event)
    local player = mc.player
    local dx = player:getX() - player.xo
    local dz = player:getZ() - player.zo
    local speed = math.sqrt(dx * dx + dz * dz) * 20
    local color = speed > threshold:get() and 0xFFFF0086 or 0xFFFFFFFF
    client.text(event, string.format("%.2f b/s", speed), 4, 40, color)
end)

module:on("attack", function(target)
    if announce:get() then
        print("Hit " .. target:getName():getString())
    end
end)

module:on("disable", function()
    print("Demo disabled!")
end)
