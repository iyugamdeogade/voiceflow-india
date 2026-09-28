package com.voiceflow.service;

import com.voiceflow.dto.CleanupOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Simple, transparent, local text tidy-up. No AI and no network. It never translates
 * and never rewrites sentences; every rule below is a small regular expression.
 * The result lists exactly which rules changed the text.
 */
@Service
public class TextCleanupService {

    public record Result(String text, List<String> operations) {}

    // Conservative English-only fillers. Hindi/Marathi words are never removed.
    private static final Pattern FILLERS = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:u+m+|u+h+|e+r+m*|h+m+)(?![\\p{L}\\p{N}]),?[ \\t]*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SPACE_BEFORE_PUNCT = Pattern.compile("[ \\t]+([,.!?;\\u0964])");
    private static final Pattern REPEATED_COMMA = Pattern.compile(",{2,}");
    private static final Pattern MISSING_SPACE_AFTER = Pattern.compile("([,;!?\\u0964])(?=\\p{L})");
    private static final Pattern MISSING_SPACE_AFTER_DOT = Pattern.compile("(?<=\\p{Ll}{2})\\.(?=\\p{Lu})");
    private static final Pattern SENTENCE_START = Pattern.compile("(^|[.!?]\\s+|\\n)(\\p{Ll})");
    private static final Pattern PRONOUN_I = Pattern.compile(
            "(?<![\\p{L}\\p{N}'\\u2019])i(?=(?:['\\u2019](?:m|ve|ll|d))?(?![\\p{L}\\p{N}]))");

    public Result clean(String input, CleanupOptions options) {
        String text = input == null ? "" : input;
        List<String> ops = new ArrayList<>();

        if (options.removeFillers()) {
            text = step(text, FILLERS.matcher(text).replaceAll(""), "Removed filler words (um, uh, er, hmm)", ops);
        }
        if (options.fixWhitespace()) {
            text = step(text, normalizeWhitespace(text), "Tidied spaces and line breaks", ops);
        }
        if (options.fixPunctuation()) {
            text = step(text, fixPunctuation(text), "Fixed spacing around punctuation", ops);
            text = step(text, addClosingPunctuation(text), "Added closing punctuation", ops);
        }
        if (options.capitalize()) {
            text = step(text, capitalize(text), "Capitalized sentence starts and the word \"I\"", ops);
        }
        return new Result(text, List.copyOf(ops));
    }

    private static String step(String before, String after, String label, List<String> ops) {
        if (!before.equals(after)) ops.add(label);
        return after;
    }

    private static String normalizeWhitespace(String text) {
        String t = text.replace("\r\n", "\n").replace('\r', '\n');
        t = t.replaceAll("[ \\t\\u00A0]+", " ");
        t = t.replaceAll(" ?\\n ?", "\n");
        t = t.replaceAll("\\n{3,}", "\n\n");
        return t.strip();
    }

    private static String fixPunctuation(String text) {
        String t = SPACE_BEFORE_PUNCT.matcher(text).replaceAll("$1");
        t = REPEATED_COMMA.matcher(t).replaceAll(",");
        t = MISSING_SPACE_AFTER.matcher(t).replaceAll("$1 ");
        t = MISSING_SPACE_AFTER_DOT.matcher(t).replaceAll(". ");
        return t;
    }

    /** Adds "." (or the Devanagari danda "।") when the text ends in a letter or digit. */
    private static String addClosingPunctuation(String text) {
        String t = text.stripTrailing();
        if (t.isEmpty()) return text;
        int last = t.codePointBefore(t.length());
        int type = Character.getType(last);
        boolean wordLike = Character.isLetterOrDigit(last)
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK;
        if (!wordLike) return text;
        boolean devanagari = Character.UnicodeScript.of(last) == Character.UnicodeScript.DEVANAGARI;
        return t + (devanagari ? "\u0964" : ".");
    }

    private static String capitalize(String text) {
        Matcher m = SENTENCE_START.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String letter = m.group(2);
            boolean latin = Character.UnicodeScript.of(letter.codePointAt(0)) == Character.UnicodeScript.LATIN;
            String replacement = m.group(1) + (latin ? letter.toUpperCase() : letter);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return PRONOUN_I.matcher(sb.toString()).replaceAll("I");
    }
}
