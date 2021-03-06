package ch.bubendorf.spam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExecResultTest {
    private ExecResult execResult;

    @BeforeEach
    public void setup() {
        execResult = new ExecResult("", 0, """
                Results for file: stdin (0.795 seconds)
                [Metric: default]
                Action: reject
                Spam: true
                Score: 24.99 / 15.00
                Symbol: ABUSE_SURBL (5.50)[yourtopicgroup.com.de:url]
                Symbol: ARC_NA (0.00)
                Symbol: ARC_SIGNED (0.00)[i=1]
                Symbol: ASN (0.00)[asn:138749, ipnet:103.141.227.0/24, country:IN]
                Symbol: BAYES_SPAM (5.07)[99.93%]
                Symbol: DATE_IN_PAST (1.00)
                Symbol: DKIM_TRACE (0.00)[yourtopicgroup.com.de:-]
                Symbol: DMARC_POLICY_QUARANTINE (1.50)[yourtopicgroup.com.de : No valid SPF, quarantine]
                Symbol: FROM_HAS_DN (0.00)
                Symbol: FUZZY_DENIED (7.66)[1:a1d6f40e6d:0.53:txt]
                Symbol: HAS_LIST_UNSUB (-0.01)
                Symbol: HAS_REPLYTO (0.00)[reply@yourtopicgroup.com.de]
                Symbol: HTML_SHORT_LINK_IMG_1 (2.00)
                Symbol: MANY_INVISIBLE_PARTS (0.30)[4]
                Symbol: MID_RHS_MATCH_FROM (0.00)
                Symbol: MIME_GOOD (-0.10)[multipart/alternative, text/plain]
                Symbol: MIME_TRACE (0.00)[0:+, 1:+, 2:~]
                Symbol: ONCE_RECEIVED (0.10)
                Symbol: PREVIOUSLY_DELIVERED (0.00)[markus@bubendorf.ch]
                Symbol: RCPT_COUNT_ONE (0.00)[1]
                Symbol: RCVD_COUNT_ONE (0.00)[1]
                Symbol: RCVD_TLS_ALL (0.00)
                Symbol: REPLYTO_DOM_EQ_FROM_DOM (0.00)
                Symbol: RWL_MAILSPIKE_POSSIBLE (0.00)[103.141.227.65:from]
                Symbol: R_DKIM_REJECT (1.00)[yourtopicgroup.com.de:s=key]
                Symbol: R_PARTS_DIFFER (0.97)[98.6%]
                Symbol: R_SPF_NA (0.00)[No domain]
                Symbol: SUBJECT_ENDS_EXCLAIM (0.00)
                Symbol: TO_DN_NONE (0.00)
                Message-ID: 3-n17PpSxTecfI-pCl9ueit4n6x@yourtopicgroup.com.de
                Urls: ["fonts.googleapis.com","click.yourtopicgroup.com.de"]
                Emails: []""", "");
    }

    @Test
    public void getExitCode() {
        assertEquals(0, execResult.getExitCode());
    }

    @Test
    public void getAction() {
        assertEquals("reject", execResult.getAction());
    }

    @Test
    public void isSpam() {
        assertTrue(execResult.isSpam());
    }

    @Test
    public void getScore() {
        assertEquals(24.99, execResult.getScore());
    }

    @Test
    public void getScoreThreshold() {
        assertEquals(15.0, execResult.getScoreThreshold());
    }

    @Test
    public void hasSymbols() {
        assertTrue(execResult.hasSymbols());
    }

    @Test
    public void getSymbols() {
        assertNotNull(execResult.getSymbols());
        assertEquals(29, execResult.getSymbols().size());
    }

    /*@Test
    public void getHeaderText() {
        assertEquals("Spam: true, Score: 24.99\r\n" +
                        "\tABUSE_SURBL (5.5)[yourtopicgroup.com.de:url], ARC_NA (0.0), ARC_SIGNED (0.0)[i=1], ASN (0.0)[asn:138749, ipnet:103.141.227.0/24, country:IN], \r\n" +
                        "\tBAYES_SPAM (5.07)[99.93%], DATE_IN_PAST (1.0), DKIM_TRACE (0.0)[yourtopicgroup.com.de:-], DMARC_POLICY_QUARANTINE (1.5)[yourtopicgroup.com.de : No valid SPF, quarantine], \r\n" +
                        "\tFROM_HAS_DN (0.0), FUZZY_DENIED (7.66)[1:a1d6f40e6d:0.53:txt], HAS_LIST_UNSUB (-0.01), HAS_REPLYTO (0.0)[reply@yourtopicgroup.com.de], \r\n" +
                        "\tHTML_SHORT_LINK_IMG_1 (2.0), MANY_INVISIBLE_PARTS (0.3)[4], MID_RHS_MATCH_FROM (0.0), MIME_GOOD (-0.1)[multipart/alternative, text/plain], \r\n" +
                        "\tMIME_TRACE (0.0)[0:+, 1:+, 2:~], ONCE_RECEIVED (0.1), PREVIOUSLY_DELIVERED (0.0)[markus@bubendorf.ch], \r\n" +
                        "\tRCPT_COUNT_ONE (0.0)[1], RCVD_COUNT_ONE (0.0)[1], RCVD_TLS_ALL (0.0), REPLYTO_DOM_EQ_FROM_DOM (0.0), \r\n" +
                        "\tRWL_MAILSPIKE_POSSIBLE (0.0)[103.141.227.65:from], R_DKIM_REJECT (1.0)[yourtopicgroup.com.de:s=key], \r\n" +
                        "\tR_PARTS_DIFFER (0.97)[98.6%], R_SPF_NA (0.0)[No domain], SUBJECT_ENDS_EXCLAIM (0.0), TO_DN_NONE (0.0)",
                execResult.getHeaderText());
    }*/

}
