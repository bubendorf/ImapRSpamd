package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Message;

import java.io.IOException;

public class LearnSpamCmd extends BaseFolderCommand {

    public LearnSpamCmd(final CommandLineArguments cmdArgs, final IMAPStore store) {
        super(cmdArgs, store);
    }

    @Override
    protected String getFolderName() {
        return cmdArgs.getSpamFolder();
    }

    @Override
    protected ExecResult apply(final Message msg, final String messageText) throws IOException, InterruptedException {
        final ExecResult result = execRSpamd("learn_spam", messageText);
        logger.info("Success = " + result.isSuccess() + ", ScanTime = " + result.getScanTime());
        if (!result.isSuccess()) {
            logger.info(result.getError());
        }
        return result;
    }
}
