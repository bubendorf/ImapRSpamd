package ch.bubendorf.spam;

import java.io.*;

public class StreamGobbler implements Runnable {
    private final InputStream inputStream;
    private String result;
    private static final String lineSeparator = System.getProperty("line.separator");

    public StreamGobbler(final InputStream is) {
        inputStream = is;
    }

    public String getResult() {
        return result;
    }

    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(inputStream);
            br = new BufferedReader(isr);
            final StringBuilder output = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
                output.append(lineSeparator);
            }
            result = output.toString();
        } catch (final IOException e) {
            result = e.getMessage();
        } finally {
            close(isr);
            close(br);
        }
    }

    private void close(final Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (final IOException e) {
                //e.printStackTrace();
            }
        }
    }
}
