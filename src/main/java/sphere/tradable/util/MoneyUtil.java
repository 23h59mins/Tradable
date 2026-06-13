package sphere.tradable.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MoneyUtil {
    private static final DecimalFormat STANDARD = new DecimalFormat("0.00");
    private static final DecimalFormat COMPACT = new DecimalFormat("0.##");
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000L);
    private static final MathContext MULTIPLY_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    private static final String[] SUFFIXES = {
            "", "K", "M", "B", "T",
            "Qa", "Qi", "Sx", "Sp", "Oc", "No",
            "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod",
            "Vg", "Uvg", "Dvg", "Tvg", "Qavg", "Qivg", "Sxvg", "Spvg", "Ocvg", "Novg"
    };

    private static final Map<String, Integer> SUFFIX_POWERS = createSuffixPowerMap();

    static {
        STANDARD.setRoundingMode(RoundingMode.HALF_UP);
        COMPACT.setRoundingMode(RoundingMode.DOWN);
    }

    private MoneyUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static double sanitize(final double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return 0D;
        }

        return BigDecimal.valueOf(Math.max(0D, amount))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static BigDecimal sanitize(final BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return amount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public static double requirePositive(final double amount, final String field) {
        final double value = sanitize(amount);
        if (value <= 0D) {
            throw new IllegalArgumentException(field + " must be positive.");
        }

        return value;
    }

    public static BigDecimal requirePositive(final BigDecimal amount, final String field) {
        final BigDecimal value = sanitize(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be positive.");
        }

        return value;
    }

    public static String format(final double amount) {
        return STANDARD.format(sanitize(amount));
    }

    public static String format(final BigDecimal amount) {
        return STANDARD.format(sanitize(amount));
    }

    public static String formatCompact(final double amount) {
        return formatCompact(BigDecimal.valueOf(sanitize(amount)));
    }

    public static String formatCompact(final BigDecimal amount) {
        BigDecimal value = sanitize(amount);

        if (value.compareTo(THOUSAND) < 0) {
            return trimZeros(value);
        }

        int suffixIndex = 0;
        BigDecimal compact = value;

        while (compact.compareTo(THOUSAND) >= 0 && suffixIndex < SUFFIXES.length - 1) {
            compact = compact.divide(THOUSAND, 6, RoundingMode.DOWN);
            suffixIndex++;
        }

        return COMPACT.format(compact.doubleValue()) + SUFFIXES[suffixIndex];
    }

    public static double parse(final String input) {
        return sanitize(parseBigDecimal(input).doubleValue());
    }

    public static BigDecimal parseBigDecimal(final String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be blank.");
        }

        final String normalized = normalizeNumericInput(input);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be blank.");
        }

        final String lower = normalized.toLowerCase(Locale.ROOT);

        for (final Map.Entry<String, Integer> entry : SUFFIX_POWERS.entrySet()) {
            final String suffix = entry.getKey();

            if (!lower.endsWith(suffix)) {
                continue;
            }

            final String numberPart = lower.substring(0, lower.length() - suffix.length()).trim();
            if (numberPart.isEmpty()) {
                break;
            }

            try {
                final BigDecimal base = new BigDecimal(numberPart);
                final BigDecimal multiplier = THOUSAND.pow(entry.getValue(), MULTIPLY_CONTEXT);
                return sanitize(base.multiply(multiplier, MULTIPLY_CONTEXT));
            } catch (final NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid amount: " + input, exception);
            }
        }

        try {
            return sanitize(new BigDecimal(lower));
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid amount: " + input, exception);
        }
    }

    public static String toPlainString(final BigDecimal value) {
        if (value == null) {
            return "0";
        }

        final BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            return normalized.setScale(0, RoundingMode.DOWN).toPlainString();
        }

        return normalized.toPlainString();
    }

    private static String trimZeros(final BigDecimal value) {
        final BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            return normalized.setScale(0, RoundingMode.DOWN).toPlainString();
        }

        return normalized.toPlainString();
    }

    private static String normalizeNumericInput(final String input) {
        return input.trim()
                .replace(",", "")
                .replace("_", "")
                .replace("$", "");
    }

    private static Map<String, Integer> createSuffixPowerMap() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        register(map, "novg", 30);
        register(map, "ocvg", 29);
        register(map, "spvg", 28);
        register(map, "sxvg", 27);
        register(map, "qivg", 26);
        register(map, "qavg", 25);
        register(map, "tvg", 24);
        register(map, "dvg", 23);
        register(map, "uvg", 22);
        register(map, "vg", 21);
        register(map, "nod", 20);
        register(map, "ocd", 19);
        register(map, "spd", 18);
        register(map, "sxd", 17);
        register(map, "qid", 16);
        register(map, "qad", 15);
        register(map, "td", 14);
        register(map, "dd", 13);
        register(map, "ud", 12);
        register(map, "dc", 11);
        register(map, "no", 10);
        register(map, "oc", 9);
        register(map, "sp", 8);
        register(map, "sx", 7);
        register(map, "qi", 6);
        register(map, "qa", 5);
        register(map, "t", 4);
        register(map, "b", 3);
        register(map, "m", 2);
        register(map, "k", 1);
        return map;
    }

    private static void register(final Map<String, Integer> map, final String suffix, final int power) {
        map.put(suffix, power);
    }
}