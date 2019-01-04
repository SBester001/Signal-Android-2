package org.thoughtcrime.securesm2.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.mms.pdu_alt.SendConf;

import org.thoughtcrime.securesm2.transport.UndeliverableMessageException;


public interface OutgoingMmsConnection {
  @Nullable
  SendConf send(@NonNull byte[] pduBytes, int subscriptionId) throws UndeliverableMessageException;
}
