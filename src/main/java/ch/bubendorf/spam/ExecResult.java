package ch.bubendorf.spam;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecResult {
    private final String input;
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public ExecResult(final String input, final int exitCode, final String stdout, final String stderr) {
        this.input = input;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public String getInput() {
        return input;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    private final Pattern ACTION_PATTERN = Pattern.compile("^Action: (\\p{Alnum}+)$", Pattern.MULTILINE);
    private final Pattern SPAM_PATTERN = Pattern.compile("^Spam: (\\p{Alnum}+)$", Pattern.MULTILINE);
    private final Pattern SUCCESS_PATTERN = Pattern.compile("^success = (\\p{Alnum}+);$", Pattern.MULTILINE);
    private final Pattern SCORE_PATTERN = Pattern.compile("^Score: ([0-9.-]+) / ([0-9.-]+)$", Pattern.MULTILINE);
    private final Pattern SCAN_TIME_PATTERN = Pattern.compile("^scan_time = ([0-9.]+);$", Pattern.MULTILINE);
    private final Pattern SYMBOL_PATTERN = Pattern.compile("^Symbol: (.+)$", Pattern.MULTILINE);
    private final Pattern ERROR_PATTERN = Pattern.compile("^error = \"(.+)\";$", Pattern.MULTILINE);

    public String getAction() {
        if (StringUtils.isBlank(stdout)) {
            return null;
        }
        final Matcher matcher = ACTION_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public boolean isSpam() {
        if (StringUtils.isBlank(stdout)) {
            return false;
        }
        final Matcher matcher = SPAM_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    public double getScore() {
        if (StringUtils.isBlank(stdout)) {
            return Double.NaN;
        }
        final Matcher matcher = SCORE_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return Double.NaN;
    }

    public double getScoreThreshold() {
        if (StringUtils.isBlank(stdout)) {
            return Double.NaN;
        }
        final Matcher matcher = SCORE_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(2));
        }
        return Double.NaN;
    }

    public boolean hasSymbols() {
        if (StringUtils.isBlank(stdout)) {
            return false;
        }
        final Matcher matcher = SYMBOL_PATTERN.matcher(stdout);
        return matcher.find();
    }

    public List<Symbol> getSymbols() {
        if (StringUtils.isBlank(stdout)) {
            return null;
        }
        final Matcher matcher = SYMBOL_PATTERN.matcher(stdout);
        if (matcher.find()) {
            final List<Symbol> symbols = new ArrayList<>();
            symbols.add(new Symbol(matcher.group(1)));
            while (matcher.find()){
                symbols.add(new Symbol(matcher.group(1)));
            }
            return symbols;
        }
        return null;
    }

    public boolean isSuccess() {
        if (StringUtils.isBlank(stdout)) {
            return false;
        }
        final Matcher matcher = SUCCESS_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    public boolean isIgnore() {
        final String errorText = getError();
        return StringUtils.isNoneBlank(errorText) &&
                (errorText.contains("denied learning") || errorText.contains("has been already learned"));
    }

    public double getScanTime() {
        if (StringUtils.isBlank(stdout)) {
            return Double.NaN;
        }
        final Matcher matcher = SCAN_TIME_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return Double.NaN;
    }

    public String getError() {
        if (StringUtils.isBlank(stdout)) {
            return null;
        }
        final Matcher matcher = ERROR_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String getSymbolText() {
        final StringBuilder sb = new StringBuilder(256);

        final StringBuilder symbolsSB = new StringBuilder(256);
        for (final Symbol symbol : getSymbols()) {
            symbolsSB.append(symbol.toString()).append(", ");
            if (symbolsSB.length() >= 100) {
                sb.append(symbolsSB).append("\r\n\t");
                symbolsSB.setLength(0);
            }
        }
        if (symbolsSB.length() > 0) {
            sb.append(symbolsSB);
        }

        String text = sb.toString().trim();
        if (text.endsWith(",")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
