package org.thoughtcrime.securesm2.sms;

import org.thoughtcrime.securesm2.database.Address;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;

public class IncomingJoinedMessage extends IncomingTextMessage {

  public IncomingJoinedMessage(Address sender) {
    super(sender, 1, System.currentTimeMillis(), null, Optional.<SignalServiceGroup>absent(), 0, false);
  }

  @Override
  public boolean isJoined() {
    return true;
  }

  @Override
  public boolean isSecureMessage() {
    return true;
  }

}