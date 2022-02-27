package ch.bubendorf.spam;

import com.beust.jcommander.Parameter;
import com.github.sisyphsu.dateparser.DateParserUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ALL")
public class CommandLineArguments {

    private final Logger logger = LoggerFactory.getLogger(CommandLineArguments.class);

    /*
     * Commands:
     * listFolders: Lists all folders
     * stat:
     * learnHam: Learn the mails from the HAM folder as ham
     * learnSpam: Learn the mails from the SPAM folder as spam
     * checkInbox: Run rspamc on the mails in the INBOX folder
     */
    @Parameter(names = {"-cmd", "--command"}, description = "listFolders, learnSpam, learnHam, checkInbox, stat, idle", arity = 1)
    private String cmds = null;

    @Parameter(names = {"-h", "--help"}, help = true, description = "Show this help")
    private boolean isHelp = false;

    @Parameter(names = {"-v", "--verbose"}, description = "Be verbose")
    private boolean verbose = false;

    @Parameter(names = {"-f", "--force"}, description = "Process even already processed mails")
    private boolean force = false;

    @Parameter(names = {"-H", "--host"}, description = "Host name", arity = 1)
    private String host = null;

    @Parameter(names = {"-p", "--port"}, description = "Port number", arity = 1)
    private int port = 993;

    @Parameter(names = {"--protocol"}, description = "Protocol: imap or imaps", arity = 1)
    private String protcol = "imaps";

    @Parameter(names = {"--starttls"}, description = "Use STARTTLS")
    private boolean starttls = false;

    @Parameter(names = {"--ssltrust"}, description = "Trust all SSL/TSL certificates", arity = 1)
    private String ssltrust = null;

    @Parameter(names = {"-u", "--user"}, description = "User name to login to the server", arity = 1)
    private String user = null;

    @Parameter(names = {"-pw", "--password"}, description = "Password to login to the server", arity = 1)
    private String password = null;

    @Parameter(names = {"-i", "--inboxFolder"}, description = "Name of the INBOX folder", arity = 1)
    private String inboxFolder = "INBOX";

    @Parameter(names = {"--idleFolder"}, description = "Name of the IDLE folder. Default: The same as --inboxFolder", arity = 1)
    private String idleFolder = null;

    @Parameter(names = {"--idleTimeout"}, description = "IDLE timeout in seconds. Default: 1790", arity = 1)
    private int idleTimeout = 1790;

    @Parameter(names = {"--tomatoFolder"}, description = "Name of the TOMATO folder", arity = 1)
    private String tomatoFolder = "Junk";

    @Parameter(names = {"-s", "--spamFolder"}, description = "Name of the SPAM folder", arity = 1)
    private String spamFolder = "Junk";

    @Parameter(names = "--hamFolder", description = "Name of the HAM folder", arity = 1)
    private String hamFolder = "ham";

    @Parameter(names = "--trashFolder", description = "Name of the TRASH folder", arity = 1)
    private String trashFolder = "Trash";

    @Parameter(names = "--rspamc", description = "Commandline for rspamc", arity = 1)
    private String rspamc = "rspamc";
    // Von Windows aus: ssh mbu@n020 rspamc

    @Parameter(names = "--maxMessages", description = "Process at most this many messages", arity = 1)
    private int maxMessages = Integer.MAX_VALUE;

    @Parameter(names = "--skipMessages", description = "Skip that many messages", arity = 1)
    private int skipMessages = 0;

    @Parameter(names = {"--messageId"}, description = "Only process mails with given id", arity = 1)
    private String messageIds = null;

    @Parameter(names = "--maxSize", description = "Maximum message size. Default: 1'048'576 bytes.", arity = 1)
    private int maxSize = 1048576; // 1 MB

    @Parameter(names = "--receivedDateAfter", description = "Skip messages received before the date/time", arity = 1)
    private String receivedDateAfter = null;
    private Date receivedDateAfterDate = null;

    @Parameter(names = "--receivedDateBefore", description = "Skip messages received after the date/time", arity = 1)
    private String receivedDateBefore = null;
    private Date receivedDateBeforeDate = null;

    @Parameter(names = {"--hamAction"}, description = "addHeader, rewriteSubject, update, move, copy, delete, trash, noop\n" +
            "\taddHeader: Add the X-ImapRSpamd-XXX headers to the message\n" +
            "\trewriteSubject: Replace the subject of the message\n" +
            "\tupdate: Update the message (if changed)\n" +
            "\tmove: Move the message to the ham/tomato/spam folder\n" +
            "\tcopy: Copy the message to the ham/tomate/spam folder\n" +
            "\tdelete: Delete the message\n" +
            "\ttrash: Moce the message to the trash folder\n" +
            "\tnoop: Do nothing\n", arity = 1)
    private String hamActions = null;

    @Parameter(names = {"--tomatoAction"}, description = "addHeader, rewriteSubject, update, move, copy, delete, trash, noop", arity = 1)
    private String tomatoActions = "addHeader,move";

    @Parameter(names = {"--spamAction"}, description = "addHeader, rewriteSubject, update, move, copy, delete, trash, noop", arity = 1)
    private String spamActions = "addHeader,move";

    @Parameter(names = "--tomatoScore", description = "Tomato score", arity = 1)
    private double tomatoScore = 8.0;

    @Parameter(names = "--spamScore", description = "Spam score", arity = 1)
    private double spamScore = 18.0;

    @Parameter(names = "--newSubject", description = "Rewritten subject. %s=original subject, %c=Score", arity = 1)
    private String newSubject = "[SPAM %c] %s";

    @Parameter(names = {"--systemd"}, description = "Run as a systemd service. Send watchdog messages.")
    private boolean systemd = false;

    public void setDefaults() {
        if (idleFolder == null) {
            idleFolder = inboxFolder;
        }

        if (StringUtils.isBlank(tomatoActions)) {
            tomatoActions = "addHeader,move";
        }
        if (!tomatoActions.contains("update") && !tomatoActions.contains("move") &&
                !tomatoActions.contains("copy") && !tomatoActions.contains("delete")) {
            tomatoActions += ",update";
        }

        if (StringUtils.isBlank(spamActions)) {
            spamActions = "addHeader,move";
        }
        if (!spamActions.contains("update") && !spamActions.contains("move") &&
                !spamActions.contains("copy") && !spamActions.contains("delete")) {
            spamActions += ",update";
        }

        if (StringUtils.isNotBlank(hamActions) &&
                !hamActions.contains("update") && !hamActions.contains("move") &&
                !hamActions.contains("copy") && !hamActions.contains("delete")) {
            hamActions += ",update";
        }
    }

    public boolean isValid() {

        if (StringUtils.isBlank(cmds)) {
            logger.error("Missing commands");
            return false;
        }
        if (StringUtils.isBlank(host)) {
            logger.error("Missing host");
            return false;
        }
        if (StringUtils.isBlank(user)) {
            logger.error("Missing user name");
            return false;
        }
        if (StringUtils.isBlank(password)) {
            logger.error("Missing password");
            return false;
        }
        return true;
    }

    private static List<String> toList(final String csv) {
        // Empty CSV ==> Empty list
        if (StringUtils.isBlank(csv)) {
            return Collections.emptyList();
        }
        return Stream.of(csv.split(","))
                .map(String::trim)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> getCmds() {
        return toList(cmds);
    }

    public boolean isHelp() {
        return isHelp;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isForce() {
        return force;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtcol() {
        return protcol;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getInboxFolder() {
        return inboxFolder;
    }

    public String getSpamFolder() {
        return spamFolder;
    }

    public String getHamFolder() {
        return hamFolder;
    }

    public String getRspamc() {
        return rspamc;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public int getSkipMessages() {
        return skipMessages;
    }

    public List<String> getMessageIds() {
        return toList(messageIds);
    }

    public Date getReceivedDateAfter() {
        if (receivedDateAfter != null && receivedDateAfterDate == null) {
            receivedDateAfterDate = DateParserUtils.parseDate(receivedDateAfter);
        }
        return receivedDateAfterDate;
    }

    public Date getReceivedDateBefore() {
        if (receivedDateBefore != null && receivedDateBeforeDate == null) {
            receivedDateBeforeDate = DateParserUtils.parseDate(receivedDateBefore);
        }
        return receivedDateBeforeDate;
    }

    public boolean isStarttls() {
        return starttls;
    }

    public String getSsltrust() {
        return ssltrust;
    }

    public String getTomatoFolder() {
        return tomatoFolder;
    }

    public List<String> getHamActions() {
        return toList(hamActions);
    }

    public List<String> getTomatoActions() {
        return toList(tomatoActions);
    }

    public List<String> getSpamActions() {
        return toList(spamActions);
    }

    public double getTomatoScore() {
        return tomatoScore;
    }

    public double getSpamScore() {
        return spamScore;
    }

    public String getNewSubject() {
        return newSubject;
    }

    public String getIdleFolder() {
        return idleFolder;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public String getTrashFolder() {
        return trashFolder;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public boolean isSystemd() {
        return systemd;
    }
}