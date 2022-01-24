package ch.bubendorf.spam;

import ch.bubendorf.spam.cmd.LearnHamCmd;
import ch.bubendorf.spam.cmd.LearnSpamCmd;
import ch.bubendorf.spam.cmd.CheckInboxCmd;
import ch.bubendorf.spam.cmd.StatCmd;
import com.beust.jcommander.JCommander;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Main {

    private final CommandLineArguments cmdArgs = new CommandLineArguments();
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

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
//        props.setProperty("mail.imaps.ssl.checkserveridentity", "false");
        }

        final Session session = Session.getDefaultInstance(props, null);
        session.setDebug(cmdArgs.isVerbose());

        final IMAPStore store = (IMAPStore) session.getStore(cmdArgs.getProtcol());
        store.connect(cmdArgs.getHost(), cmdArgs.getPort(), cmdArgs.getUser(), cmdArgs.getPassword());

        for (final String cmd : cmdArgs.getCmds()) {
            switch (cmd) {
                case "listFolders" -> listFolders(store);
                case "learnSpam" -> learnSpam(store);
                case "learnHam" -> learnHam(store);
                case "checkInbox" -> checkInbox(store);
                case "stat" -> stat();
                default -> unknownCommand(cmd);
            }
        }
        store.close();
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