package com.chzzk.rpg.grade;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.stats.StatType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BonusStatGenerator {
    private static final Random random = new Random();

    // Allowed stat types (HP is excluded)
    private static final StatType[] ALLOWED_STATS = {
        StatType.ATK, StatType.DEF, StatType.CRIT, StatType.CRIT_DMG, StatType.PEN
    };

    private BonusStatGenerator() {
    }

    public static BonusStat generate(WeaponGrade grade) {
        ChzzkRPG plugin = ChzzkRPG.getInstance();

        StatType type = selectRandomStatType(plugin);
        double[] range = getStatRange(plugin, type);
        double gradeMultiplier = getGradeMultiplier(plugin, grade);

        double min = range[0] * gradeMultiplier;
        double max = range[1] * gradeMultiplier;
        double value = min + (random.nextDouble() * (max - min));

        // Round to 2 decimal places
        value = Math.round(value * 100.0) / 100.0;

        return new BonusStat(type, value);
    }

    // Default values for testing (when plugin is not available)
    private static final double[][] DEFAULT_RANGES = {
        {1.0, 5.0},   // ATK
        {1.0, 5.0},   // DEF
        {0.5, 2.0},   // CRIT
        {0.02, 0.08}, // CRIT_DMG
        {0.5, 2.0}    // PEN
    };
    private static final int[] DEFAULT_WEIGHTS = {30, 30, 15, 12, 13};

    public static List<BonusStat> generateForGrade(WeaponGrade grade, List<BonusStat> existing) {
        List<BonusStat> result = new ArrayList<>(existing);
        int targetSlots = grade.getBonusStatSlots();
        int currentSlots = result.size();

        for (int i = currentSlots; i < targetSlots; i++) {
            result.add(generate(grade));
        }
        return result;
    }

    public static List<BonusStat> rerollAll(WeaponGrade grade, int count) {
        List<BonusStat> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(generate(grade));
        }
        return result;
    }

    private static StatType selectRandomStatType(ChzzkRPG plugin) {
        Map<StatType, Integer> weights = new EnumMap<>(StatType.class);

        int totalWeight = 0;
        for (int i = 0; i < ALLOWED_STATS.length; i++) {
            StatType stat = ALLOWED_STATS[i];
            int weight;
            if (plugin != null) {
                ConfigurationSection section = plugin.getConfig().getConfigurationSection("bonus-stats.weights");
                weight = section != null ? section.getInt(stat.name(), DEFAULT_WEIGHTS[i]) : DEFAULT_WEIGHTS[i];
            } else {
                weight = DEFAULT_WEIGHTS[i];
            }
            weights.put(stat, weight);
            totalWeight += weight;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (Map.Entry<StatType, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        return StatType.ATK;
    }

    private static double[] getStatRange(ChzzkRPG plugin, StatType type) {
        int index = java.util.Arrays.asList(ALLOWED_STATS).indexOf(type);
        if (index < 0) index = 0;

        if (plugin != null) {
            String path = "bonus-stats.base-ranges." + type.name();
            double min = plugin.getConfig().getDouble(path + ".min", DEFAULT_RANGES[index][0]);
            double max = plugin.getConfig().getDouble(path + ".max", DEFAULT_RANGES[index][1]);
            return new double[]{min, max};
        }
        return DEFAULT_RANGES[index];
    }

    private static double getGradeMultiplier(ChzzkRPG plugin, WeaponGrade grade) {
        if (plugin != null) {
            return plugin.getConfig().getDouble("bonus-stats.grade-multipliers." + grade.name(), grade.getStatMultiplier());
        }
        return grade.getStatMultiplier();
    }
}
