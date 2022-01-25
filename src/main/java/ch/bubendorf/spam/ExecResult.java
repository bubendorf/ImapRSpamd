package ch.bubendorf.spam;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public ExecResult(final int exitCode, final String stdout, final String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
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

    private final Pattern ACTION_PATTERN = Pattern.compile("^Action: ([\\p{Alnum}]+)$", Pattern.MULTILINE);
    private final Pattern SPAM_PATTERN = Pattern.compile("^Spam: ([\\p{Alnum}]+)$", Pattern.MULTILINE);
    private final Pattern SUCCESS_PATTERN = Pattern.compile("^success = ([\\p{Alnum}]+);$", Pattern.MULTILINE);
    private final Pattern SCORE_PATTERN = Pattern.compile("^Score: ([0-9.]+) / ([0-9.]+)$", Pattern.MULTILINE);
    private final Pattern SCAN_TIME_PATTERN = Pattern.compile("^scan_time = ([0-9.]+);$", Pattern.MULTILINE);
    private final Pattern SYMBOL_PATTERN = Pattern.compile("^Symbol: (.+)$", Pattern.MULTILINE);
    private final Pattern ERROR_PATTERN = Pattern.compile("^error = \"(.+)\";$", Pattern.MULTILINE);

    public String getAction() {
        final Matcher matcher = ACTION_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public boolean isSpam() {
        final Matcher matcher = SPAM_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    public double getScore() {
        final Matcher matcher = SCORE_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    public double getScoreThreshold() {
        final Matcher matcher = SCORE_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(2));
        }
        return 0.0;
    }

    public List<String> getSymbols() {
        final Matcher matcher = SYMBOL_PATTERN.matcher(stdout);
        if (matcher.find()) {
            final List<String> symbols = new ArrayList<>();
            symbols.add(matcher.group(1));
            while (matcher.find()){
                symbols.add(matcher.group(1));
            }
            return symbols;
        }
        return null;
    }

    public boolean isSuccess() {
        final Matcher matcher = SUCCESS_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    public double getScanTime() {
        final Matcher matcher = SCAN_TIME_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    public String getError() {
        final Matcher matcher = ERROR_PATTERN.matcher(stdout);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String getHeaderText() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Spam: ").append(isSpam()).append(", ");
        sb.append("Score: ").append(getScore()).append("\r\n\t ");

        final StringBuilder symbolsSB = new StringBuilder(256);
        for (final String symbol : getSymbols()) {
            symbolsSB.append(symbol).append(", ");
            if (symbolsSB.length() >= 100) {
                sb.setLength(sb.length() - 1);
                sb.append(symbolsSB).append("\r\n\t ");
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
