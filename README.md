# ImapRSpamd

Bridge from IMAP to Rspamd

Read eMails from an IMAP server, pass it through Rspamd and update/copy/move/delete the message based on the spam score.

## Build

- Clone the repo
- gradle build


## Usage

`Usage: <main class> [options]

  Options:
    -cmd, --command
      Default: []      
    -f, --force
      Default: false
    --hamAction
      addHeader, rewriteSubject, update, move, copy, delete, trash
      Default: []
    --hamFolder
      Name of the HAM folder
      Default: ham
    -h, --help
    -H, --host
      Host name
    --idleFolder
      Name of the IDLE folder
    -i, --inbox, --inboxFolder
      Name of the INBOX folder
      Default: INBOX
    --maxMessages
      Process at most this many messages
      Default: 2147483647
    --newSubject
      Rewritten subject. %s=original Subject, %c=Score
      Default: [SPAM %c] %s
    -pw, --password
      Password
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
      addHeader, rewriteSubject, update, move, copy, delete, trash
      Default: []
    -s, --spamFolder
      Name of the SPAM folder
      Default: Junk
    --spamScore
      Spam score
      Default: 15.0
    --ssltrust
      Trust TSL certificate errors
    --starttls
      Use STARTTLS
      Default: false
    --tomatoAction
      addHeader, rewriteSubject, update, move, copy, delete, trash
      Default: []
    --tomatoFolder
      Name of the TOMATO folder
      Default: Junk
    --tomatoScore
      Tomato score
      Default: 6.0
    --trashFolder
      Name of the TRASH folder
      Default: Trash
    -u, --user
      User name
    -v, --verbose
      Be verbose
      Default: false
`      
