package ir.hanzodev1375.ghostide.postman.util;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * JSON pretty-printing plus a small hand-rolled syntax highlighter for
 * displaying response/request bodies. Deliberately avoids a regex-based
 * highlighter (too easy to get subtly wrong on edge cases) in favour of a
 * simple character-by-character scan.
 */
public class JsonUtils {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    public static boolean looksLikeJson(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return false;
        char first = trimmed.charAt(0);
        if (first != '{' && first != '[') return false;
        try {
            JsonParser.parseString(trimmed);
            return true;
        } catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }
    }

    /** Returns a nicely indented version of the JSON, or the original text unchanged if it isn't valid JSON. */
    public static String prettyPrint(String rawText) {
        if (rawText == null) return "";
        try {
            JsonElement element = JsonParser.parseString(rawText);
            return PRETTY_GSON.toJson(element);
        } catch (Exception e) {
            return rawText;
        }
    }

    /**
     * Colors JSON keys, string values and numbers in a pretty-printed JSON string.
     * Falls back gracefully (returns plain text) for anything that isn't
     * actually JSON-shaped.
     */
    public static CharSequence highlight(Context context, String prettyJson, int keyColor, int stringColor, int numberColor) {
        SpannableString spannable = new SpannableString(prettyJson);
        int length = prettyJson.length();
        int i = 0;
        while (i < length) {
            char c = prettyJson.charAt(i);

            if (c == '"') {
                int start = i;
                i++;
                while (i < length) {
                    char current = prettyJson.charAt(i);
                    if (current == '\\' && i + 1 < length) {
                        i += 2;
                        continue;
                    }
                    if (current == '"') {
                        i++;
                        break;
                    }
                    i++;
                }
                int end = i; // one past the closing quote
                int lookahead = end;
                while (lookahead < length && Character.isWhitespace(prettyJson.charAt(lookahead))) {
                    lookahead++;
                }
                boolean isKey = lookahead < length && prettyJson.charAt(lookahead) == ':';
                int color = isKey ? keyColor : stringColor;
                spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            boolean isNumberStart = Character.isDigit(c) || (c == '-' && i + 1 < length && Character.isDigit(prettyJson.charAt(i + 1)));
            if (isNumberStart) {
                int start = i;
                i++;
                while (i < length) {
                    char current = prettyJson.charAt(i);
                    boolean partOfNumber = Character.isDigit(current) || current == '.' || current == 'e' || current == 'E' || current == '+' || current == '-';
                    if (!partOfNumber) break;
                    i++;
                }
                spannable.setSpan(new ForegroundColorSpan(numberColor), start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            i++;
        }
        return spannable;
    }
}
