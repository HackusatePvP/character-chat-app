package me.piitex.app.utils;

import com.drew.lang.annotations.Nullable;
import javafx.scene.paint.Color;
import me.piitex.app.App;
import me.piitex.app.backend.Character;
import me.piitex.app.backend.User;
import me.piitex.app.configuration.AppSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholder {

    public static String retrieveOriginalText(String bbCodeText) {
        if (bbCodeText == null || bbCodeText.isEmpty()) {
            return bbCodeText;
        }

        Pattern bbCodePattern = Pattern.compile("\\[color=[a-zA-Z]+\\]|\\[/color\\]");

        Matcher matcher = bbCodePattern.matcher(bbCodeText);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Adds coloring around text. Quotes are wrapped in yellow. Astrix are wrapped in blue.
     *
     * <pre>
     *     plainText = {character} stares at {user} "What do you think you're looking at?" *glares*
     *
     *     return = {character} stares at {user} [color=lightyellow]"What do you think you're looking at?"[/color] [color=lightblue]*glares*[/color]
     * </pre>
     * @param plainText Base text to apply coloring to.
     * @return Text with BB-Code tags.
     */
    public static String applyDynamicBBCode(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        AppSettings appSettings = App.getInstance().getAppSettings();
        Color astrixColor = Color.web(appSettings.getAstrixColor());
        Color quoteColor = Color.web(appSettings.getQuoteColor());

        String result = plainText;

        Pattern quotePattern = Pattern.compile("(\"[^\"]*\")");
        Pattern asteriskPattern = Pattern.compile("(\\*[^\\*]*\\*)");

        Matcher quoteMatcher = quotePattern.matcher(result);
        StringBuilder sbQuote = new StringBuilder();
        while (quoteMatcher.find()) {
            String matchedQuote = quoteMatcher.group(1).trim(); // e.g., "Hello, player."
            String replacement = "[color=rgb(" + quoteColor.getRed()  * 255 + "," + quoteColor.getGreen() * 255 + ","  + quoteColor.getBlue() * 255 + ")]" + matchedQuote.trim() + "[/color]";
            quoteMatcher.appendReplacement(sbQuote, Matcher.quoteReplacement(replacement));
        }
        quoteMatcher.appendTail(sbQuote);
        result = sbQuote.toString();

        Matcher asteriskMatcher = asteriskPattern.matcher(result);
        StringBuilder sbAsterisk = new StringBuilder();
        while (asteriskMatcher.find()) {
            String matchedAsterisk = asteriskMatcher.group(1).trim(); // e.g., *Waves*
            String replacement = "[color=rgb(" + astrixColor.getRed()  * 255 + "," + astrixColor.getGreen() * 255 + ","  + astrixColor.getBlue() * 255 + ")]" + matchedAsterisk.trim() + "[/color]";
            asteriskMatcher.appendReplacement(sbAsterisk, Matcher.quoteReplacement(replacement));
        }
        asteriskMatcher.appendTail(sbAsterisk);
        result = sbAsterisk.toString().trim();

        return result;
    }

    public static String formatSymbols(String content) {
        // Replaces weird ai symbols that I find
        content = content.replace("“", "\"");
        content = content.replace("”", "\"");

        return content;
    }


    public static String formatPlaceholders(String content, @Nullable Character character, @Nullable User user) {
        content = formatImproperTags(content);
        if (character != null) {
            content = content.replace("{{char}}", character.getDisplayName()).replace("{char}", character.getDisplayName()).replace("{{chara}}", character.getDisplayName()).replace("{chara}", character.getDisplayName()).replace("{{character}}", character.getDisplayName()).replace("{character}", character.getDisplayName());
        }

        if (user != null) {
            content = content.replace("{user}", user.getDisplayName()).replace("{{user}}", user.getDisplayName()).replace("{usr}", user.getDisplayName()).replace("{{usr}}", user.getDisplayName());
        }

        return content;
    }

    public static String formatImproperTags(String content) {
        // Removes improper tags that are caused by misconfigured models/chat templates
        content = content.replace("<im_end>", "");
        content = content.replace("<|im_end|>", "");
        content = content.replace("<|im_start|>user", "");
        content = content.replace("<|im_start|>assistant", "");

        return content;
    }
}
