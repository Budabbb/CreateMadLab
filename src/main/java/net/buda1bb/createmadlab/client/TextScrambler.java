package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.effect.FentanylEffectsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TextScrambler {
    private static final float MIN_VISIBLE_STRENGTH = 0.015F;
    private static final float MAX_TEXT_BLUR_STRENGTH = 0.60F;
    private static final int SCRAMBLE_CACHE_LIMIT = 4096;
    private static final Map<String, String> SCRAMBLED_WORD_CACHE = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return this.size() > SCRAMBLE_CACHE_LIMIT;
        }
    };

    private TextScrambler() {
    }

    public static String scrambleText(String text) {
        float strength = getScrambleStrength();
        if (strength <= MIN_VISIBLE_STRENGTH) {
            return text;
        }

        return scrambleText(text, strength);
    }

    public static String scrambleTypedText(String text) {
        return scrambleText(text);
    }

    public static FormattedCharSequence wrapSequence(FormattedCharSequence sequence) {
        float strength = getScrambleStrength();
        if (strength <= MIN_VISIBLE_STRENGTH) {
            return sequence;
        }

        return sink -> {
            List<Glyph> glyphs = new ArrayList<>();
            sequence.accept((position, style, codePoint) -> {
                glyphs.add(new Glyph(style, codePoint));
                return true;
            });
            return emitScrambled(glyphs, strength, sink);
        };
    }

    public static float getBlurStrength() {
        return Mth.clamp(getScrambleStrength() * MAX_TEXT_BLUR_STRENGTH, 0.0F, MAX_TEXT_BLUR_STRENGTH);
    }

    public static int withBlurAlpha(int color, float blurStrength) {
        int adjustedColor = (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
        int alpha = adjustedColor >>> 24;
        int blurredAlpha = Mth.clamp((int) (alpha * (0.16F + 0.16F * blurStrength)), 0, 255);
        return (adjustedColor & 0x00FFFFFF) | (blurredAlpha << 24);
    }

    public static float getScrambleStrength() {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = ClientRenderTime.partialTick(minecraft);
        float strength = 0.0F;

        if (HeroinTripState.isActive()) {
            strength = Math.max(strength, HeroinTripState.getSmoothedIntensity(partialTick));
        }

        if (FentanylTripState.isActive()) {
            float elapsedTicks = FentanylTripState.getElapsedTicks(partialTick);
            strength = Math.max(strength, FentanylEffectsManager.computeEffectFadeIn(elapsedTicks));
        }

        return Mth.clamp(strength, 0.0F, 1.0F);
    }

    private static String scrambleText(String text, float strength) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder scrambled = new StringBuilder(text.length());
        StringBuilder word = new StringBuilder();

        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\u00A7' && offset + charCount < text.length()) {
                appendScrambledWord(scrambled, word, strength);
                scrambled.appendCodePoint(codePoint);
                int formattingCode = text.codePointAt(offset + charCount);
                scrambled.appendCodePoint(formattingCode);
                offset += charCount + Character.charCount(formattingCode);
                continue;
            }

            if (isWordCodePoint(codePoint)) {
                word.appendCodePoint(codePoint);
            } else {
                appendScrambledWord(scrambled, word, strength);
                scrambled.appendCodePoint(codePoint);
            }

            offset += charCount;
        }

        appendScrambledWord(scrambled, word, strength);
        return scrambled.toString();
    }

    private static void appendScrambledWord(StringBuilder target, StringBuilder word, float strength) {
        if (word.length() > 0) {
            target.append(scrambleWord(word.toString(), strength));
            word.setLength(0);
        }
    }

    private static boolean emitScrambled(List<Glyph> glyphs, float strength, FormattedCharSink sink) {
        List<Glyph> word = new ArrayList<>();
        int[] position = {0};

        for (Glyph glyph : glyphs) {
            if (isWordCodePoint(glyph.codePoint)) {
                word.add(glyph);
                continue;
            }

            if (!emitScrambledWord(word, strength, sink, position)) {
                return false;
            }
            if (!emitGlyph(glyph, sink, position)) {
                return false;
            }
        }

        return emitScrambledWord(word, strength, sink, position);
    }

    private static boolean emitScrambledWord(List<Glyph> word, float strength, FormattedCharSink sink, int[] position) {
        if (word.isEmpty()) {
            return true;
        }

        String wordText = glyphsToString(word);
        String scrambledText = scrambleWord(wordText, strength);
        if (scrambledText.equals(wordText)) {
            for (Glyph glyph : word) {
                if (!emitGlyph(glyph, sink, position)) {
                    word.clear();
                    return false;
                }
            }
            word.clear();
            return true;
        }

        boolean[] used = new boolean[word.size()];
        for (int offset = 0; offset < scrambledText.length(); ) {
            int codePoint = scrambledText.codePointAt(offset);
            int glyphIndex = findUnusedGlyph(word, used, codePoint);
            Glyph glyph = glyphIndex >= 0 ? word.get(glyphIndex) : new Glyph(word.get(0).style, codePoint);
            if (glyphIndex >= 0) {
                used[glyphIndex] = true;
            }
            if (!emitGlyph(glyph, sink, position)) {
                word.clear();
                return false;
            }
            offset += Character.charCount(codePoint);
        }

        word.clear();
        return true;
    }

    private static boolean emitGlyph(Glyph glyph, FormattedCharSink sink, int[] position) {
        return sink.accept(position[0]++, glyph.style, glyph.codePoint);
    }

    private static int findUnusedGlyph(List<Glyph> glyphs, boolean[] used, int codePoint) {
        for (int i = 0; i < glyphs.size(); i++) {
            if (!used[i] && glyphs.get(i).codePoint == codePoint) {
                return i;
            }
        }
        return -1;
    }

    private static String glyphsToString(List<Glyph> glyphs) {
        StringBuilder builder = new StringBuilder(glyphs.size());
        for (Glyph glyph : glyphs) {
            builder.appendCodePoint(glyph.codePoint);
        }
        return builder.toString();
    }

    private static String scrambleWord(String word, float strength) {
        int codePointCount = word.codePointCount(0, word.length());
        if (codePointCount <= 3) {
            return word;
        }

        String cached;
        synchronized (SCRAMBLED_WORD_CACHE) {
            cached = SCRAMBLED_WORD_CACHE.get(word);
        }
        if (word.equals(cached)) {
            return word;
        }
        if (!shouldScrambleWord(word, strength)) {
            return word;
        }
        if (cached != null) {
            return cached;
        }

        int[] codePoints = word.codePoints().toArray();
        int innerLength = codePoints.length - 2;
        int[] middle = new int[innerLength];
        System.arraycopy(codePoints, 1, middle, 0, innerLength);

        int seed = stableHash(word);
        int swapCount = Math.max(1, Math.round(innerLength * (0.22F + 0.28F * strength)));
        for (int i = 0; i < swapCount; i++) {
            seed = mix(seed + i * 0x9e3779b9);
            int first = Math.floorMod(seed, innerLength);
            seed = mix(seed ^ 0x85ebca6b);
            int second = Math.floorMod(seed, innerLength);
            if (first == second) {
                second = (second + 1) % innerLength;
            }

            int temp = middle[first];
            middle[first] = middle[second];
            middle[second] = temp;
        }

        ensureChanged(codePoints, middle);

        StringBuilder scrambled = new StringBuilder(word.length());
        scrambled.appendCodePoint(codePoints[0]);
        for (int codePoint : middle) {
            scrambled.appendCodePoint(codePoint);
        }
        scrambled.appendCodePoint(codePoints[codePoints.length - 1]);

        String scrambledWord = scrambled.toString();
        synchronized (SCRAMBLED_WORD_CACHE) {
            SCRAMBLED_WORD_CACHE.put(word, scrambledWord);
            SCRAMBLED_WORD_CACHE.put(scrambledWord, scrambledWord);
        }
        return scrambledWord;
    }

    private static void ensureChanged(int[] original, int[] middle) {
        boolean changed = false;
        for (int i = 0; i < middle.length; i++) {
            if (middle[i] != original[i + 1]) {
                changed = true;
                break;
            }
        }
        if (changed) {
            return;
        }

        for (int i = 0; i < middle.length - 1; i++) {
            if (middle[i] != middle[i + 1]) {
                int temp = middle[i];
                middle[i] = middle[i + 1];
                middle[i + 1] = temp;
                return;
            }
        }
    }

    private static boolean shouldScrambleWord(String word, float strength) {
        if (strength >= 0.98F) {
            return true;
        }

        float threshold = Mth.clamp(strength, 0.0F, 1.0F);
        int hash = stableHash(word);
        float roll = (mix(hash ^ 0x45d9f3b) & 0xFFFFFF) / (float) 0x1000000;
        return roll < threshold;
    }

    private static boolean isWordCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint);
    }

    private static int stableHash(String value) {
        int hash = 0x811c9dc5;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = Character.toLowerCase(value.codePointAt(offset));
            hash ^= codePoint;
            hash *= 0x01000193;
            offset += Character.charCount(codePoint);
        }
        return hash;
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return value;
    }

    private static final class Glyph {
        private final Style style;
        private final int codePoint;

        private Glyph(Style style, int codePoint) {
            this.style = style;
            this.codePoint = codePoint;
        }
    }
}
