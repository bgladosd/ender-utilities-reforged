package dev.celli.enderutilities.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class EUStringUtils {
    private static final String[] SI_PREFIXES = {"", "k", "M", "G", "T", "P", "E", "Z", "Y"};

    private EUStringUtils() {
    }

    /**
     * Orig {@code EUStringUtils.getStackSizeString}: full digits through 9999, then SI.
     */
    public static String getStackSizeString(net.minecraft.world.item.ItemStack stack, int maxChars) {
        return formatNumber(stack.getCount(), 9999, maxChars);
    }

    public static String formatNumber(long value, long maxUnformatted, int maxChars) {
        if (value <= maxUnformatted) {
            return String.valueOf(value);
        }
        double scaled = value;
        int prefixIndex = 0;
        while (scaled >= 1000D && prefixIndex < SI_PREFIXES.length - 1) {
            scaled /= 1000D;
            prefixIndex++;
        }
        int digits = 1;
        double div = 10D;
        while (scaled >= div && digits < 64) {
            div *= 10;
            digits++;
        }
        int maxDecimals = maxChars - digits - 1 - SI_PREFIXES[prefixIndex].length();
        String valueStr = String.valueOf(scaled);
        int endIndex = maxDecimals > 0 ? Math.min(digits + 1 + maxDecimals, valueStr.length()) : digits;
        return valueStr.substring(0, Math.min(endIndex, valueStr.length())) + SI_PREFIXES[prefixIndex];
    }

    public static String formatNumberWithKSeparators(long value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    public static String getInitialsWithDots(String str) {
        StringBuilder sb = new StringBuilder(64);
        for (String word : str.split(" ")) {
            for (int i = 0; i < word.length(); i++) {
                if (Character.isAlphabetic(word.charAt(i))) {
                    sb.append(word.charAt(i)).append('.');
                    break;
                }
            }
        }
        return sb.toString();
    }
}
