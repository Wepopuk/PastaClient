package fun.pasta.module.impl.visual.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.pasta.Pasta;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Font;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.music.MusicUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.util.shader.ShaderUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector4f;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlayerRender implements IRenderHud {

    private static final float SCALE = 2.0f;

    private static final float HUD_WIDTH = 285f / SCALE;
    private static final float HUD_HEIGHT = 80f / SCALE;
    private static final float HUD_RADIUS = 16f / SCALE;
    private static final float PADDING_LEFT = 14f / SCALE;
    private static final float PADDING_RIGHT = 18f / SCALE;
    private static final float GAP = 8f / SCALE;

    private static final float ART_SIZE = 60f / SCALE;
    private static final float ART_RADIUS = 9f / SCALE;

    private static final float TITLE_SIZE = 16f;
    private static final float ARTIST_SIZE = 11f;
    private static final float TIME_SIZE = 12f;
    private static final float TEXT_GAP = 2f;
    private static final float TIME_TOP_GAP = 3f / SCALE;
    private static final float BAR_TOP_GAP = 0.5f / SCALE;
    private static final float BAR_HEIGHT = 5f / SCALE;

    private static final long FETCH_INTERVAL_MS = 1000L;

    private static final int BACKGROUND_COLOR = ColorUtil.rgba(64, 59, 55, 179);
    private static final int TITLE_COLOR = ColorUtil.rgba(255, 255, 255, 255);
    private static final int ARTIST_COLOR = ColorUtil.rgba(135, 135, 135, 255);
    private static final int TIME_COLOR = ColorUtil.rgba(255, 255, 255, 255);
    private static final int BAR_BG = ColorUtil.rgba(35, 35, 35, 180);
    private static final int BAR_FG = ColorUtil.rgba(255, 251, 251, 255);
    private static final int ART_PLACEHOLDER = ColorUtil.rgba(45, 45, 45, 255);

    private final Dragging dragging;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean fetching;
    private volatile long lastFetch;

    private volatile MusicUtil.TrackData trackInfo = MusicUtil.TrackData.empty();
    private volatile ResourceLocation albumTexture;
    private volatile BufferedImage pendingArtwork;
    private volatile String trackKey = "";
    private float progressAnim;

    public MusicPlayerRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D e) {
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager
                .getModule("HUD");
        if (hud == null || !hud.enable || !hud.musicPlayer.get()) {
            return;
        }

        fetchIfNeeded();

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        boolean playingNow = trackInfo.playing;
        if (!playingNow && !chatOpen) {
            return;
        }

        float x = dragging.getX();
        float y = dragging.getY();
        dragging.setWidth(HUD_WIDTH);
        dragging.setHeight(HUD_HEIGHT);

        if (pendingArtwork != null) {
            BufferedImage artwork = pendingArtwork;
            pendingArtwork = null;
            updateTexture(artwork);
        }

        RenderUtil2D.drawRoundRect(x, y, HUD_WIDTH, HUD_HEIGHT, HUD_RADIUS, BACKGROUND_COLOR);

        float artX = x + PADDING_LEFT;
        float artY = y + (HUD_HEIGHT - ART_SIZE) / 2f;
        drawArtwork(artX, artY);

        float textX = artX + ART_SIZE + GAP;
        float textAreaWidth = Math.max(0f, x + HUD_WIDTH - PADDING_RIGHT - textX);

        Font titleFont = Fonts.sfpro.get(TITLE_SIZE);
        Font artistFont = Fonts.sfpro.get(ARTIST_SIZE);
        Font timeFont = Fonts.sfpro.get(TIME_SIZE);

        String title = trimToWidth(trackInfo.title, textAreaWidth, titleFont);
        String artist = trimToWidth(trackInfo.artist, textAreaWidth, artistFont);

        float titleY = artY;
        float artistY = titleY + titleFont.getHeight() + TEXT_GAP;
        float timeY = artistY + artistFont.getHeight() + TIME_TOP_GAP;

        titleFont.drawString(title, textX, titleY, TITLE_COLOR);
        artistFont.drawString(artist, textX, artistY, ARTIST_COLOR);

        int duration = trackInfo.durationSeconds;
        int position = trackInfo.positionSeconds;
        if (duration <= 0 || position < 0) {
            duration = 0;
            position = 0;
        }

        String timeLeft = formatTime(position);
        String timeRight = formatTime(duration);

        float timeRightWidth = timeFont.getWidth(timeRight);
        float timeRightX = x + HUD_WIDTH - PADDING_RIGHT - timeRightWidth;
        timeFont.drawString(timeLeft, textX, timeY, TIME_COLOR);
        timeFont.drawString(timeRight, timeRightX, timeY, TIME_COLOR);

        float barX = textX;
        float barY = timeY + timeFont.getHeight() + BAR_TOP_GAP;
        float barWidth = textAreaWidth;
        float barRadius = BAR_HEIGHT / 2f;

        RenderUtil2D.drawRoundRect(barX, barY, barWidth, BAR_HEIGHT, barRadius, BAR_BG);

        float progressTarget = duration > 0 ? (float) position / (float) duration : 0f;
        progressAnim = MathUtil.lerp(progressAnim, progressTarget, 6f);
        float progressWidth = barWidth * progressAnim;
        if (progressWidth > 0.5f) {
            float fillRadius = Math.min(barRadius, progressWidth / 2f);
            RenderUtil2D.drawRoundRect(barX, barY, progressWidth, BAR_HEIGHT, fillRadius, BAR_FG);
        }
    }

    private void fetchIfNeeded() {
        long now = System.currentTimeMillis();
        if (fetching || now - lastFetch < FETCH_INTERVAL_MS) {
            return;
        }
        fetching = true;
        lastFetch = now;
        executor.submit(() -> {
            try {
                MusicUtil.TrackData data = MusicUtil.getCurrentTrack();
                trackInfo = data;

                String newKey = data.title + "|" + data.artist + "|" + data.durationSeconds;
                if (!newKey.equals(trackKey) || (albumTexture == null && data.artwork != null)) {
                    trackKey = newKey;
                    if (data.artwork != null) {
                        pendingArtwork = data.artwork;
                    } else {
                        albumTexture = null;
                        pendingArtwork = null;
                    }
                }
            } catch (Throwable ignored) {
                trackInfo = MusicUtil.TrackData.empty();
                albumTexture = null;
                pendingArtwork = null;
            } finally {
                fetching = false;
            }
        });
    }

    private void drawArtwork(float x, float y) {
        if (albumTexture == null) {
            RenderUtil2D.drawRoundRect(x, y, ART_SIZE, ART_SIZE, ART_RADIUS, ART_PLACEHOLDER);
            return;
        }

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        float radius = Math.min(ART_RADIUS, ART_SIZE / 2f);
        Vector4f round = new Vector4f(radius, radius, radius, radius);
        ShaderUtil.roundedImage.attach();
        ShaderUtil.roundedImage.setUniform("size", ART_SIZE, ART_SIZE);
        ShaderUtil.roundedImage.setUniform(
                "round",
                round.x,
                round.y,
                round.z,
                round.w
        );
        ShaderUtil.roundedImage.setUniform("value", 0f);
        ShaderUtil.roundedImage.setUniform("smoothness", 0.f, 1.5f);
        ShaderUtil.roundedImage.setUniform("alpha", 1f);
        RenderUtil2D.drawImage(albumTexture, x, y, ART_SIZE, ART_SIZE, -1);
        ShaderUtil.roundedImage.detach();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void updateTexture(BufferedImage image) {
        try {
            NativeImage nativeImage = toNativeImage(image);
            DynamicTexture texture = new DynamicTexture(nativeImage);
            albumTexture = mc.getTextureManager().getDynamicTextureLocation("media_album", texture);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Media artwork arrives as ARGB, while NativeImage RGBA stores channels in ABGR int order.
     * Explicit conversion prevents red/blue channel swap.
     */
    private NativeImage toNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;

                nativeImage.setPixelRGBA(x, y, NativeImage.getCombined(a, b, g, r));
            }
        }

        return nativeImage;
    }

    private String trimToWidth(String text, float maxWidth, Font font) {
        if (text == null || text.isEmpty() || maxWidth <= 0f) {
            return "";
        }
        if (font.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        float ellipsisWidth = font.getWidth(ellipsis);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && font.getWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end > 0 ? text.substring(0, end) + ellipsis : "";
    }

    private String formatTime(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}
