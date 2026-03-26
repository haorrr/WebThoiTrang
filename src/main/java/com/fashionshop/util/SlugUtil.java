package com.fashionshop.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SlugUtil {

    private SlugUtil() {}

    public static String toSlug(String input) {
        if (input == null) return "";
        // Normalize unicode (accents → base chars)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Remove combining marks
        String noAccents = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
        return noAccents.toLowerCase()
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .trim();
    }

    public static String toUniqueSlug(String base, java.util.function.Predicate<String> exists) {
        String slug = toSlug(base);
        if (!exists.test(slug)) return slug;
        // Append timestamp suffix
        return slug + "-" + System.currentTimeMillis();
    }
}
