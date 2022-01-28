package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;

public class ListFoldersCmd extends BaseCommand {

    protected final IMAPStore store;

    public ListFoldersCmd(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs);
        this.store = store;
    }

    public void run() throws MessagingException {
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder[] folders = defaultFolder.list("*");
        for (final Folder folder : folders) {
            logger.info(folder.getFullName());
        }
    }
}
