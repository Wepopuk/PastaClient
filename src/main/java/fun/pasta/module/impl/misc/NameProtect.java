package fun.pasta.module.impl.misc;

import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameProtect extends Module {
    private static final String REPLACEMENT = "Pasta";
    private static final String BRAVO_SERVER_KEY = "bravohvh";
    private static final Pattern BRAVO_LABEL_PATTERN = Pattern.compile("(?iu)(?:\\u043d\\s*\\u0438\\s*\\u043a|n\\s*i\\s*c\\s*k)\\s*:");

    public NameProtect() {
        super("NameProtect", Category.MISC);
    }

    public String getReplacement() {
        return REPLACEMENT;
    }

    public List<String> getProtectedNames() {
        List<String> result = new ArrayList<>();
        if (!enable) {
            return result;
        }

        Set<String> unique = new LinkedHashSet<>();
        if (mc.session != null && mc.session.getUsername() != null && !mc.session.getUsername().isEmpty()) {
            unique.add(mc.session.getUsername());
        }

        if (mc.player != null && mc.player.getName() != null) {
            String playerName = mc.player.getName().getString();
            if (playerName != null && !playerName.isEmpty()) {
                unique.add(playerName);
            }
        }

        result.addAll(unique);
        return result;
    }

    public String protect(String text) {
        if (text == null || text.isEmpty() || !enable) {
            return text;
        }

        String result = text;
        if (isBravoServer()) {
            result = replaceBravoNick(result);
        }

        for (String name : getProtectedNames()) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            result = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(result)
                    .replaceAll(Matcher.quoteReplacement(REPLACEMENT));
        }
        return result;
    }

    private String replaceBravoNick(String text) {
        if (text.indexOf(':') < 0) {
            return text;
        }

        String[] lines = text.split("\\r?\\n", -1);
        boolean changed = false;

        for (int i = 0; i < lines.length; i++) {
            String replaced = replaceBravoNickInLine(lines[i]);
            if (!replaced.equals(lines[i])) {
                lines[i] = replaced;
                changed = true;
            }
        }

        if (!changed) {
            return text;
        }
        return String.join("\n", lines);
    }

    private String replaceBravoNickInLine(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        StringBuilder visible = new StringBuilder(line.length());
        List<Integer> map = new ArrayList<>(line.length());

        for (int i = 0; i < line.length(); ) {
            if (isColorCodeAt(line, i)) {
                i += 2;
                continue;
            }

            char ch = line.charAt(i);
            if (isInvisibleChar(ch)) {
                i++;
                continue;
            }

            visible.append(Character.toLowerCase(ch));
            map.add(i);
            i++;
        }

        Matcher labelMatcher = BRAVO_LABEL_PATTERN.matcher(visible);
        if (!labelMatcher.find()) {
            return line;
        }

        int colonVisibleIndex = labelMatcher.end() - 1;
        if (colonVisibleIndex < 0 || colonVisibleIndex >= map.size()) {
            return line;
        }

        int colonOriginalIndex = map.get(colonVisibleIndex);
        int tokenStart = colonOriginalIndex + 1;

        while (tokenStart < line.length() && Character.isWhitespace(line.charAt(tokenStart))) {
            tokenStart++;
        }
        while (tokenStart + 1 < line.length() && isColorCodeAt(line, tokenStart)) {
            tokenStart += 2;
        }
        while (tokenStart < line.length() && isInvisibleChar(line.charAt(tokenStart))) {
            tokenStart++;
        }

        if (tokenStart >= line.length()) {
            return line;
        }

        int tokenEnd = tokenStart;
        while (tokenEnd < line.length()) {
            if (isColorCodeAt(line, tokenEnd)) {
                tokenEnd += 2;
                continue;
            }

            char c = line.charAt(tokenEnd);
            if (Character.isWhitespace(c) || c == '|' || c == '\u00BB' || c == '\u00AB') {
                break;
            }
            tokenEnd++;
        }

        if (tokenEnd <= tokenStart) {
            return line;
        }

        return line.substring(0, tokenStart) + REPLACEMENT + line.substring(tokenEnd);
    }

    private boolean isColorCodeAt(String text, int index) {
        return index + 1 < text.length() && text.charAt(index) == '\u00A7';
    }

    private boolean isInvisibleChar(char c) {
        int type = Character.getType(c);
        return Character.isISOControl(c)
                || type == Character.FORMAT
                || c == '\u00A0'
                || c == '\u00AD'
                || (c >= '\u2000' && c <= '\u200F')
                || (c >= '\u2028' && c <= '\u202F')
                || c == '\u205F'
                || c == '\u2060'
                || c == '\uFEFF';
    }

    private boolean isBravoServer() {
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null || mc.getCurrentServerData().serverIP == null) {
            return false;
        }
        String ip = mc.getCurrentServerData().serverIP.toLowerCase(Locale.ROOT).trim();
        return ip.contains(BRAVO_SERVER_KEY);
    }
}
