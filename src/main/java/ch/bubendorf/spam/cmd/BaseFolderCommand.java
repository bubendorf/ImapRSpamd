package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;

import static jakarta.mail.Flags.Flag.*;

public abstract class BaseFolderCommand extends BaseCommand {

    protected static final Flags CheckInboxFlag = new Flags("ImapRSpamd");
    protected static final Flags LearnHamFlag = new Flags("ImapRSpamdHam");
    protected static final Flags LearnSpamFlag = new Flags("ImapRSpamdSpam");

    protected final IMAPStore store;
    protected IMAPFolder srcFolder;

    protected BaseFolderCommand(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs);
        this.store = store;
    }

    public void run() throws MessagingException, IOException, InterruptedException {
        srcFolder = (IMAPFolder) store.getFolder(getFolderName());
        srcFolder.open(Folder.READ_WRITE);
        final Message[] genericMessages = srcFolder.getMessages();
        final IMAPMessage[] messages = Arrays.copyOf(genericMessages, genericMessages.length, IMAPMessage[].class);
        int count = 0;
        int skipped = 0;
        int success = 0;
        int ignored = 0;
        int error = 0;
        int ham = 0;
        int tomato = 0;
        int spam = 0;
        for (final IMAPMessage msg : messages) {
            msg.setPeek(true);
//            logger.debug("Flags : " + msg.getFlags());
            if (cmdArgs.isForce() || (isEligible(msg) &&
                    isReceivedAfter(msg) &&
                    isReceivedBefore(msg) &&
                    isSmallEnough(msg))) {
                if (cmdArgs.getSkipMessages() > skipped) {
                    logger.debug("Skipped " + getMessageId(msg) + "/" + getFrom(msg) + "/" + msg.getSubject());
                    skipped++;
                    continue;
                }
                count++;
                if (count > cmdArgs.getMaxMessages()) {
                    break;
                }
                logger.info(getMessageId(msg) + "/" + getFrom(msg) + "/" + msg.getSubject());
                final StringBuilder sb = getMailText(msg);
                if (sb.length() == 0) {
                    logger.error("PANIC: Could not download message text!");
                    System.exit(1);
                }
                final ExecResult result = apply(msg, sb.toString());
                if (result.isIgnore()) {
                    ignored++;
                } else if (result.isSuccess() || result.hasSymbols()) {
                    success++;
                } else {
                    error++;
                }
                final double score = result.getScore();
                if (!Double.isNaN(score)) {
                    if (score >= cmdArgs.getSpamScore()) {
                        spam++;
                    } else if (score >= cmdArgs.getTomatoScore()) {
                        tomato++;
                    } else {
                        ham++;
                    }
                }
            } else {
                logger.debug("Skipped " + getMessageId(msg) + "/" + getFrom(msg) + "/" + msg.getSubject());
                skipped++;
            }
        }
        close();
        srcFolder.close(true);
        logger.info(getName() + " : Skipped : " + skipped + ", Success : " + success + ", Ignored : " + ignored + ", Error : " + error);
        if (ham > 0 || tomato > 0 || spam > 0) {
            logger.info("Ham : " + ham + ", Tomato : " + tomato + ", Spam : " + spam);
        }
    }

    @NotNull
    protected String getFrom(final Message msg) throws MessagingException {
        final Address[] from = msg.getFrom();
        if (from == null || from.length == 0) {
            return "";
        }
        return from[0].toString();
    }

    protected void close() throws MessagingException {
        // Empty
    }

    protected boolean isEligible(final IMAPMessage msg) throws MessagingException {
        if (CollectionUtils.isNotEmpty(cmdArgs.getMessageIds())) {
            final String id = msg.getMessageID();
            return cmdArgs.getMessageIds().contains(id);
        }
        return true; // Override in subclass!
    }

    protected abstract String getFolderName();

    protected abstract ExecResult apply(final IMAPMessage msg, final String messageText) throws IOException, InterruptedException, MessagingException;

    protected String getMessageId(final IMAPMessage msg) throws MessagingException {
        final String[] headers = msg.getHeader("Message-ID");
        return ArrayUtils.isNotEmpty(headers) ? headers[0] : null;
    }

    private StringBuilder getMailText(final IMAPMessage msg) throws MessagingException, IOException {
        final StringBuilder sb = new StringBuilder(1024);
        final Enumeration<Header> headers = msg.getAllHeaders();
        while (headers.hasMoreElements()) {
            final Header header = headers.nextElement();
            sb.append(header.getName()).append(": ").append(header.getValue()).append("\n");
        }
        sb.append("\n");

        final String body = new String(msg.getRawInputStream().readAllBytes(), StandardCharsets.UTF_8);

        sb.append(body);
        sb.append("\n");

        return sb;
    }

    private static final Flags.Flag[] systemFlags = new Flags.Flag[]{FLAGGED, ANSWERED, DELETED, SEEN, DRAFT, USER};

    protected void restoreFlags(final MimeMessage msg, final Flags flags) throws MessagingException {
        // Restore system flags
        for (final Flags.Flag flag : systemFlags) {
            final boolean isSet = flags.contains(flag);
            msg.setFlag(flag, isSet);
        }

        // Restore user flags
        for (final String flag : flags.getUserFlags()) {
            msg.setFlags(new Flags(flag), true);
        }
    }

}
