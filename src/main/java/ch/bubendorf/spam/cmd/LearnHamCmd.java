package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Flags;
import jakarta.mail.MessagingException;

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
    protected boolean isEligible(final IMAPMessage msg) throws MessagingException {
        final Flags flags = msg.getFlags();
        return super.isEligible(msg) && !flags.contains(LearnHamFlag);
    }

    @Override
    protected ExecResult apply(final IMAPMessage msg, final String messageText) throws IOException, InterruptedException, MessagingException {
        final ExecResult result = execRSpamd("learn_ham", messageText);
        logger.info("Success = " + result.isSuccess() + ", ScanTime = " + result.getScanTime());
        if (!result.isSuccess()) {
            logger.info(result.getError());
        }
        msg.setFlags(LearnHamFlag, true);
        return result;
    }

    @Override
    public String getName() {
        return "LearnHam";
    }
}
