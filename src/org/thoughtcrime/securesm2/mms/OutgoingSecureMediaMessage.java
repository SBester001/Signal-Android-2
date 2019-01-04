package org.thoughtcrime.securesm2.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesm2.attachments.Attachment;
import org.thoughtcrime.securesm2.contactshare.Contact;
import org.thoughtcrime.securesm2.recipients.Recipient;

import java.util.Collections;
import java.util.List;

public class OutgoingSecureMediaMessage extends OutgoingMediaMessage {

  public OutgoingSecureMediaMessage(Recipient recipient, String body,
                                    List<Attachment> attachments,
                                    long sentTimeMillis,
                                    int distributionType,
                                    long expiresIn,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts)
  {
    super(recipient, body, attachments, sentTimeMillis, -1, expiresIn, distributionType, quote, contacts, Collections.emptyList(), Collections.emptyList());
  }

  public OutgoingSecureMediaMessage(OutgoingMediaMessage base) {
    super(base);
  }

  @Override
  public boolean isSecure() {
    return true;
  }
}
