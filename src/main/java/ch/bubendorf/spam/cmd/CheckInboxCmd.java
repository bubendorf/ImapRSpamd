package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


import java.io.IOException;
import java.util.List;

public class CheckInboxCmd extends BaseFolderCommand {

    private static final Flags imapRSpamdFlag = new Flags("ImapRSpamd");

    private IMAPFolder hamFolder;
    private IMAPFolder tomatoFolder;
    private IMAPFolder spamFolder;

    public CheckInboxCmd(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs, store);
    }

    @Override
    protected String getFolderName() {
        return cmdArgs.getInboxFolder();
    }

    @Override
    protected boolean isEligible(final Message msg) throws MessagingException {
        final Flags flags = msg.getFlags();
        return !flags.contains(imapRSpamdFlag);
    }

    @Override
    protected ExecResult apply(final Message msg, final String messageText) throws IOException, InterruptedException, MessagingException {
        final ExecResult result = execRSpamd("", messageText);

        final double score = result.getScore();
        if (score >= cmdArgs.getSpamScore()) {
            processAsSpam(msg, result);
        } else if (score >= cmdArgs.getTomatoScore()) {
            processAsTomato(msg, result);
        } else {
            processAsHam(msg, result);
        }

        msg.setFlags(imapRSpamdFlag, true);

        return result;
    }

    private void processAsSpam(final Message msg, final ExecResult result) throws MessagingException {
        logger.info("SPAM");
        genericProcess(cmdArgs.getSpamActions(), msg, getSpamFolder(), result);
    }

    private void processAsTomato(final Message msg, final ExecResult result) throws MessagingException {
        logger.info("TOMATO");
        genericProcess(cmdArgs.getTomatoActions(), msg, getTomatoFolder(), result);
    }

    private void processAsHam(final Message msg, final ExecResult result) throws MessagingException {
        logger.info("HAM");
        genericProcess(cmdArgs.getHamActions(), msg, getHamFolder(), result);
    }

    private void genericProcess(final List<String> actions, final Message msg, final IMAPFolder destFolder,
                                final ExecResult result) throws MessagingException {

//        final boolean hasAddHeader = actions.contains("addHeader");
//        final boolean hasRewriteSubject = actions.contains("rewriteSubject");

        MimeMessage copyOfMessage = null;

        for (final String action : actions) {
            switch(action) {
                case "copy" -> {
                    if (copyOfMessage == null) {
                        // Directly copy the message on the server
                        folder.copyMessages(new Message[]{msg}, destFolder);
                    } else {
                        // Add the copy of the message to the destination folder
                        destFolder.addMessages(new Message[]{copyOfMessage});
                    }
                }
                case "move" -> {
                    if (copyOfMessage == null) {
                        // Directly move the message on the server
                        folder.moveMessages(new Message[]{msg}, destFolder);
                    } else {
                        // Add the copy of the message to the destination folder and delete the original
                        destFolder.addMessages(new Message[]{copyOfMessage});
                        msg.setFlag(Flags.Flag.DELETED, true);
                    }
                }
                case "update" -> {
                    if (copyOfMessage == null) {
                        // Nothing to.
                    } else {
                        // Add the copy of the message to the source folder and delete the original
                        folder.addMessages(new Message[]{copyOfMessage});
                        msg.setFlag(Flags.Flag.DELETED, true);
                    }
                }
                case "addHeader" -> {
                    if (copyOfMessage == null) {
                        copyOfMessage = new MimeMessage((MimeMessage) msg);
                    }
                    copyOfMessage.addHeader("X-ImapRSpamd", result.getHeaderText());
                }
                case "rewriteSubject" -> {
                    if (copyOfMessage == null) {
                        copyOfMessage = new MimeMessage((MimeMessage) msg);
                    }
                    copyOfMessage.setSubject(getNewSubject(msg, result));
                }
                case "delete" -> {
                    msg.setFlag(Flags.Flag.DELETED, true);
                }
                default -> {
                    throw new IllegalArgumentException("Unknown action: " + action);
                }
            }
        }
    }

    private String getNewSubject(final Message msg, final ExecResult result) throws MessagingException {
        String subject = cmdArgs.getNewSubject();
        subject = subject.replace("%s",msg.getSubject());
        subject = subject.replace("%c",Double.toString(result.getScore()));
        return subject;
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
    }

    private IMAPFolder getHamFolder() throws MessagingException {
        if (hamFolder == null) {
            hamFolder = (IMAPFolder)store.getFolder(cmdArgs.getHamFolder());
            hamFolder.open(Folder.READ_WRITE);
        }
        return hamFolder;
    }

    private IMAPFolder getSpamFolder() throws MessagingException {
        if (spamFolder == null) {
            spamFolder = (IMAPFolder)store.getFolder(cmdArgs.getSpamFolder());
            spamFolder.open(Folder.READ_WRITE);
        }
        return spamFolder;
    }

    private IMAPFolder getTomatoFolder() throws MessagingException {
        if (tomatoFolder == null) {
            tomatoFolder = (IMAPFolder)store.getFolder(cmdArgs.getTomatoFolder());
            tomatoFolder.open(Folder.READ_WRITE);
        }
        return tomatoFolder;
    }
}
