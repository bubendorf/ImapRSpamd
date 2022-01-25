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
        final ExecResult result = execRSpamd("learn_ham --pass-all", messageText);
        logger.info("Success = " + result.isSuccess() + ", ScanTime = " + result.getScanTime());
        if (!result.isSuccess()) {
            logger.info(result.getError());
        }
        return result;
    }
}
