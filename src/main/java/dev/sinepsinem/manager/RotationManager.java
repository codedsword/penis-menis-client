package dev.sinepsinem.manager;

import dev.sinepsinem.event.Stage;
import dev.sinepsinem.event.impl.entity.player.MovementInputEvent;
import dev.sinepsinem.event.impl.entity.player.TravelEvent;
import dev.sinepsinem.event.impl.entity.player.UpdateWalkingPlayerEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.movement.MovementFixModule;
import dev.sinepsinem.util.MovementUtil;
import dev.sinepsinem.util.RotationUtil;
import dev.sinepsinem.util.models.Angles;
import dev.sinepsinem.util.traits.Util;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Silent rotations that stay consistent with the server's movement simulation.
 * <ul>
 *     <li>Modules call {@link #rotate} during {@code TickEvent.Pre}; the highest priority request wins.</li>
 *     <li>The tick's rotation is used for movement (with input correction, see {@link MovementFixModule})
 *     and sent in the same tick's movement packet, which is exactly what the server simulates with.</li>
 *     <li>When nothing requests a rotation the server side aim eases back to the camera in mouse-like
 *     steps instead of snapping.</li>
 * </ul>
 * Actions that depend on a rotation (attacks, item uses) must happen on the tick after it was sent,
 * check {@link #getServerAngles()}.
 */
public class RotationManager implements Util {
    private static final float RETURN_SPEED = 45.0f;

    private @Nullable Request request;
    // rotation the server holds, null while it matches the camera
    private @Nullable Angles serverAngles;
    // rotation used for this tick's movement and packet
    private @Nullable Angles tickAngles;

    private boolean updateRender;
    private Angles renderSnapshot, renderSnapshot0;

    private @Nullable Angles travelSnapshot;
    private @Nullable Angles packetSnapshot;

    public RotationManager() {
        EVENT_BUS.register(this);
    }

    public void rotate(Angles angles, int priority) {
        if (request == null || priority >= request.priority()) {
            request = new Request(new Angles(angles.yRot(), Mth.clamp(angles.xRot(), -90f, 90f)), priority);
        }
    }

    public boolean isRotating() {
        return serverAngles != null;
    }

    /**
     * @return the rotation the server currently has for us, i.e. the one from the last movement packet
     */
    public Angles getServerAngles() {
        return serverAngles != null ? serverAngles : cameraAngles();
    }

    private Angles cameraAngles() {
        return new Angles(mc.player.getYRot(), mc.player.getXRot());
    }

    // TickEvent.Pre has finished by now, so every module had its chance to request
    @Subscribe(priority = 1000)
    public void onTravelPre(TravelEvent.Pre event) {
        tickAngles = resolveTickAngles();
        request = null;
        if (tickAngles == null) return;

        travelSnapshot = cameraAngles();
        mc.player.setYRot(tickAngles.yRot());
        mc.player.setXRot(tickAngles.xRot());
    }

    private @Nullable Angles resolveTickAngles() {
        if (request != null) return request.angles();
        if (serverAngles == null) return null;

        Angles camera = cameraAngles();
        float yawDiff = Math.abs(Mth.wrapDegrees(camera.yRot() - serverAngles.yRot()));
        float pitchDiff = Math.abs(camera.xRot() - serverAngles.xRot());
        if (yawDiff <= RETURN_SPEED && pitchDiff <= RETURN_SPEED) {
            // close enough to hand back to the camera in one normal sized mouse movement
            return null;
        }
        return RotationUtil.smooth(serverAngles, camera, RETURN_SPEED, RETURN_SPEED);
    }

    @Subscribe
    public void onMovementInput(MovementInputEvent event) {
        if (travelSnapshot == null) return;
        MovementFixModule fix = MovementFixModule.getInstance();
        // without correction the server simulates our keys with a yaw we did not move with
        if (fix != null && fix.isEnabled() && fix.getMode() == MovementFixModule.Mode.PENIS) return;
        MovementUtil.correctInput(event.getInput(), travelSnapshot.yRot(), mc.player.getYRot());
    }

    @Subscribe(priority = -1000)
    public void onTravelPost(TravelEvent.Post event) {
        if (travelSnapshot == null) return;
        mc.player.setYRot(travelSnapshot.yRot());
        mc.player.setXRot(travelSnapshot.xRot());
        // baseTick copied the fake angles into the previous rotation, which would make the camera jitter
        mc.player.yRotO = travelSnapshot.yRot();
        mc.player.xRotO = travelSnapshot.xRot();
        travelSnapshot = null;
    }

    @Subscribe
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (event.getStage() == Stage.PRE) {
            if (tickAngles == null) {
                updateRenderSnapshot(cameraAngles(), false);
                serverAngles = null;
                return;
            }

            updateRenderSnapshot(tickAngles, true);
            packetSnapshot = cameraAngles();
            mc.player.setYRot(tickAngles.yRot());
            mc.player.setXRot(tickAngles.xRot());
            serverAngles = tickAngles;
        } else if (packetSnapshot != null) {
            mc.player.setYRot(packetSnapshot.yRot());
            mc.player.setXRot(packetSnapshot.xRot());
            packetSnapshot = null;
        }
    }

    /**
     * Runs {@code action} with the player temporarily rotated, for packets that carry the
     * rotation themselves (using items).
     */
    public void withRotation(Angles angles, Runnable action) {
        float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
        mc.player.setYRot(angles.yRot());
        mc.player.setXRot(angles.xRot());
        try {
            action.run();
        } finally {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }
    }

    public Angles getLerpRenderSnapshot(float lerp) {
        if (!updateRender || renderSnapshot0 == null) return null;
        return new Angles(
                Mth.rotLerp(lerp, renderSnapshot0.yRot(), renderSnapshot.yRot()),
                Mth.lerp(lerp, renderSnapshot0.xRot(), renderSnapshot.xRot())
        );
    }

    public void sendDuplicate(float yaw, float pitch) {
        mc.player.positionReminder = 20;
        mc.player.yRotLast = yaw;
        mc.player.xRotLast = pitch;
        mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                yaw, pitch,
                mc.player.onGround(), mc.player.horizontalCollision
        ));
    }

    private void updateRenderSnapshot(Angles angles, boolean update) {
        this.renderSnapshot0 = this.renderSnapshot == null ? angles : this.renderSnapshot;
        this.renderSnapshot = angles;
        this.updateRender = update;
    }

    private record Request(Angles angles, int priority) {
    }
}
