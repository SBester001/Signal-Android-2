package org.thoughtcrime.securesm2.crypto.storage;

import android.content.Context;
import org.thoughtcrime.securesm2.logging.Log;

import org.thoughtcrime.securesm2.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesm2.crypto.SessionUtil;
import org.thoughtcrime.securesm2.database.Address;
import org.thoughtcrime.securesm2.database.DatabaseFactory;
import org.thoughtcrime.securesm2.database.IdentityDatabase;
import org.thoughtcrime.securesm2.database.IdentityDatabase.IdentityRecord;
import org.thoughtcrime.securesm2.database.IdentityDatabase.VerifiedStatus;
import org.thoughtcrime.securesm2.recipients.Recipient;
import org.thoughtcrime.securesm2.util.IdentityUtil;
import org.thoughtcrime.securesm2.util.TextSecurePreferences;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.concurrent.TimeUnit;

public class TextSecureIdentityKeyStore implements IdentityKeyStore {

  private static final int TIMESTAMP_THRESHOLD_SECONDS = 5;

  private static final String TAG = TextSecureIdentityKeyStore.class.getSimpleName();
  private static final Object LOCK = new Object();

  private final Context context;

  public TextSecureIdentityKeyStore(Context context) {
    this.context = context;
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return IdentityKeyUtil.getIdentityKeyPair(context);
  }

  @Override
  public int getLocalRegistrationId() {
    return TextSecurePreferences.getLocalRegistrationId(context);
  }

  public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey, boolean nonBlockingApproval) {
    synchronized (LOCK) {
      IdentityDatabase         identityDatabase = DatabaseFactory.getIdentityDatabase(context);
      Address                  signalAddress    = Address.fromExternal(context, address.getName());
      Optional<IdentityRecord> identityRecord   = identityDatabase.getIdentity(signalAddress);

      if (!identityRecord.isPresent()) {
        Log.i(TAG, "Saving new identity...");
        identityDatabase.saveIdentity(signalAddress, identityKey, VerifiedStatus.DEFAULT, true, System.currentTimeMillis(), nonBlockingApproval);
        return false;
      }

      if (!identityRecord.get().getIdentityKey().equals(identityKey)) {
        Log.i(TAG, "Replacing existing identity...");
        VerifiedStatus verifiedStatus;

        if (identityRecord.get().getVerifiedStatus() == VerifiedStatus.VERIFIED ||
            identityRecord.get().getVerifiedStatus() == VerifiedStatus.UNVERIFIED)
        {
          verifiedStatus = VerifiedStatus.UNVERIFIED;
        } else {
          verifiedStatus = VerifiedStatus.DEFAULT;
        }

        identityDatabase.saveIdentity(signalAddress, identityKey, verifiedStatus, false, System.currentTimeMillis(), nonBlockingApproval);
        IdentityUtil.markIdentityUpdate(context, Recipient.from(context, signalAddress, true));
        SessionUtil.archiveSiblingSessions(context, address);
        return true;
      }

      if (isNonBlockingApprovalRequired(identityRecord.get())) {
        Log.i(TAG, "Setting approval status...");
        identityDatabase.setApproval(signalAddress, nonBlockingApproval);
        return false;
      }

      return false;
    }
  }

  @Override
  public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
    return saveIdentity(address, identityKey, false);
  }

  @Override
  public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
    synchronized (LOCK) {
      IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(context);
      String           ourNumber        = TextSecurePreferences.getLocalNumber(context);
      Address          theirAddress     = Address.fromExternal(context, address.getName());

      if (ourNumber.equals(address.getName()) || Address.fromSerialized(ourNumber).equals(theirAddress)) {
        return identityKey.equals(IdentityKeyUtil.getIdentityKey(context));
      }

      switch (direction) {
        case SENDING:   return isTrustedForSending(identityKey, identityDatabase.getIdentity(theirAddress));
        case RECEIVING: return true;
        default:        throw new AssertionError("Unknown direction: " + direction);
      }
    }
  }

  @Override
  public IdentityKey getIdentity(SignalProtocolAddress address) {
    Optional<IdentityRecord> record = DatabaseFactory.getIdentityDatabase(context).getIdentity(Address.fromSerialized(address.getName()));

    if (record.isPresent()) {
      return record.get().getIdentityKey();
    } else {
      return null;
    }
  }

  private boolean isTrustedForSending(IdentityKey identityKey, Optional<IdentityRecord> identityRecord) {
    if (!identityRecord.isPresent()) {
      Log.w(TAG, "Nothing here, returning true...");
      return true;
    }

    if (!identityKey.equals(identityRecord.get().getIdentityKey())) {
      Log.w(TAG, "Identity keys don't match...");
      return false;
    }

    if (identityRecord.get().getVerifiedStatus() == VerifiedStatus.UNVERIFIED) {
      Log.w(TAG, "Needs unverified approval!");
      return false;
    }

    if (isNonBlockingApprovalRequired(identityRecord.get())) {
      Log.w(TAG, "Needs non-blocking approval!");
      return false;
    }

    return true;
  }

  private boolean isNonBlockingApprovalRequired(IdentityRecord identityRecord) {
    return !identityRecord.isFirstUse() &&
           System.currentTimeMillis() - identityRecord.getTimestamp() < TimeUnit.SECONDS.toMillis(TIMESTAMP_THRESHOLD_SECONDS) &&
           !identityRecord.isApprovedNonBlocking();
  }
}
