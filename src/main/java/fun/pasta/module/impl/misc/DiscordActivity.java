package fun.pasta.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.util.NotificationManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class DiscordActivity extends Module {

    private static final String APPLICATION_ID = "1266715470082478130";

    private static final String DETAILS = "Version: 1.01";
    private static final String STATE = "UID: 1488";
    private static final String LARGE_IMAGE_KEY = "pasta_main";
    private static final String LARGE_IMAGE_TEXT = "PastaClient";
    private static final String BUTTON_BUY_LABEL = "\u041a\u0443\u043f\u0438\u0442\u044c";
    private static final String BUTTON_BUY_URL = "https://discord.com/users/1258369647292583976";
    private static final String BUTTON_DISCORD_LABEL = "Discord";
    private static final String BUTTON_DISCORD_URL = "https://discord.gg/NCPNuNsBBW";

    private static final String SMALL_REALLYWORLD = "reallyworld";
    private static final String SMALL_FUNTIME = "funtime";
    private static final String SMALL_BRAVOHVH = "bravohvh";
    private static final String SMALL_HOLYWORLD = "holyworld";
    private static final String SMALL_DEFAULT = "server_default";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final BooleanOption debugLogs = new BooleanOption("Debug", true);
    private final BooleanOption debugNotify = new BooleanOption("Debug Notify", true);
    private final DiscordIpcClient ipcClient = new DiscordIpcClient(APPLICATION_ID);
    private final Object logLock = new Object();

    private long startTimestamp;
    private long lastUpdateMs;
    private long lastConnectAttemptMs;
    private long lastNotifyMs;
    private long lastDebugNotifyMs;
    private long lastPresenceSentMs;
    private String lastPresencePayload;
    private File debugFile;

    public DiscordActivity() {
        super("DiscordActivity", Category.MISC);
        add(debugLogs, debugNotify);
        ipcClient.setDebugSink(this::debug);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        startTimestamp = System.currentTimeMillis() / 1000L;
        lastUpdateMs = 0L;
        lastConnectAttemptMs = 0L;
        lastNotifyMs = 0L;
        lastDebugNotifyMs = 0L;
        lastPresenceSentMs = 0L;
        lastPresencePayload = "";
        debugFile = mc != null ? new File(mc.gameDir, "discord_activity_debug.log") : new File("discord_activity_debug.log");
        ipcClient.close();

        debug("=== ENABLE ===");
        debug("appId=" + APPLICATION_ID + ", pid=" + ipcClient.getProcessId());
        notifyDebug("DiscordActivity debug ON", true);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!ipcClient.isConnected() && now - lastConnectAttemptMs >= 5000L) {
            lastConnectAttemptMs = now;
            debug("connect attempt");

            boolean connected = ipcClient.connect();
            if (!connected) {
                String reason = ipcClient.getLastError();
                debug("connect failed: " + reason);
                if (now - lastNotifyMs >= 5000L) {
                    lastNotifyMs = now;
                    NotificationManager.push("Discord connect fail: " + shorten(reason, 46), false, 3000L);
                }
                return;
            }

            debug("connected to pipe #" + ipcClient.getConnectedPipeIndex() + " handshakeV=" + ipcClient.getLastHandshakeVersion());
            notifyDebug("Discord pipe connected #" + ipcClient.getConnectedPipeIndex(), true);
            lastUpdateMs = 0L;
        }

        if (!ipcClient.isConnected()) {
            return;
        }

        if (!ipcClient.isReady()) {
            if (now - ipcClient.getHandshakeSentAt() > 8000L) {
                debug("no READY for 8s, reconnect");
                ipcClient.close();
                return;
            }
            if (now - lastNotifyMs >= 5000L) {
                lastNotifyMs = now;
                NotificationManager.push("Discord RPC no READY", false, 2500L);
            }
        }

        if (now - lastUpdateMs < 1500L) {
            return;
        }

        lastUpdateMs = now;
        updatePresence();
    }

    @Override
    public void onDisable() {
        debug("disable -> clear + close");
        ipcClient.clearActivity();
        ipcClient.close();
        notifyDebug("DiscordActivity disabled", false);
        super.onDisable();
    }

    private void updatePresence() {
        String ip = getServerIp();
        ServerIcon icon = resolveIcon(ip);
        String activityJson = buildActivityJson(icon);
        long now = System.currentTimeMillis();

        boolean changed = !activityJson.equals(lastPresencePayload);
        if (!changed && now - lastPresenceSentMs < 15000L) {
            return;
        }

        boolean ok = ipcClient.setActivity(activityJson);
        if (!ok) {
            String reason = ipcClient.getLastError();
            debug("setActivity failed: " + reason);
            NotificationManager.push("Discord update fail: " + shorten(reason, 47), false, 3000L);
            return;
        }

        lastPresencePayload = activityJson;
        lastPresenceSentMs = now;

        long sent = ipcClient.getSentActivityCount();
        if (sent <= 3 || sent % 20 == 0) {
            debug("setActivity ok #" + sent + " icon=" + icon.key + " text=" + icon.text);
        }
    }

    private String buildActivityJson(ServerIcon icon) {
        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        sb.append("\"details\":\"").append(escapeJson(DETAILS)).append("\",");
        sb.append("\"state\":\"").append(escapeJson(STATE)).append("\",");
        sb.append("\"timestamps\":{\"start\":").append(startTimestamp).append("},");
        sb.append("\"assets\":{");
        sb.append("\"large_image\":\"").append(escapeJson(LARGE_IMAGE_KEY)).append("\",");
        sb.append("\"large_text\":\"").append(escapeJson(LARGE_IMAGE_TEXT)).append("\",");
        sb.append("\"small_image\":\"").append(escapeJson(icon.key)).append("\",");
        sb.append("\"small_text\":\"").append(escapeJson(icon.text)).append("\"},");
        // Discord IPC expects button objects: [{"label":"...","url":"..."}].
        sb.append("\"buttons\":[");
        sb.append("{\"label\":\"").append(escapeJson(BUTTON_BUY_LABEL)).append("\",\"url\":\"").append(escapeJson(BUTTON_BUY_URL)).append("\"},");
        sb.append("{\"label\":\"").append(escapeJson(BUTTON_DISCORD_LABEL)).append("\",\"url\":\"").append(escapeJson(BUTTON_DISCORD_URL)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }

    private String getServerIp() {
        if (mc == null || mc.getCurrentServerData() == null || mc.isSingleplayer()) {
            return "";
        }
        String ip = mc.getCurrentServerData().serverIP;
        return ip == null ? "" : ip.toLowerCase(Locale.ROOT);
    }

    private ServerIcon resolveIcon(String ip) {
        if (ip.contains("reallyworld")) {
            return new ServerIcon(SMALL_REALLYWORLD, "mc.reallyworld.me");
        }
        if (ip.contains("funtime")) {
            return new ServerIcon(SMALL_FUNTIME, "funtime.su");
        }
        if (ip.contains("bravohvh")) {
            return new ServerIcon(SMALL_BRAVOHVH, "mc.bravohvh.fun");
        }
        if (ip.contains("holyworld")) {
            return new ServerIcon(SMALL_HOLYWORLD, "holyworld");
        }
        if (!ip.isEmpty()) {
            return new ServerIcon(SMALL_DEFAULT, ip);
        }
        return new ServerIcon(SMALL_DEFAULT, "Singleplayer");
    }

    private String escapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        return out.toString();
    }

    private void notifyDebug(String message, boolean good) {
        if (!debugNotify.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDebugNotifyMs < 1200L) {
            return;
        }
        lastDebugNotifyMs = now;
        NotificationManager.push(message, good, 2200L);
    }

    private String shorten(String value, int maxLen) {
        if (value == null) {
            return "unknown";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private void debug(String message) {
        if (!debugLogs.get()) {
            return;
        }
        String line = "[" + TIME_FORMAT.format(LocalDateTime.now()) + "] " + message;
        System.out.println("[DiscordActivity] " + message);

        File file = debugFile != null ? debugFile : new File("discord_activity_debug.log");
        synchronized (logLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(line);
                writer.newLine();
            } catch (IOException ignored) {
            }
        }
    }

    private static final class ServerIcon {
        private final String key;
        private final String text;

        private ServerIcon(String key, String text) {
            this.key = key;
            this.text = text;
        }
    }

    private static final class DiscordIpcClient {
        private static final int OPCODE_HANDSHAKE = 0;
        private static final int OPCODE_FRAME = 1;
        private static final int OPCODE_CLOSE = 2;
        private static final int OPCODE_PING = 3;
        private static final int OPCODE_PONG = 4;

        private final String clientId;
        private final Object writeLock = new Object();

        private RandomAccessFile pipe;
        private Thread readerThread;
        private volatile boolean connected;
        private volatile boolean ready;
        private volatile boolean readerRunning;
        private long processId;
        private long sentActivityCount;
        private long handshakeSentAt;
        private int handshakeVersion = 1;
        private int lastHandshakeVersion = 1;
        private int connectedPipeIndex = -1;
        private String lastError = "not connected";
        private String lastFrame = "";
        private long lastFrameAt;
        private DebugSink debugSink;

        private DiscordIpcClient(String clientId) {
            this.clientId = clientId;
            this.processId = resolveProcessId();
        }

        private void setDebugSink(DebugSink debugSink) {
            this.debugSink = debugSink;
        }

        private void log(String text) {
            if (debugSink != null) {
                debugSink.log("[IPC] " + text);
            }
        }

        private boolean isConnected() {
            return connected && pipe != null;
        }

        private boolean isReady() {
            return ready;
        }

        private long getProcessId() {
            return processId;
        }

        private long getSentActivityCount() {
            return sentActivityCount;
        }

        private long getHandshakeSentAt() {
            return handshakeSentAt;
        }

        private int getLastHandshakeVersion() {
            return lastHandshakeVersion;
        }

        private int getConnectedPipeIndex() {
            return connectedPipeIndex;
        }

        private String getLastError() {
            return lastError;
        }

        private boolean connect() {
            if (clientId == null || clientId.trim().isEmpty()) {
                lastError = "empty client id";
                log(lastError);
                return false;
            }

            close();
            lastError = "no discord ipc pipe";

            for (int i = 0; i < 10; i++) {
                RandomAccessFile opened = tryOpenPipe(i);
                if (opened == null) {
                    continue;
                }

                pipe = opened;
                connectedPipeIndex = i;
                connected = true;
                ready = false;
                sentActivityCount = 0L;
                handshakeSentAt = 0L;
                lastFrame = "";
                lastFrameAt = 0L;

                try {
                    startReader();
                    int version = handshakeVersion;
                    handshakeVersion = version == 1 ? 0 : 1;
                    lastHandshakeVersion = version;
                    String handshake = "{\"v\":" + version + ",\"client_id\":\"" + clientId + "\"}";
                    writeFrame(OPCODE_HANDSHAKE, handshake);
                    handshakeSentAt = System.currentTimeMillis();
                    log("handshake v" + version + " sent on pipe #" + i + ", bytes=" + handshake.getBytes(StandardCharsets.UTF_8).length);
                    lastError = "";
                    return true;
                } catch (IOException e) {
                    lastError = "handshake failed pipe #" + i + ": " + shortError(e);
                    log(lastError);
                    close();
                }
            }

            log("connect failed: " + lastError);
            return false;
        }

        private RandomAccessFile tryOpenPipe(int index) {
            String first = "\\\\.\\pipe\\discord-ipc-" + index;
            try {
                RandomAccessFile raf = new RandomAccessFile(first, "rw");
                log("opened " + first);
                return raf;
            } catch (IOException e) {
                log("open fail " + first + " -> " + shortError(e));
                lastError = "open " + first + " failed: " + shortError(e);
            }

            String second = "\\\\?\\pipe\\discord-ipc-" + index;
            try {
                RandomAccessFile raf = new RandomAccessFile(second, "rw");
                log("opened " + second);
                return raf;
            } catch (IOException e) {
                log("open fail " + second + " -> " + shortError(e));
                lastError = "open " + second + " failed: " + shortError(e);
                return null;
            }
        }

        private boolean setActivity(String activityJson) {
            if (!isConnected()) {
                lastError = "setActivity while not connected";
                return false;
            }

            String nonce = UUID.randomUUID().toString();
            String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + processId + ",\"activity\":" + activityJson + "},\"nonce\":\"" + nonce + "\"}";
            try {
                writeFrame(OPCODE_FRAME, payload);
                sentActivityCount++;
                return true;
            } catch (IOException e) {
                lastError = "setActivity write failed: " + shortError(e);
                log(lastError);
                close();
                return false;
            }
        }

        private void clearActivity() {
            if (!isConnected()) {
                return;
            }
            String nonce = UUID.randomUUID().toString();
            String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + processId + ",\"activity\":null},\"nonce\":\"" + nonce + "\"}";
            try {
                writeFrame(OPCODE_FRAME, payload);
                log("clear activity sent");
            } catch (IOException e) {
                lastError = "clearActivity failed: " + shortError(e);
                log(lastError);
            }
        }

        private void writeFrame(int opcode, String json) throws IOException {
            if (pipe == null) {
                throw new IOException("pipe is null");
            }
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(opcode);
            header.putInt(data.length);
            synchronized (writeLock) {
                pipe.write(header.array());
                pipe.write(data);
            }
        }

        private void startReader() {
            if (readerThread != null && readerThread.isAlive()) {
                return;
            }
            readerRunning = true;
            readerThread = new Thread(() -> {
                log("reader started");
                while (readerRunning && pipe != null) {
                    try {
                        int hint = getAvailableLength();
                        if (hint >= 0 && hint < 8) {
                            Thread.sleep(15L);
                            continue;
                        }

                        int opcode = Integer.reverseBytes(pipe.readInt());
                        int len = Integer.reverseBytes(pipe.readInt());
                        if (len < 0 || len > 1_000_000) {
                            lastError = "invalid frame length: " + len;
                            log(lastError);
                            break;
                        }

                        if (hint >= 0) {
                            long waitStart = System.currentTimeMillis();
                            while (readerRunning && pipe != null) {
                                int current = getAvailableLength();
                                if (current < 0 || current >= len) {
                                    break;
                                }
                                if (System.currentTimeMillis() - waitStart > 1500L) {
                                    break;
                                }
                                Thread.sleep(5L);
                            }
                        }

                        byte[] payloadBytes = new byte[len];
                        pipe.readFully(payloadBytes);
                        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
                        lastFrame = payload;
                        lastFrameAt = System.currentTimeMillis();

                        if (payload.length() <= 260) {
                            log("recv opcode=" + opcode + " payload=" + payload);
                        } else {
                            log("recv opcode=" + opcode + " payload=" + payload.substring(0, 260) + "...");
                        }

                        if (payload.contains("\"evt\":\"READY\"")) {
                            ready = true;
                            log("READY received");
                        }

                        if (payload.contains("\"evt\":\"ERROR\"")) {
                            String message = extractJsonString(payload, "message");
                            String code = extractJsonNumber(payload, "code");
                            lastError = "discord ERROR code=" + code + " message=" + message;
                            log(lastError);
                        }

                        if (opcode == OPCODE_PING) {
                            try {
                                writeFrame(OPCODE_PONG, payload);
                                log("PONG sent");
                            } catch (IOException io) {
                                lastError = "pong failed: " + shortError(io);
                                log(lastError);
                            }
                        }
                        if (opcode == OPCODE_CLOSE) {
                            lastError = "discord close frame: " + shortenFrame(payload);
                            log(lastError);
                            break;
                        }
                    } catch (IOException e) {
                        if (readerRunning) {
                            lastError = "reader io error: " + shortError(e);
                            log(lastError);
                        }
                        break;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Throwable t) {
                        if (readerRunning) {
                            lastError = "reader error: " + shortError(t);
                            log(lastError);
                        }
                        break;
                    }
                }
                ready = false;
                connected = false;
                log("reader stopped");
            }, "discord-ipc-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        }

        private int getAvailableLength() {
            if (pipe == null) {
                return -1;
            }
            try {
                long len = pipe.length();
                if (len < 0L) {
                    return 0;
                }
                if (len > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                return (int) len;
            } catch (IOException ignored) {
                return -1;
            }
        }

        private String extractJsonString(String json, String key) {
            if (json == null || key == null) {
                return "";
            }
            String needle = "\"" + key + "\":\"";
            int start = json.indexOf(needle);
            if (start < 0) {
                return "";
            }
            start += needle.length();
            StringBuilder result = new StringBuilder();
            boolean escaped = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    result.append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    return result.toString();
                }
                result.append(c);
            }
            return result.toString();
        }

        private String extractJsonNumber(String json, String key) {
            if (json == null || key == null) {
                return "";
            }
            String needle = "\"" + key + "\":";
            int start = json.indexOf(needle);
            if (start < 0) {
                return "";
            }
            start += needle.length();
            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+') {
                    end++;
                } else {
                    break;
                }
            }
            if (end <= start) {
                return "";
            }
            return json.substring(start, end);
        }

        private String shortenFrame(String frame) {
            if (frame == null || frame.isEmpty()) {
                return "empty";
            }
            if (frame.length() <= 180) {
                return frame;
            }
            return frame.substring(0, 180) + "...";
        }

        private void close() {
            connected = false;
            ready = false;
            readerRunning = false;
            connectedPipeIndex = -1;
            handshakeSentAt = 0L;

            if (readerThread != null) {
                try {
                    readerThread.interrupt();
                } catch (Throwable ignored) {
                }
                readerThread = null;
            }

            if (pipe != null) {
                try {
                    pipe.close();
                } catch (IOException ignored) {
                } finally {
                    pipe = null;
                }
            }
        }

        private long resolveProcessId() {
            try {
                String runtime = ManagementFactory.getRuntimeMXBean().getName();
                int atIndex = runtime.indexOf('@');
                if (atIndex > 0) {
                    return Long.parseLong(runtime.substring(0, atIndex));
                }
            } catch (Throwable ignored) {
            }
            return 0L;
        }

        private String shortError(Throwable throwable) {
            if (throwable == null) {
                return "unknown";
            }
            String message = throwable.getMessage();
            if (message == null || message.isEmpty()) {
                return throwable.getClass().getSimpleName();
            }
            return throwable.getClass().getSimpleName() + ": " + message;
        }
    }

    @FunctionalInterface
    private interface DebugSink {
        void log(String text);
    }
}
