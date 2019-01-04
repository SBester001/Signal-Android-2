package org.thoughtcrime.securesm2.mms;

import org.thoughtcrime.securesm2.attachments.Attachment;
import org.thoughtcrime.securesm2.database.ThreadDatabase;
import org.thoughtcrime.securesm2.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

  public OutgoingExpirationUpdateMessage(Recipient recipient, long sentTimeMillis, long expiresIn) {
    super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn, null, Collections.emptyList());
  }

  @Override
  public boolean isExpirationUpdate() {
    return true;
  }

}
