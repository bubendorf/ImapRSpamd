package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class CheckInboxCmd extends BaseFolderCommand {

    public static final String X_IMAP_RSPAMD_SPAM = "X-ImapRSpamd-Spam";
    public static final String X_IMAP_RSPAMD_SCORE = "X-ImapRSpamd-Score";
    public static final String X_IMAP_RSPAMD_SYMBOLS = "X-ImapRSpamd-Symbols";
    public static final String X_IMAP_RSPAMD_SUBJECT = "X-ImapRSpamd-Subject";

    private IMAPFolder hamFolder;
    private IMAPFolder tomatoFolder;
    private IMAPFolder spamFolder;
    private IMAPFolder trashFolder;

    public CheckInboxCmd(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs, store);
    }

    @Override
    protected String getFolderName() {
        return cmdArgs.getInboxFolder();
    }

    @Override
    protected boolean isEligible(final IMAPMessage msg) throws MessagingException {
        final Flags flags = msg.getFlags();
        return super.isEligible(msg) && !flags.contains(CheckInboxFlag);
    }

    @Override
    protected ExecResult apply(final IMAPMessage msg, final String messageText) throws IOException, InterruptedException, MessagingException {
        return apply(msg, messageText, 1);
    }

    protected ExecResult apply(final IMAPMessage msg, final String messageText, final int numberOfRetries) throws IOException, InterruptedException, MessagingException {
        final ExecResult result = execRSpamd("", messageText);

        if (result.getExitCode() == 0) {
            final double score = result.getScore();
            if (Double.isNaN(score)) {
                logger.warn("Got strange or no result from rspamc" +
                        "\nlength of input=" + result.getInput().length() +
                        "\nstdout=" + result.getStdout() +
                        "\nstderr=" + result.getStderr() +
                        "\nWill do another " + numberOfRetries + " tries!");
                if (numberOfRetries > 0) {
                    return apply(msg, messageText, numberOfRetries - 1);
                }
            } else {
                if (score >= cmdArgs.getSpamScore()) {
                    processAsSpam(msg, result);
                } else if (score >= cmdArgs.getTomatoScore()) {
                    processAsTomato(msg, result);
                } else {
                    processAsHam(msg, result);
                }
                msg.setFlags(CheckInboxFlag, true);
            }
        } else {
            // Hmmm. Something happened. I think it is best we don't do anything
            logger.warn("Running rspamc failed. ExitCode=" + result.getExitCode() +
                    ", stdout=" + result.getStdout() +
                    ", stderr=" + result.getStderr());
        }
        return result;
    }

    private void processAsSpam(final IMAPMessage msg, final ExecResult result) throws MessagingException {
        logger.info("SPAM [" + result.getScore() + "]");
        genericProcess(cmdArgs.getSpamActions(), msg, getSpamFolder(), result);
    }

    private void processAsTomato(final IMAPMessage msg, final ExecResult result) throws MessagingException {
        logger.info("TOMATO [" + result.getScore() + "]");
        genericProcess(cmdArgs.getTomatoActions(), msg, getTomatoFolder(), result);
    }

    private void processAsHam(final IMAPMessage msg, final ExecResult result) throws MessagingException {
        logger.info("HAM [" + result.getScore() + "]");
        genericProcess(cmdArgs.getHamActions(), msg, getHamFolder(), result);
    }

    private void genericProcess(final List<String> actions, final IMAPMessage msg, final IMAPFolder destFolder,
                                final ExecResult result) throws MessagingException {

        MimeMessage copyOfMessage = null;

        for (final String action : actions) {
            switch (action) {
                case "copy" -> {
                    if (copyOfMessage == null) {
                        // Original message has not been changed ==> directly copy the message on the server
                        srcFolder.copyMessages(new Message[]{msg}, destFolder);
                    } else {
                        // Add the copy of the message to the destination folder
                        openFolder(destFolder);
                        destFolder.addMessages(new Message[]{copyOfMessage});
                    }
                    logger.info("Copy to " + destFolder.getName());
                }
                case "move" -> {
                    moveTo(copyOfMessage, msg, destFolder);
                }
                case "update" -> {
                    if (copyOfMessage != null) {
                        // Add the copy of the message to the source folder and delete the original.
                        // Do nothing if the message has not been changed!
                        openFolder(srcFolder);

                        copyOfMessage.setFlag(Flags.Flag.SEEN, false);
//                        srcFolder.addMessages(new Message[]{copyOfMessage});
                        srcFolder.appendMessages(new Message[]{copyOfMessage});

                        msg.setFlag(Flags.Flag.DELETED, true);

                        logger.info("Update in " + srcFolder.getName());
                    }
                }
                case "addHeader" -> {
                    final String[] existingScore = msg.getHeader(X_IMAP_RSPAMD_SCORE);
                    final double newScore = result.getScore();
                    if (existingScore == null || !equalScore(existingScore[0], newScore)) {
                        copyOfMessage = createCopyOfMessage(msg, copyOfMessage);
                        copyOfMessage.removeHeader(X_IMAP_RSPAMD_SPAM);
                        copyOfMessage.addHeader(X_IMAP_RSPAMD_SPAM, Boolean.toString(result.isSpam()));

                        copyOfMessage.removeHeader(X_IMAP_RSPAMD_SCORE);
                        copyOfMessage.addHeader(X_IMAP_RSPAMD_SCORE, Double.toString(newScore));

                        copyOfMessage.removeHeader(X_IMAP_RSPAMD_SYMBOLS);
                        copyOfMessage.addHeader(X_IMAP_RSPAMD_SYMBOLS, result.getSymbolText());

                        logger.info("Added X_IMAP_RSPAMD headers");
                    }
                }
                case "rewriteSubject", "rewriteSubject1" -> {
                    copyOfMessage = createCopyOfMessage(msg, copyOfMessage);
                    final String oldSubject = getOldSubject(msg);
                    final String newSubject = createNewSubject1(oldSubject, result);
                    copyOfMessage.setSubject(newSubject);
                    rememberOriginalSubject(copyOfMessage, oldSubject);

                    logger.info("Rewrite subject(1) to " + newSubject);
                }
                case "rewriteSubject2" -> {
                    copyOfMessage = createCopyOfMessage(msg, copyOfMessage);
                    final String oldSubject = getOldSubject(msg);
                    final String newSubject = createNewSubject2(oldSubject, result);
                    copyOfMessage.setSubject(newSubject);
                    rememberOriginalSubject(copyOfMessage, oldSubject);

                    logger.info("Rewrite subject(2) to " + newSubject);
                }
                case "rewriteSubject3" -> {
                    copyOfMessage = createCopyOfMessage(msg, copyOfMessage);
                    final String oldSubject = getOldSubject(msg);
                    final String newSubject = createNewSubject3(oldSubject, result);
                    copyOfMessage.setSubject(newSubject);
                    rememberOriginalSubject(copyOfMessage, oldSubject);

                    logger.info("Rewrite subject(3) to " + newSubject);
                }
                case "delete" -> {
                    msg.setFlag(Flags.Flag.DELETED, true);
                    logger.info("Delete message");
                }
                case "trash" -> {
                    moveTo(copyOfMessage, msg, getTrashFolder());
                }
                case "noop" -> {
                    // No-Operation
                }
                default -> {
                    throw new IllegalArgumentException("Unknown action: " + action);
                }
            }
        }
    }

    private static void rememberOriginalSubject(final MimeMessage copyOfMessage, final String oldSubject) throws MessagingException {
        // Remember the original subject
        copyOfMessage.removeHeader(X_IMAP_RSPAMD_SUBJECT);
        copyOfMessage.addHeader(X_IMAP_RSPAMD_SUBJECT, oldSubject);
    }

    @NotNull
    private MimeMessage createCopyOfMessage(final IMAPMessage msg, MimeMessage copyOfMessage) throws MessagingException {
        if (copyOfMessage == null) {
            copyOfMessage = new MimeMessage(msg);
            copyFlags(msg, copyOfMessage);
            copyOfMessage.setFlags(CheckInboxFlag, true);
        }
        return copyOfMessage;
    }

    private boolean equalScore(final @NotNull String oldScore, final double newScore) {
        return Double.parseDouble(oldScore) == newScore;
    }

    private void copyFlags(final IMAPMessage srcMessage, final MimeMessage destMessage) throws MessagingException {
        final Flags srcFlags = srcMessage.getFlags();
        restoreFlags(destMessage, srcFlags);
        logger.debug("SrcFlags: " + srcFlags + ", DestFlag: " + destMessage.getFlags());
    }

    private void openFolder(final Folder folder) throws MessagingException {
        if (!folder.isOpen()) {
            folder.open(Folder.READ_WRITE);
        }
    }

    private void moveTo(final MimeMessage copyOfMessage, final Message msg, final IMAPFolder destFolder) throws MessagingException {
        if (copyOfMessage == null) {
            // Original message has not been changed ==> directly move the message on the server to the destination folder
            srcFolder.moveMessages(new Message[]{msg}, destFolder);
        } else {
            // Add the copy of the message to the destination folder and delete the original
            openFolder(destFolder);
            destFolder.addMessages(new Message[]{copyOfMessage});
            msg.setFlag(Flags.Flag.DELETED, true);
        }
        logger.info("Move to " + destFolder.getName());
    }

    private String getOldSubject(final IMAPMessage msg) throws MessagingException {
        final String[] header = msg.getHeader(X_IMAP_RSPAMD_SUBJECT);
        if (ArrayUtils.isNotEmpty(header)) {
            return header[0];
        }
        return StringUtils.defaultString(msg.getSubject());
    }

    private String createNewSubject1(final String oldSubject, final ExecResult result)  {
        final String newSubject = cmdArgs.getNewSubject1();
        return createNewSubject(oldSubject, result, newSubject);
    }

    private String createNewSubject2(final String oldSubject, final ExecResult result)  {
        final String newSubject = cmdArgs.getNewSubject2();
        return createNewSubject(oldSubject, result, newSubject);
    }

    private String createNewSubject3(final String oldSubject, final ExecResult result)  {
        final String newSubject = cmdArgs.getNewSubject3();
        return createNewSubject(oldSubject, result, newSubject);
    }

    @NotNull
    private static String createNewSubject(final String oldSubject, final ExecResult result, String newSubject) {
        newSubject = newSubject.replace("%s", oldSubject);
        newSubject = newSubject.replace("%c", Double.toString(result.getScore()));
        return newSubject;
    }

    protected void close() throws MessagingException {
        if (spamFolder != null && spamFolder.isOpen()) {
            spamFolder.close();
        }
        spamFolder = null;

        if (tomatoFolder != null && tomatoFolder.isOpen()) {
            tomatoFolder.close();
        }
        tomatoFolder = null;

        if (hamFolder != null && hamFolder.isOpen()) {
            hamFolder.close();
        }
        hamFolder = null;

        if (trashFolder != null && trashFolder.isOpen()) {
            trashFolder.close();
        }
        trashFolder = null;
    }

    private IMAPFolder getHamFolder() throws MessagingException {
        if (hamFolder == null) {
            hamFolder = (IMAPFolder) store.getFolder(cmdArgs.getHamFolder());
        }
//            hamFolder.open(Folder.READ_WRITE);
        return hamFolder;
    }

    private IMAPFolder getSpamFolder() throws MessagingException {
        if (spamFolder == null) {
            spamFolder = (IMAPFolder) store.getFolder(cmdArgs.getSpamFolder());
        }
//            spamFolder.open(Folder.READ_WRITE);
        return spamFolder;
    }

    private IMAPFolder getTomatoFolder() throws MessagingException {
        if (tomatoFolder == null) {
            tomatoFolder = (IMAPFolder) store.getFolder(cmdArgs.getTomatoFolder());
        }
//            tomatoFolder.open(Folder.READ_WRITE);
        return tomatoFolder;
    }

    private IMAPFolder getTrashFolder() throws MessagingException {
        if (trashFolder == null) {
            trashFolder = (IMAPFolder) store.getFolder(cmdArgs.getTomatoFolder());
        }
//            trashFolder.open(Folder.READ_WRITE);
        return trashFolder;
    }

    @Override
    public String getName() {
        return "CheckInbox";
    }
}
