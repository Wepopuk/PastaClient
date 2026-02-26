package fun.pasta.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HitSound extends Module {

    private static final double KILL_RADIUS = 25.0D;
    private static final double KILL_RADIUS_SQ = KILL_RADIUS * KILL_RADIUS;
    private static final ResourceLocation HIT_SOUND = new ResourceLocation("pasta/sound/hit/hit.wav");
    private static final ResourceLocation KILL_SOUND = new ResourceLocation("pasta/sound/hit/kill.wav");
    private final BooleanOption hitSound = new BooleanOption("Hit Sound", true);
    private final BooleanOption killSound = new BooleanOption("Kill Sound", true);
    private static final ExecutorService SOUND_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "pasta-hitsound");
        thread.setDaemon(true);
        return thread;
    });

    public HitSound() {
        super("HitSound", Category.MISC);
        add(hitSound, killSound);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (hitSound.get() && event.isSend() && event.packet instanceof CUseEntityPacket packet
                && packet.getAction() == CUseEntityPacket.Action.ATTACK) {
            Entity target = packet.getEntityFromWorld(mc.world);
            if (target instanceof PlayerEntity && target != mc.player) {
                play(HIT_SOUND);
            }
            return;
        }

        if (killSound.get() && event.isreceive() && event.packet instanceof SEntityStatusPacket packet && packet.getOpCode() == 3) {
            Entity entity = packet.getEntity(mc.world);
            if (!(entity instanceof PlayerEntity) || entity == mc.player) {
                return;
            }
            if (mc.player.getDistanceSq(entity) <= KILL_RADIUS_SQ) {
                play(KILL_SOUND);
            }
        }
    }

    private void play(ResourceLocation location) {
        SOUND_EXECUTOR.execute(() -> {
            try (IResource resource = mc.getResourceManager().getResource(location);
                 BufferedInputStream input = new BufferedInputStream(resource.getInputStream());
                 AudioInputStream audio = AudioSystem.getAudioInputStream(input)) {
                Clip clip = AudioSystem.getClip();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                        clip.close();
                    }
                });
                clip.open(audio);
                clip.start();
            } catch (Exception ignored) {
            }
        });
    }
}
