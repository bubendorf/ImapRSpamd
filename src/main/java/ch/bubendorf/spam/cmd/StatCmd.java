package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;

import java.io.IOException;

public class StatCmd extends BaseCommand {

    public StatCmd(final CommandLineArguments cmdArgs) {
        super(cmdArgs);
    }

    public void run() throws IOException, InterruptedException {
        final ExecResult result = execRSpamd("stat", "");
        logger.info(result.getStdout());

    }
}
