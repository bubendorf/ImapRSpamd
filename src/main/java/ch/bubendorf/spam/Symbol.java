package ch.bubendorf.spam;


import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "MANY_INVISIBLE_PARTS(0.10)[2]" ==> MANY_INVISIBLE_PARTS, 0,.10, 2
 * "TO_DN_NONE(0.00)" ==> TO_DN_NONE, 0.00, null
 */
public class Symbol {

    private static final Pattern SYMBOL_SPLIT_PATTERN = Pattern.compile("([A-Za-z0-9_]+)\\s*([(]([0-9.-]+)[)])?(\\[(.+)])?");

    /*private static final Map<String, Function<String, String>> descFormatters= new HashMap<>();
    static {
        descFormatters.put("R_SPF_ALLOW", in -> in.replaceAll(":[a-z]$", ""));
        descFormatters.put("DCC_REJECT", in -> null);
    }*/

    private final String name;
    private final double score;
    private final String desc;

    public Symbol(final String symbol) {
        final Matcher matcher = SYMBOL_SPLIT_PATTERN.matcher(symbol);
        if (matcher.matches()) {
            name = matcher.group(1);
            score = matcher.groupCount() > 2 && matcher.group(3) != null ? Double.parseDouble(matcher.group(3)) : Double.NaN;
            desc = matcher.groupCount() > 4 && matcher.group(5) != null ? matcher.group(5).trim() : null;
        } else {
            name = symbol;
            score = Double.NaN;
            desc = null;
        }
    }

    @Override
    @NonNull
    public String toString() {
        return name + (Double.isNaN(score) ? "" : " (" + score + ")") + (desc == null ? "" : "[" + desc + "]");
    }

    @NonNull
    public String getShortForm() {
        return name + (Double.isNaN(score) ? "" : " (" + score + ")");
    }

    /*@NonNull
    public String getFormatted() {
        final String formattedDesc = desc == null ? null : descFormatters.getOrDefault(name, in->in).apply(desc);
        return name + (Double.isNaN(score) ? "" : " (" + score + ")") + (formattedDesc == null ? "" : "[" + formattedDesc + "]");
    }*/

    @NonNull
    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    @Nullable
    public String getDesc() {
        return desc;
    }
}
