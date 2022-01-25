package ch.bubendorf.spam;

import com.beust.jcommander.Parameter;
import com.github.sisyphsu.dateparser.DateParserUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    @Parameter(names = {"-cmd", "--command"})
    private List<String> cmds = new ArrayList<>();

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean isHelp = false;

    @Parameter(names = { "-v", "--verbose" }, description = "Be verbose")
    private boolean verbose = false;

    @Parameter(names = { "-f", "--force" }, description = "")
    private boolean force = false;

    @Parameter(names = {"-H", "--host"}, description = "Host name", arity = 1)
    private String host= null;

    @Parameter(names = {"-p", "--port"}, description = "Port number", arity = 1)
    private int port = 993;

    @Parameter(names = {"--protocol"}, description = "Protocol: imap or imaps", arity = 1)
    private String protcol = "imaps";

    @Parameter(names = {"--starttls"}, description = "Use STARTTLS", arity = 1)
    private boolean starttls = false;

    @Parameter(names = {"--ssltrust"}, description = "Trust TSL certificate errors", arity = 1)
    private String ssltrust = null;

    @Parameter(names = {"-u", "--user"}, description = "User name", arity = 1)
    private String user = null;

    @Parameter(names = {"-pw", "--password"}, description = "Password", arity = 1)
    private String password = null;

    @Parameter(names = {"-i", "--inbox", "--inboxFolder"}, description = "Name of the INBOX folder", arity = 1)
    private String inboxFolder = "INBOX";

    @Parameter(names = {"--idleFolder"}, description = "Name of the IDLE folder", arity = 1)
    private String idleFolder = null;

    @Parameter(names = {"--tomatoFolder"}, description = "Name of the TOMATO folder", arity = 1)
    private String tomatoFolder = "Junk";

    @Parameter(names = {"-s", "--spamFolder"}, description = "Name of the SPAM folder", arity = 1)
    private String spamFolder = "Junk";

    @Parameter(names = "--hamFolder", description = "Name of the HAM folder")
    private String hamFolder = "ham";

    @Parameter(names = "--trashFolder", description = "Name of the TRASH folder")
    private String trashFolder = "Trash";

    @Parameter(names = "--rspamc", description = "Commandline for rspamc")
    private String rspamc = "rspamc";
    // Von Windows aus: ssh mbu@n020 rspamc

    @Parameter(names = "--maxMessages", description = "Process at most this many message")
    private int maxMessages = Integer.MAX_VALUE;

    @Parameter(names = "--skipMessages", description = "Skip that many message")
    private int skipMessages = 0;

    @Parameter(names = "--receivedDateAfter", description = "Skip messages received before that date")
    private String receivedDateAfter = null;
    private Date receivedDateAfterDate = null;

    @Parameter(names = {"--hamAction"}, description = "addHeader, rewriteSubject, update, move, copy, delete, trash")
    private List<String> hamActions = new ArrayList<>();

    @Parameter(names = {"--tomatoAction"}, description = "addHeader, rewriteSubject, update, move, copy, delete, trash")
    private List<String> tomatoActions = new ArrayList<>();

    @Parameter(names = {"--spamAction"}, description = "addHeader, rewriteSubject, update, move, copy, delete, trash")
    private List<String> spamActions = new ArrayList<>();

    @Parameter(names = "--tomatoScore", description = "Tomato score")
    private double tomatoScore = 6.0;

    @Parameter(names = "--spamScore", description = "Spam score")
    private double spamScore = 6.0;

    @Parameter(names = "--newSubject", description = "Rewritten subject. %s=original Subject, %c=Score")
    private String newSubject = "[SPAM %c] %s";

    public void setDefaults() {
        if (idleFolder == null) {
            idleFolder = inboxFolder;
        }

        if (tomatoActions.isEmpty()) {
            tomatoActions.add("addHeader");
            tomatoActions.add("move");
        }
        if (!tomatoActions.contains("update") && !tomatoActions.contains("move") &&
                !tomatoActions.contains("copy") && !tomatoActions.contains("delete")) {
            tomatoActions.add("update");
        }

        if (spamActions.isEmpty()) {
            spamActions.add("addHeader");
            spamActions.add("move");
        }
        if (!spamActions.contains("update") && !spamActions.contains("move") &&
                !spamActions.contains("copy") && !spamActions.contains("delete")) {
            spamActions.add("update");
        }

        if (!hamActions.contains("update") && !hamActions.contains("move") &&
                !hamActions.contains("copy") && !hamActions.contains("delete")) {
            hamActions.add("update");
        }
    }

    public boolean isValid() {

        if (CollectionUtils.isEmpty(cmds)) {
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

    public List<String> getCmds() {
        return cmds;
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

    public Date getReceivedDateAfter() {
        if (receivedDateAfter != null && receivedDateAfterDate == null) {
            receivedDateAfterDate = DateParserUtils.parseDate(receivedDateAfter);
        }
        return receivedDateAfterDate;
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

    public Date getReceivedDateAfterDate() {
        return receivedDateAfterDate;
    }

    public List<String> getHamActions() {
        return hamActions;
    }

    public List<String> getTomatoActions() {
        return tomatoActions;
    }

    public List<String> getSpamActions() {
        return spamActions;
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

    public String getTrashFolder() {
        return trashFolder;
    }
}