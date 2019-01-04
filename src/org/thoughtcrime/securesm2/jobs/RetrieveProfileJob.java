package org.thoughtcrime.securesm2.jobs;


import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.thoughtcrime.securesm2.ApplicationContext;
import org.thoughtcrime.securesm2.crypto.UnidentifiedAccessUtil;
import org.thoughtcrime.securesm2.database.Address;
import org.thoughtcrime.securesm2.database.DatabaseFactory;
import org.thoughtcrime.securesm2.database.RecipientDatabase;
import org.thoughtcrime.securesm2.database.RecipientDatabase.UnidentifiedAccessMode;
import org.thoughtcrime.securesm2.dependencies.InjectableType;
import org.thoughtcrime.securesm2.jobmanager.JobParameters;
import org.thoughtcrime.securesm2.jobmanager.SafeData;
import org.thoughtcrime.securesm2.logging.Log;
import org.thoughtcrime.securesm2.recipients.Recipient;
import org.thoughtcrime.securesm2.service.IncomingMessageObserver;
import org.thoughtcrime.securesm2.util.Base64;
import org.thoughtcrime.securesm2.util.IdentityUtil;
import org.thoughtcrime.securesm2.util.Util;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessagePipe;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.crypto.ProfileCipher;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccess;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.profiles.SignalServiceProfile;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.util.InvalidNumberException;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class RetrieveProfileJob extends ContextJob implements InjectableType {

  private static final String TAG = RetrieveProfileJob.class.getSimpleName();

  private static final String KEY_ADDRESS = "address";

  @Inject transient SignalServiceMessageReceiver receiver;

  private Recipient recipient;

  public RetrieveProfileJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public RetrieveProfileJob(Context context, Recipient recipient) {
    super(context, JobParameters.newBuilder()
                                .withNetworkRequirement()
                                .withRetryCount(3)
                                .create());

    this.recipient = recipient;
  }

  @Override
  protected void initialize(@NonNull SafeData data) {
    recipient = Recipient.from(context, Address.fromSerialized(data.getString(KEY_ADDRESS)), true);
  }

  @Override
  protected @NonNull Data serialize(@NonNull Data.Builder dataBuilder) {
    return dataBuilder.putString(KEY_ADDRESS, recipient.getAddress().serialize()).build();
  }

  @Override
  public void onRun() throws IOException, InvalidKeyException {
    try {
      if (recipient.isGroupRecipient()) handleGroupRecipient(recipient);
      else                              handleIndividualRecipient(recipient);
    } catch (InvalidNumberException e) {
      Log.w(TAG, e);
    }
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return false;
  }

  @Override
  public void onCanceled() {}

  private void handleIndividualRecipient(Recipient recipient)
      throws IOException, InvalidKeyException, InvalidNumberException
  {
    String                       number             = recipient.getAddress().toPhoneString();
    Optional<UnidentifiedAccess> unidentifiedAccess = getUnidentifiedAccess(recipient);

    SignalServiceProfile profile;

    try {
      profile = retrieveProfile(number, unidentifiedAccess);
    } catch (NonSuccessfulResponseCodeException e) {
      if (unidentifiedAccess.isPresent()) {
        profile = retrieveProfile(number, Optional.absent());
      } else {
        throw e;
      }
    }

    setIdentityKey(recipient, profile.getIdentityKey());
    setProfileName(recipient, profile.getName());
    setProfileAvatar(recipient, profile.getAvatar());
    setUnidentifiedAccessMode(recipient, profile.getUnidentifiedAccess(), profile.isUnrestrictedUnidentifiedAccess());
  }

  private void handleGroupRecipient(Recipient group)
      throws IOException, InvalidKeyException, InvalidNumberException
  {
    List<Recipient> recipients = DatabaseFactory.getGroupDatabase(context).getGroupMembers(group.getAddress().toGroupString(), false);

    for (Recipient recipient : recipients) {
      handleIndividualRecipient(recipient);
    }
  }

  private SignalServiceProfile retrieveProfile(@NonNull String number, Optional<UnidentifiedAccess> unidentifiedAccess)
      throws IOException
  {
    SignalServiceMessagePipe authPipe         = IncomingMessageObserver.getPipe();
    SignalServiceMessagePipe unidentifiedPipe = IncomingMessageObserver.getUnidentifiedPipe();
    SignalServiceMessagePipe pipe             = unidentifiedPipe != null && unidentifiedAccess.isPresent() ? unidentifiedPipe
                                                                                                           : authPipe;

    if (pipe != null) {
      try {
        return pipe.getProfile(new SignalServiceAddress(number), unidentifiedAccess);
      } catch (IOException e) {
        Log.w(TAG, e);
      }
    }

    return receiver.retrieveProfile(new SignalServiceAddress(number), unidentifiedAccess);
  }

  private void setIdentityKey(Recipient recipient, String identityKeyValue) {
    try {
      if (TextUtils.isEmpty(identityKeyValue)) {
        Log.w(TAG, "Identity key is missing on profile!");
        return;
      }

      IdentityKey identityKey = new IdentityKey(Base64.decode(identityKeyValue), 0);

      if (!DatabaseFactory.getIdentityDatabase(context)
                          .getIdentity(recipient.getAddress())
                          .isPresent())
      {
        Log.w(TAG, "Still first use...");
        return;
      }

      IdentityUtil.saveIdentity(context, recipient.getAddress().toPhoneString(), identityKey);
    } catch (InvalidKeyException | IOException e) {
      Log.w(TAG, e);
    }
  }

  private void setUnidentifiedAccessMode(Recipient recipient, String unidentifiedAccessVerifier, boolean unrestrictedUnidentifiedAccess) {
    RecipientDatabase recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
    byte[]            profileKey        = recipient.getProfileKey();

    if (unrestrictedUnidentifiedAccess && unidentifiedAccessVerifier != null) {
      Log.i(TAG, "Marking recipient UD status as unrestricted.");
      recipientDatabase.setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.UNRESTRICTED);
    } else if (profileKey == null || unidentifiedAccessVerifier == null) {
      Log.i(TAG, "Marking recipient UD status as disabled.");
      recipientDatabase.setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.DISABLED);
    } else {
      ProfileCipher profileCipher = new ProfileCipher(profileKey);
      boolean verifiedUnidentifiedAccess;

      try {
        verifiedUnidentifiedAccess = profileCipher.verifyUnidentifiedAccess(Base64.decode(unidentifiedAccessVerifier));
      } catch (IOException e) {
        Log.w(TAG, e);
        verifiedUnidentifiedAccess = false;
      }

      UnidentifiedAccessMode mode = verifiedUnidentifiedAccess ? UnidentifiedAccessMode.ENABLED : UnidentifiedAccessMode.DISABLED;
      Log.i(TAG, "Marking recipient UD status as " + mode.name() + " after verification.");
      recipientDatabase.setUnidentifiedAccessMode(recipient, mode);
    }
  }

  private void setProfileName(Recipient recipient, String profileName) {
    try {
      byte[] profileKey = recipient.getProfileKey();
      if (profileKey == null) return;

      String plaintextProfileName = null;

      if (profileName != null) {
        ProfileCipher profileCipher = new ProfileCipher(profileKey);
        plaintextProfileName = new String(profileCipher.decryptName(Base64.decode(profileName)));
      }

      if (!Util.equals(plaintextProfileName, recipient.getProfileName())) {
        DatabaseFactory.getRecipientDatabase(context).setProfileName(recipient, plaintextProfileName);
      }
    } catch (ProfileCipher.InvalidCiphertextException | IOException e) {
      Log.w(TAG, e);
    }
  }

  private void setProfileAvatar(Recipient recipient, String profileAvatar) {
    if (recipient.getProfileKey() == null) return;

    if (!Util.equals(profileAvatar, recipient.getProfileAvatar())) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new RetrieveProfileAvatarJob(context, recipient, profileAvatar));
    }
  }

  private Optional<UnidentifiedAccess> getUnidentifiedAccess(@NonNull Recipient recipient) {
    Optional<UnidentifiedAccessPair> unidentifiedAccess = UnidentifiedAccessUtil.getAccessFor(context, recipient);

    if (unidentifiedAccess.isPresent()) {
      return unidentifiedAccess.get().getTargetUnidentifiedAccess();
    }

    return Optional.absent();
  }
}
