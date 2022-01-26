package ch.bubendorf.spam;

import ch.bubendorf.spam.cmd.LearnHamCmd;
import ch.bubendorf.spam.cmd.LearnSpamCmd;
import ch.bubendorf.spam.cmd.CheckInboxCmd;
import ch.bubendorf.spam.cmd.StatCmd;
import com.beust.jcommander.JCommander;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IdleManager;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private final CommandLineArguments cmdArgs = new CommandLineArguments();
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private boolean stayInMainLoop = false;
    private IdleManager idleManager;
    private ExecutorService execService;
    private final Object idleLock = new Object();
    private boolean keepOnIdeling;
    private IMAPFolder idleFolder;

    public static void main(final String[] args) throws MessagingException, IOException, InterruptedException {
        logger.info("ImapRSpamd Version " + BuildVersion.getBuildVersion());
        new Main().imapRSpamd(args);
        logger.info("Done");
    }

    public void imapRSpamd(final String[] args) throws MessagingException, IOException, InterruptedException {
        final JCommander jCommander = new JCommander(cmdArgs);
        jCommander.setAcceptUnknownOptions(true);
        jCommander.setAllowAbbreviatedOptions(true);
        jCommander.setExpandAtSign(true);
        jCommander.setAllowParameterOverwriting(true);

        final List<String> allArgs = new ArrayList<>();
        for (final String file : new String[]{"/etc/imaprspamd/default.conf",
                System.getProperty("user.home") + File.separator + "default.conf",
                "default.conf",
                "/etc/imaprspamd/local.conf",
                System.getProperty("user.home") + File.separator + "local.conf",
                "local.conf"}) {
            if (new File(file).exists()) {
                allArgs.add("@" + file);
            }
        }

        allArgs.addAll(Arrays.asList(args));
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        jCommander.parse(allArgs.toArray(new String[allArgs.size()]));

        if (jCommander.getUnknownOptions().size() > 0) {
            for (final String arg : jCommander.getUnknownOptions()) {
                logger.error("Unknown parameter: " + arg);
            }
            System.exit(2);
        }

        if (cmdArgs.isHelp()) {
            jCommander.usage();
            System.exit(1);
        }

        cmdArgs.setDefaults();
        if (!cmdArgs.isValid()) {
            System.exit(2);
        }

        final Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", cmdArgs.getProtcol());
        if (cmdArgs.isStarttls()) {
            props.setProperty("mail.imap.starttls.enable", "true");
        }
        if (StringUtils.isNotBlank(cmdArgs.getSsltrust())) {
            props.setProperty("mail.imaps.ssl.trust", cmdArgs.getSsltrust());
        }

        props.setProperty("mail.imap.usesocketchannels", "true");
        props.setProperty("mail.imaps.usesocketchannels", "true");

        installSignalHandlers();

        final Session session = Session.getDefaultInstance(props, null);
        session.setDebug(cmdArgs.isVerbose());

        final IMAPStore store = (IMAPStore) session.getStore(cmdArgs.getProtcol());
        store.connect(cmdArgs.getHost(), cmdArgs.getPort(), cmdArgs.getUser(), cmdArgs.getPassword());

        mainLoop(session, store);

        store.close();
    }

    private void installSignalHandlers() {
        Signal.handle(new Signal("TERM"), sig -> {
            logger.info("Received TERM signal!");
            stayInMainLoop = false;
            synchronized (idleLock) {
                idleLock.notifyAll();
            }
            shutdownIdleManager();
            shutdownExecService();
        });
    }

    private void shutdownIdleManager() {
        if (idleManager != null) {
            idleManager.stop();
            idleManager = null;
        }
    }

    private void shutdownExecService() {
        if (execService != null) {
            execService.shutdown();
            execService = null;
        }
    }

    private void mainLoop(final Session session, final IMAPStore store) throws MessagingException, IOException, InterruptedException {
        do {
            for (final String cmd : cmdArgs.getCmds()) {
                switch (cmd) {
                    case "listFolders" -> listFolders(store);
                    case "learnSpam" -> learnSpam(store);
                    case "learnHam" -> learnHam(store);
                    case "checkInbox" -> checkInbox(store);
                    case "stat" -> stat();
                    case "idle" -> idle(session, store);
                    default -> unknownCommand(cmd);
                }
            }
        } while (stayInMainLoop);
        shutdownIdleManager();
        shutdownExecService();
    }

    private void learnSpam(final IMAPStore store) throws MessagingException, IOException, InterruptedException {
      final LearnSpamCmd learnSpamCmd = new LearnSpamCmd(cmdArgs, store);
        learnSpamCmd.run();
    }

  private void checkInbox (final IMAPStore store) throws MessagingException, IOException, InterruptedException {
      final CheckInboxCmd checkInboxCmd = new CheckInboxCmd(cmdArgs, store);
        checkInboxCmd.run();
    }

    private void learnHam(final IMAPStore store) throws MessagingException, IOException, InterruptedException {
        final LearnHamCmd learnHamCmd = new LearnHamCmd(cmdArgs, store);
        learnHamCmd.run();
    }

    private void stat() throws IOException, InterruptedException {
        final StatCmd statCmd = new StatCmd(cmdArgs);
        statCmd.run();
    }

    private void idle(final Session session, final IMAPStore store) throws IOException, MessagingException {
        stayInMainLoop = true;
        keepOnIdeling = true;

        if (execService == null) {
            execService = Executors.newCachedThreadPool();
        }
        final IMAPFolder idleFolder = getIdleFolder(store);
        if (idleManager == null) {
            idleManager = new IdleManager(session, execService);
            final MessageCountAdapter messageCountAdapter = new MessageCountAdapter() {
                public void messagesAdded(final MessageCountEvent ev) {
                    logger.debug("Message Count Changed");
                    keepOnIdeling = false;
                    synchronized (idleLock) {
                        idleLock.notifyAll();
                    }
                }
            };
            idleFolder.addMessageCountListener(messageCountAdapter);
        }

        while (idleManager.isRunning() && keepOnIdeling) {
            logger.debug("Watch IDLE folder");
            idleManager.watch(idleFolder);
            try {
                synchronized (idleLock) {
                    logger.debug("Before lock()");
                    idleLock.wait(9 * 60 * 1000L); // Do something after 9 minutes
                    logger.debug("After lock()");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (idleManager.isRunning() && keepOnIdeling) {
                // Keep the connection alive by sending an IMAP noop
                final IMAPFolder.ProtocolCommand noop = protocol -> {
                    logger.debug("Sending NoOp");
                    protocol.simpleCommand("NOOP", null);
                    return null;
                };
                idleFolder.doCommand(noop);
            }
        }

        logger.debug("Finish ideling");

/*        idleFolder.removeMessageCountListener(messageCountAdapter);
        idleManager.stop();
        idleManager = null;*/
    }

    private IMAPFolder getIdleFolder(final IMAPStore store) throws MessagingException {
        // TODO: Reuse the INBOX folder if it is the same
        if (idleFolder == null) {
            idleFolder = (IMAPFolder)store.getFolder(cmdArgs.getIdleFolder());
            idleFolder.open(Folder.READ_WRITE);
        }
        return idleFolder;
    }
    private void unknownCommand(final String cmd) {
        logger.error("Unknown command " + cmd);
        System.exit(2);
    }

    private void listFolders(final IMAPStore store) throws MessagingException {
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder[] folders = defaultFolder.list("*");
        for (final Folder folder : folders) {
            logger.info(folder.getFullName());
        }
    }
}