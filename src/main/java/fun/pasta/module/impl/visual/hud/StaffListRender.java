package fun.pasta.module.impl.visual.hud;

import fun.pasta.Pasta;
import fun.pasta.drag.Dragging;
import fun.pasta.event.EventRender2D;
import fun.pasta.fonts.Fonts;
import fun.pasta.module.impl.visual.hud.impl.IRenderHud;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.math.MathUtil;
import fun.pasta.util.render.RenderUtil2D;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class StaffListRender implements IRenderHud {

    private static final float PILL_HEIGHT = 14f;
    private static final float PILL_RADIUS = 7f;
    private static final float GAP = 1f;
    private static final float TEXT_SIZE = 13f;
    private static final float HEADER_MIN_WIDTH = 56f;
    private static final float HEADER_PADDING_X = 10f;
    private static final float ITEM_PADDING_X = 5f;
    private static final float ITEM_EXTRA_WIDTH = 16f;
    private static final float HEADER_OFFSET_X = 6f;

    private static final int HEADER_COLOR = ColorUtil.rgba(64, 59, 55, 200);
    private static final int ITEM_COLOR = ColorUtil.rgba(64, 59, 55, 150);
    private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 255);
    private static final int VANISH_TEXT_COLOR = ColorUtil.rgba(255, 120, 120, 255);
    private static final String STAFF_PREFIX_TOKENS =
            "mod|moder|moder\\+|adm|admin|ml\\.admin|ml\\.moder|st\\.moder|gl\\.moder|helper|d\\.helper|d\\.help|help|"
                    + "\\u0445\\u0435\\u043b\\u043f|\\u0430\\u0434\\u043c";

    private final Dragging dragging;
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(" + STAFF_PREFIX_TOKENS + ").*", Pattern.CASE_INSENSITIVE);

    private float renderX;
    private float renderY;
    private float width;
    private float height;
    private boolean positionInitialized;

    public StaffListRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D e) {
        fun.pasta.module.impl.visual.Hud hud = (fun.pasta.module.impl.visual.Hud) Pasta.instance.manager.getModule("HUD");
        if (hud == null || !hud.enable || !hud.staffList.get()) {
            return;
        }

        setPosition();

        List<StaffLine> lines = collectStaffLines();
        float headerTextWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth("Staff List");
        float headerWidth = Math.max(HEADER_MIN_WIDTH, headerTextWidth + HEADER_PADDING_X * 2f);

        float maxItemWidth = 0f;
        for (StaffLine line : lines) {
            float lineWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(line.text) + ITEM_PADDING_X * 2f + ITEM_EXTRA_WIDTH;
            if (lineWidth > maxItemWidth) {
                maxItemWidth = lineWidth;
            }
        }

        width = Math.max(maxItemWidth, headerWidth + HEADER_OFFSET_X);
        height = PILL_HEIGHT;
        if (!lines.isEmpty()) {
            height += GAP + lines.size() * PILL_HEIGHT + (lines.size() - 1) * GAP;
        }

        dragging.setWidth(width);
        dragging.setHeight(height);

        drawPill(renderX + HEADER_OFFSET_X, renderY, headerWidth, HEADER_COLOR, "Staff List", TEXT_COLOR);

        float startY = renderY + PILL_HEIGHT + (lines.isEmpty() ? 0f : GAP);
        for (int i = 0; i < lines.size(); i++) {
            StaffLine line = lines.get(i);
            float pillY = startY + i * (PILL_HEIGHT + GAP);
            float pillWidth = Fonts.sfpro.get(TEXT_SIZE).getWidth(line.text) + ITEM_PADDING_X * 2f + ITEM_EXTRA_WIDTH;
            drawPill(renderX, pillY, pillWidth, ITEM_COLOR, line.text, line.vanished ? VANISH_TEXT_COLOR : TEXT_COLOR);
        }
    }

    private List<StaffLine> collectStaffLines() {
        List<StaffLine> result = new ArrayList<>();
        if (mc.player == null || mc.world == null || mc.getConnection() == null || mc.world.getScoreboard() == null) {
            return result;
        }

        Set<String> tabPlayers = new HashSet<>();
        for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null || info.getGameProfile().getName() == null) {
                continue;
            }
            tabPlayers.add(info.getGameProfile().getName().toLowerCase(Locale.ROOT));
        }

        Set<String> added = new HashSet<>();
        List<ScorePlayerTeam> teams = new ArrayList<>(mc.world.getScoreboard().getTeams());
        teams.sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));

        for (ScorePlayerTeam team : teams) {
            Collection<String> members = team.getMembershipCollection();
            if (members == null || members.isEmpty()) {
                continue;
            }

            String prefix = team.getPrefix() == null ? "" : team.getPrefix().getString();
            String prefixLower = prefix.toLowerCase(Locale.ROOT);
            boolean hasStaffPrefix = prefixMatches.matcher(prefixLower).matches();

            for (String rawName : members) {
                if (rawName == null) {
                    continue;
                }

                String name = rawName.trim();
                if (!namePattern.matcher(name).matches()) {
                    continue;
                }
                if (mc.player.getName().getString().equalsIgnoreCase(name)) {
                    continue;
                }

                String key = name.toLowerCase(Locale.ROOT);
                if (added.contains(key)) {
                    continue;
                }

                boolean vanish = !tabPlayers.contains(key);
                if (!hasStaffPrefix) {
                    continue;
                }

                String cleanPrefix = prefix.trim();
                String text = cleanPrefix.isEmpty() ? name : cleanPrefix + " " + name;
                if (vanish) {
                    text += " [V]";
                }

                result.add(new StaffLine(text, vanish));
                added.add(key);
            }
        }

        return result;
    }

    private void setPosition() {
        float x = dragging.getX();
        float y = dragging.getY();
        if (!positionInitialized) {
            renderX = x;
            renderY = y;
            positionInitialized = true;
            return;
        }
        renderX = MathUtil.lerp(renderX, x, 10);
        renderY = MathUtil.lerp(renderY, y, 10);
    }

    private void drawPill(float x, float y, float width, int color, String text, int textColor) {
        RenderUtil2D.drawRoundRect(x, y, width, PILL_HEIGHT, PILL_RADIUS, color);
        Fonts.sfpro.get(TEXT_SIZE).drawCenterXY(text, x + width / 2f, y + PILL_HEIGHT / 2f, textColor);
    }

    private static final class StaffLine {
        private final String text;
        private final boolean vanished;

        private StaffLine(String text, boolean vanished) {
            this.text = text;
            this.vanished = vanished;
        }
    }
}
