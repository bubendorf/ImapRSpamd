package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Message;

import java.io.IOException;

public class LearnHamCmd extends BaseFolderCommand {

    public LearnHamCmd(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs, store);
    }

    @Override
    protected String getFolderName() {
        return cmdArgs.getHamFolder();
    }

    @Override
    protected ExecResult apply(final Message msg, final String messageText) throws IOException, InterruptedException {
        return execRSpamd("learn_ham", messageText);
    }
}
