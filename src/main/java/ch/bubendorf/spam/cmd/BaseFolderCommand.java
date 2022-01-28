package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.*;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import static jakarta.mail.Flags.Flag.*;

public abstract class BaseFolderCommand extends BaseCommand {
    protected final IMAPStore store;
    protected IMAPFolder srcFolder;

    protected BaseFolderCommand(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs);
        this.store = store;
    }

    public void run() throws MessagingException, IOException, InterruptedException {
        srcFolder = (IMAPFolder)store.getFolder(getFolderName());
        srcFolder.open(Folder.READ_WRITE);
        final Message[] messages = srcFolder.getMessages();
        int count = 0;
        int skipped = 0;
        for (final Message msg : messages) {
            if (cmdArgs.isForce() || (isEligible(msg) && isReceivedAfter(msg) && isReceivedBefore(msg))) {
                if (cmdArgs.getSkipMessages() > skipped) {
                    logger.debug("Skipped " + getMessageId(msg) + "/" + getFrom(msg) + "/" + msg.getSubject());
                    skipped++;
                    continue;
                }
                count++;
                if (count > cmdArgs.getMaxMessages()) {
                    break;
                }
                logger.debug(getMessageId(msg) + "/" + getFrom(msg) + "/" + msg.getSubject());
                final StringBuilder sb = getMailText(msg);
                apply(msg, sb.toString());
            } else {
                logger.debug("Skipped " + getMessageId(msg) + "/" + getFrom(msg) + "/" + msg.getSubject());
            }
        }
        close();
        srcFolder.close(true);
    }

    @NonNull
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

    protected boolean isEligible(final Message msg) throws MessagingException {
        return true; // Override in subclass!
    }

    protected abstract String getFolderName();
    protected abstract ExecResult apply(final Message msg, final String messageText) throws IOException, InterruptedException, MessagingException;

    protected String getMessageId(final Message msg) throws MessagingException {
        final String[] headers = msg.getHeader("Message-ID");
        return ArrayUtils.isNotEmpty(headers) ? headers[0] : null;
    }

    private StringBuilder getMailText(final Message msg) throws MessagingException, IOException {
        final StringBuilder sb = new StringBuilder(1024);
        final Enumeration<Header> headers = msg.getAllHeaders();
        while (headers.hasMoreElements()) {
            final Header header = headers.nextElement();
            sb.append(header.getName()).append(": ").append(header.getValue()).append("\n");
        }
        sb.append("\n");

        // Readig the mail text will at least set the \SEEN flag
        // ==> Read the flags and restore them afterwards
        final Flags flags = msg.getFlags();
        final String body = new String(msg.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        restoreFlags(msg, flags);

        sb.append(body);
        sb.append("\n");

        return sb;
    }

    private static final Flags.Flag[] systemFlags = new Flags.Flag[]{FLAGGED, ANSWERED, DELETED, SEEN, DRAFT, USER};

    protected void restoreFlags(final Message msg, final Flags flags) throws MessagingException {
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
