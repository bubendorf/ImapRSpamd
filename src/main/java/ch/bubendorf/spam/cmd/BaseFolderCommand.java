package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

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
                    logger.debug("Skipped " + getMessageId(msg) + ": " + msg.getSubject());
                    skipped++;
                    continue;
                }
                count++;
                if (count > cmdArgs.getMaxMessages()) {
                    break;
                }
                logger.info(getMessageId(msg) + ": " + msg.getSubject());
                final StringBuilder sb = getMailText(msg);
                apply(msg, sb.toString());
            } else {
                logger.debug("Skipped " + getMessageId(msg) + ": " + msg.getSubject());
            }
        }
        close();
        srcFolder.close(true);
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

        final String body = new String(msg.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        sb.append(body);
        sb.append("\n");
        return sb;
    }

}
