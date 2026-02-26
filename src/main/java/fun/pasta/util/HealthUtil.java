package fun.pasta.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import java.util.OptionalInt;

public final class HealthUtil {

    private static final boolean SCOREBOARD_METHODS_AVAILABLE = hasScoreboardMethods();

    private HealthUtil() {
    }

    public static float getHealthForTargetHud(LivingEntity entity) {
        OptionalInt scoreboardHealth = getScoreboardHealth(entity);
        if (scoreboardHealth.isPresent()) {
            return scoreboardHealth.getAsInt();
        }
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static float getHealthForEsp(LivingEntity entity) {
        OptionalInt scoreboardHealth = getScoreboardHealth(entity);
        if (scoreboardHealth.isPresent()) {
            return scoreboardHealth.getAsInt();
        }
        return entity.getHealth();
    }

    public static OptionalInt getScoreboardHealth(LivingEntity entity) {
        if (!SCOREBOARD_METHODS_AVAILABLE || entity == null) {
            return OptionalInt.empty();
        }

        Scoreboard scoreboard = entity.getEntityWorld() != null ? entity.getEntityWorld().getScoreboard() : null;
        if (scoreboard == null) {
            return OptionalInt.empty();
        }

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
        if (objective == null) {
            return OptionalInt.empty();
        }

        String scoreboardName = entity.getScoreboardName();
        if (scoreboardName == null || scoreboardName.isEmpty()) {
            return OptionalInt.empty();
        }

        if (!scoreboard.entityHasObjective(scoreboardName, objective)) {
            return OptionalInt.empty();
        }

        Score score = scoreboard.getOrCreateScore(scoreboardName, objective);
        return OptionalInt.of(Math.max(0, score.getScorePoints()));
    }

    private static boolean hasScoreboardMethods() {
        try {
            Scoreboard.class.getMethod("getObjectiveInDisplaySlot", int.class);
            Scoreboard.class.getMethod("getOrCreateScore", String.class, ScoreObjective.class);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
