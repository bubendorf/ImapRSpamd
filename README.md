# ImapRSpamd

Bridge from IMAP to Rspamd

ImapRSpamd runs in the background and reads eMails from an IMAP mail server, 
pass it through Rspamd and update/copy/move/delete the message based on the spam score.

ImapRSpamd uses rspamd to get the spam score and distinguished between three states:
- ham: The mail most likely is not a spam mail
- tomato: The mail probably is a spam mail
- spam: The mail most likely is a spam mail


## Prerequisites

- Java runtime environment 14 or newer
- rspamd (https://rspamd.com/)

## Build

- Clone the repo
- run "gradle build"


## Usage

```
Usage: java -jar ImapRSpamd-1.4.1-all.jar  [options]
Options:
-cmd, --command
listFolders, learnSpam, learnHam, checkInbox, stat, idle
        listFolders: List the IMAP folders on the server
	    learnSpam: Learn the mails from the spam folder as spam
    	learnHam: Learn the mails from the ham folder as ham
	    checkInbox: Check the mails in the inbox and perform the hamAction, tomatoAction or the spamAction
	    stat: Show output of the rspamc stat command
	    idle: Do not exit but wait for new mails
-f, --force
Process even already processed mails
Default: false
--hamAction
addHeader, rewriteSubject, update, move, copy, delete, trash, noop
        addHeader: Add the X-ImapRSpamd-XXX headers to the message
        rewriteSubject: Replace the subject of the message
        update: Update the message (if changed)
        move: Move the message to the ham/tomato/spam folder
        copy: Copy the message to the ham/tomate/spam folder
        delete: Delete the message
        trash: Moce the message to the trash folder
        noop: Do nothing
    --hamFolder
      Name of the HAM folder
      Default: ham
    -h, --help
      Show this help
    -H, --host
      Host name
    --idleFolder
      Name of the IDLE folder. Default: The same as --inboxFolder
    --idleTimeout
      IDLE timeout in seconds. Default: 1790
      Default: 1790
    -i, --inboxFolder
      Name of the INBOX folder
      Default: INBOX
    --maxMessages
      Process at most this many messages
      Default: 2147483647
    --maxSize
      Maximum message size.
      Default: 1048576
    --messageId
      Only process mails with given id
    --newSubject
      Rewritten subject. %s=original subject, %c=Score
      Default: [SPAM %c] %s
    -pw, --password
      Password to login to the server
    -p, --port
      Port number
      Default: 993
    --protocol
      Protocol: imap or imaps
      Default: imaps
    --receivedDateAfter
      Skip messages received before the date/time
    --receivedDateBefore
      Skip messages received after the date/time
    --rspamc
      Commandline for rspamc
      Default: rspamc
    --skipMessages
      Skip that many messages
      Default: 0
    --spamAction
      addHeader, rewriteSubject, update, move, copy, delete, trash, noop
      Default: addHeader,move
    -s, --spamFolder
      Name of the SPAM folder
      Default: Junk
    --spamScore
      Spam score. Mails with a higher score are treated as spam
      Default: 18.0
    --ssltrust
      Trust all SSL/TSL certificates
    --starttls
      Use STARTTLS
      Default: false
    --systemd
      Run as a systemd service. Send watchdog messages.
      Default: false
    --tomatoAction
      addHeader, rewriteSubject, update, move, copy, delete, trash, noop
      Default: addHeader,move
    --tomatoFolder
      Name of the TOMATO folder
      Default: Junk
    --tomatoScore
      Tomato score. Mails with a higer score are treated as tomato mails
      Default: 8.0
    --trashFolder
      Name of the TRASH folder
      Default: Trash
    -u, --user
      User name to login to the server
    -v, --verbose
      Be verbose
      Default: false
```

## Config files

All command line parameters may be written in one of the following files (Everything on a new line!):
```
/etc/imaprspamd/default.conf
~/default.conf
./default.conf
/etc/imaprspamd/local.conf
~/local.conf
./local.conf
```

### default.conf

```
--rspamc
rspamc --pass-all
--inboxFolder
INBOX
--hamFolder
ham
--tomatoFolder
Junk
--spamFolder
spam-n042
--hamAction
addHeader
--tomatoAction
addHeader,move
--spamAction
addHeader,move
```