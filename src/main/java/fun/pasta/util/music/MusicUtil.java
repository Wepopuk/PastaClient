package fun.pasta.util.music;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfoKt;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public final class MusicUtil {

    private static final List<String> OWNER_PRIORITY = Arrays.asList(
            "SPOTIFY",
            "CHROME",
            "YANDEX",
            "DISCORD",
            "VK",
            "AIMP",
            "VLC",
            "APPLE",
            "EDGE"
    );

    private static final MediaPlayerInfo SYSTEM_INFO = initSystemInfo();

    private MusicUtil() {
    }

    public static TrackData getCurrentTrack() {
        MediaPlayerInfo info = SYSTEM_INFO;
        if (info == null) {
            return TrackData.empty();
        }

        List<IMediaSession> sessions;
        try {
            sessions = info.getMediaSessions();
        } catch (Throwable ignored) {
            return TrackData.empty();
        }

        if (sessions == null || sessions.isEmpty()) {
            return TrackData.empty();
        }

        IMediaSession session = pickSession(sessions);
        if (session == null) {
            return TrackData.empty();
        }

        MediaInfo media;
        try {
            media = session.getMedia();
        } catch (Throwable ignored) {
            return TrackData.empty();
        }

        if (media == null) {
            return TrackData.empty();
        }

        String title = normalizeText(media.getTitle(), "No track");
        String artist = normalizeText(media.getArtist(), "Unknown artist");
        int durationSeconds = toSeconds(media.getDuration());
        int positionSeconds = toSeconds(media.getPosition());
        if (durationSeconds > 0 && positionSeconds > durationSeconds) {
            positionSeconds = durationSeconds;
        }

        BufferedImage artwork = media.getArtwork();
        boolean playing = media.getPlaying();
        String owner = session.getOwner() == null ? "" : session.getOwner();

        return new TrackData(title, artist, durationSeconds, positionSeconds, artwork, playing, owner);
    }

    private static MediaPlayerInfo initSystemInfo() {
        try {
            return MediaPlayerInfoKt.getSystemMediaPlayerInfo();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IMediaSession pickSession(List<IMediaSession> sessions) {
        IMediaSession best = null;
        boolean bestPlaying = false;
        int bestPriority = Integer.MAX_VALUE;

        for (IMediaSession session : sessions) {
            if (session == null) {
                continue;
            }

            MediaInfo media;
            try {
                media = session.getMedia();
            } catch (Throwable ignored) {
                continue;
            }

            if (media == null) {
                continue;
            }

            String title = media.getTitle();
            String artist = media.getArtist();
            if ((title == null || title.isEmpty()) && (artist == null || artist.isEmpty())) {
                continue;
            }

            boolean playing = media.getPlaying();
            int priority = getOwnerPriorityIndex(session);

            if (best == null || (playing && !bestPlaying) || (playing == bestPlaying && priority < bestPriority)) {
                best = session;
                bestPlaying = playing;
                bestPriority = priority;
            }
        }

        return best;
    }

    private static int getOwnerPriorityIndex(IMediaSession session) {
        String owner = session.getOwner();
        if (owner == null) {
            return OWNER_PRIORITY.size();
        }
        String upper = owner.toUpperCase();
        for (int i = 0; i < OWNER_PRIORITY.size(); i++) {
            if (upper.contains(OWNER_PRIORITY.get(i))) {
                return i;
            }
        }
        return OWNER_PRIORITY.size();
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private static int toSeconds(long value) {
        if (value <= 0) {
            return 0;
        }
        if (value > 1_000_000_000L) {
            return (int) (value / 10_000_000L);
        }
        if (value > 10_000_000L) {
            return (int) (value / 1_000_000L);
        }
        if (value > 100_000L) {
            return (int) (value / 1_000L);
        }
        return (int) value;
    }

    public static final class TrackData {

        public final String title;
        public final String artist;
        public final int durationSeconds;
        public final int positionSeconds;
        public final BufferedImage artwork;
        public final boolean playing;
        public final String owner;

        public TrackData(String title, String artist, int durationSeconds, int positionSeconds,
                         BufferedImage artwork, boolean playing, String owner) {
            this.title = title;
            this.artist = artist;
            this.durationSeconds = durationSeconds;
            this.positionSeconds = positionSeconds;
            this.artwork = artwork;
            this.playing = playing;
            this.owner = owner;
        }

        public static TrackData empty() {
            return new TrackData("No track", "Unknown artist", 0, 0, null, false, "");
        }
    }
}
