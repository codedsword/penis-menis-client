package dev.sinepsinem.manager;

import dev.sinepsinem.event.Stage;
import dev.sinepsinem.event.impl.entity.player.MovementInputEvent;
import dev.sinepsinem.event.impl.entity.player.TravelEvent;
import dev.sinepsinem.event.impl.entity.player.UpdateWalkingPlayerEvent;
import dev.sinepsinem.event.system.Subscribe;
import dev.sinepsinem.features.modules.movement.MovementFixModule;
import dev.sinepsinem.util.MovementUtil;
import dev.sinepsinem.util.models.Angles;
import dev.sinepsinem.util.traits.Util;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Silent rotations: the camera never moves, the requested angles are only applied while the
 * movement packet is built (and, with {@link MovementFixModule}, while the player travels).
 * <p>
 * Modules call {@link #rotate} every tick they want to rotate, the highest priority request of
 * the tick wins. The callback runs right after the rotation has been sent to the server.
 */
public class RotationManager implements Util {
    private @Nullable Request request;
    private @Nullable Angles serverAngles;

    private boolean updateRender;
    private Angles renderSnapshot, renderSnapshot0;

    private @Nullable Angles travelSnapshot;
    private @Nullable Angles packetSnapshot;

    public RotationManager() {
        EVENT_BUS.register(this);
    }

    public void rotate(Angles angles, int priority, @Nullable Runnable afterSent) {
        if (request == null || priority >= request.priority()) {
            request = new Request(new Angles(angles.yRot(), Mth.clamp(angles.xRot(), -90f, 90f)), priority, afterSent);
        }
    }

    public void rotate(Angles angles, int priority) {
        rotate(angles, priority, null);
    }

    public boolean isRotating() {
        return request != null;
    }

    /**
     * @return the angles the server currently believes we have
     */
    public Angles getServerAngles() {
        if (serverAngles == null) {
            return new Angles(mc.player.getYRot(), mc.player.getXRot());
        }
        return serverAngles;
    }

    @Subscribe(priority = 1000)
    public void onTravelPre(TravelEvent.Pre event) {
        MovementFixModule fix = MovementFixModule.getInstance();
        if (request == null || fix == null || !fix.isEnabled()) return;

        travelSnapshot = new Angles(mc.player.getYRot(), mc.player.getXRot());
        mc.player.setYRot(request.angles().yRot());
        mc.player.setXRot(request.angles().xRot());
    }

    @Subscribe
    public void onMovementInput(MovementInputEvent event) {
        MovementFixModule fix = MovementFixModule.getInstance();
        if (travelSnapshot == null || fix == null || fix.getMode() != MovementFixModule.Mode.CLASSIC) return;
        // keep walking where the camera points even though the player is rotated
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
            if (request == null) {
                updateRenderSnapshot(new Angles(mc.player.getYRot(), mc.player.getXRot()), false);
                serverAngles = null;
                return;
            }

            Angles angles = request.angles();
            updateRenderSnapshot(angles, true);
            packetSnapshot = new Angles(mc.player.getYRot(), mc.player.getXRot());
            mc.player.setYRot(angles.yRot());
            mc.player.setXRot(angles.xRot());
            serverAngles = angles;
        } else {
            if (packetSnapshot != null) {
                mc.player.setYRot(packetSnapshot.yRot());
                mc.player.setXRot(packetSnapshot.xRot());
                packetSnapshot = null;
            }
            Request sent = request;
            request = null;
            if (sent != null && sent.afterSent() != null) {
                sent.afterSent().run();
            }
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

    private record Request(Angles angles, int priority, @Nullable Runnable afterSent) {
    }
}
