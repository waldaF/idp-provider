package io.idpprovider.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Pattern;

public class MaskingConverter extends MessageConverter {

    private static final List<PatternReplacement> RULES = List.of(
            new PatternReplacement(
                    Pattern.compile("(?i)(\"(?:password|secret|clientSecret)\"\\s*:\\s*\").*?(\")"),
                    "$1***$2"
            ),
            new PatternReplacement(
                    Pattern.compile("(?i)(Bearer\\s+)[\\w\\-.+/]+=*"),
                    "$1***"
            ),
            new PatternReplacement(
                    Pattern.compile("(?i)(private[-_]?key[^:]*:\\s*)\\S+"),
                    "$1***"
            )
    );

    @Override
    public String convert(final ILoggingEvent event) {
        var message = super.convert(event);
        for (PatternReplacement rule : RULES) {
            message = rule.pattern().matcher(message).replaceAll(rule.replacement());
        }
        return message;
    }

    private record PatternReplacement(Pattern pattern, String replacement) {}
}