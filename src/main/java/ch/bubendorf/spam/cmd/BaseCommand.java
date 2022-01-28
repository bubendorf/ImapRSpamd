package ch.bubendorf.spam.cmd;

import ch.bubendorf.spam.CommandLineArguments;
import ch.bubendorf.spam.ExecResult;
import ch.bubendorf.spam.StreamGobbler;
import com.sun.mail.imap.IMAPMessage;
import jakarta.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseCommand {

    protected final static Logger logger = LoggerFactory.getLogger(BaseCommand.class);
    protected final CommandLineArguments cmdArgs;

    protected BaseCommand(final CommandLineArguments cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    protected ExecResult execRSpamd(final String parameters, final String stdinText) throws IOException, InterruptedException {
        final ProcessBuilder builder = new ProcessBuilder();
        final String cmdLine = cmdArgs.getRspamc() + " " + parameters;
//        final String cmdLine = "C:\\Windows\\System32\\OpenSSH\\ssh.exe mbu@n020 cat";

        logger.debug(cmdLine);
        final String[] cmds = cmdLine.split(" ");
        builder.command(cmds);

        builder.directory(new File(System.getProperty("user.home")));
        final Process process = builder.start();

        if (StringUtils.isNotEmpty(stdinText)) {
            final OutputStream outputStream = process.getOutputStream();
            outputStream.write(stdinText.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
        }

        final ExecutorService executorService = Executors.newCachedThreadPool();

        final StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream());
        executorService.submit(stdoutGobbler);
        final StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream());
        executorService.submit(stderrGobbler);
        final int exitCode = process.waitFor();

        executorService.shutdown();

        return new ExecResult(exitCode, stdoutGobbler.getResult(), stderrGobbler.getResult());
    }

    protected boolean isSmallEnough(final IMAPMessage msg) throws MessagingException {
        return msg.getSize() <= cmdArgs.getMaxSize();
    }

    protected boolean isReceivedAfter(final IMAPMessage msg) throws MessagingException {
        if (cmdArgs.getReceivedDateAfter() == null) {
            // No receivedAfter date specified ==> Process it
            return true;
        }
        final Date receivedDate = msg.getReceivedDate();
        return receivedDate == null || !receivedDate.before(cmdArgs.getReceivedDateAfter());
    }

    protected boolean isReceivedBefore(final IMAPMessage msg) throws MessagingException {
        if (cmdArgs.getReceivedDateBefore() == null) {
            // No receivedBefore date specified ==> Process it
            return true;
        }
        final Date receivedDate = msg.getReceivedDate();
        return receivedDate == null || !receivedDate.after(cmdArgs.getReceivedDateBefore());
    }

}
