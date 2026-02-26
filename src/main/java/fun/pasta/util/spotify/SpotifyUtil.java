package fun.pasta.util.spotify;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListenerAdapter;
import de.labystudio.spotifyapi.model.Track;

public final class SpotifyUtil {

    private static final SpotifyAPI API = SpotifyAPIFactory.create();
    private static volatile Track currentTrack;
    private static volatile boolean playing;
    private static volatile int currentPosition;
    private static volatile boolean initialized;
    private static volatile boolean initFailed;

    static {
        try {
            API.registerListener(new SpotifyListenerAdapter() {
                @Override
                public void onTrackChanged(Track track) {
                    currentTrack = track;
                }

                @Override
                public void onPositionChanged(int position) {
                    currentPosition = position;
                }

                @Override
                public void onPlayBackChanged(boolean isPlaying) {
                    playing = isPlaying;
                }

                @Override
                public void onDisconnect(Exception exception) {
                    playing = false;
                }
            });
            API.initialize();
            if (API.hasTrack()) {
                currentTrack = API.getTrack();
            }
            if (API.hasPosition()) {
                currentPosition = API.getPosition();
            }
            playing = API.isPlaying();
            initialized = true;
        } catch (Throwable e) {
            initFailed = true;
        }
    }

    private SpotifyUtil() {
    }

    public static boolean isInitialized() {
        return initialized && !initFailed;
    }

    public static Track getCurrentTrack() {
        return currentTrack;
    }

    public static int getCurrentPosition() {
        return currentPosition;
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static boolean hasTrack() {
        return currentTrack != null;
    }

    public static void refresh() {
        if (!isInitialized()) {
            return;
        }
        if (API.hasTrack()) {
            currentTrack = API.getTrack();
        }
        if (API.hasPosition()) {
            currentPosition = API.getPosition();
        }
        playing = API.isPlaying();
    }
}
