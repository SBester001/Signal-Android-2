package org.thoughtcrime.securesm2.jobs;


import android.content.Context;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesm2.crypto.UnidentifiedAccessUtil;
import org.thoughtcrime.securesm2.database.Address;
import org.thoughtcrime.securesm2.database.DatabaseFactory;
import org.thoughtcrime.securesm2.database.GroupDatabase;
import org.thoughtcrime.securesm2.database.GroupDatabase.GroupRecord;
import org.thoughtcrime.securesm2.dependencies.InjectableType;
import org.thoughtcrime.securesm2.jobmanager.JobParameters;
import org.thoughtcrime.securesm2.jobmanager.SafeData;
import org.thoughtcrime.securesm2.logging.Log;
import org.thoughtcrime.securesm2.recipients.Recipient;
import org.thoughtcrime.securesm2.util.GroupUtil;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup.Type;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class PushGroupUpdateJob extends ContextJob implements InjectableType {

  private static final String TAG = PushGroupUpdateJob.class.getSimpleName();

  private static final long serialVersionUID = 0L;

  private static final String KEY_SOURCE   = "source";
  private static final String KEY_GROUP_ID = "group_id";

  @Inject transient SignalServiceMessageSender messageSender;

  private String source;
  private byte[] groupId;

  public PushGroupUpdateJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public PushGroupUpdateJob(Context context, String source, byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withNetworkRequirement()
                                .withRetryDuration(TimeUnit.DAYS.toMillis(1))
                                .create());

    this.source  = source;
    this.groupId = groupId;
  }

  @Override
  protected void initialize(@NonNull SafeData data) {
    source = data.getString(KEY_SOURCE);
    try {
      groupId = GroupUtil.getDecodedId(data.getString(KEY_GROUP_ID));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  protected @NonNull Data serialize(@NonNull Data.Builder dataBuilder) {
    return dataBuilder.putString(KEY_SOURCE, source)
                      .putString(KEY_GROUP_ID, GroupUtil.getEncodedId(groupId, false))
                      .build();
  }

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    GroupDatabase           groupDatabase = DatabaseFactory.getGroupDatabase(context);
    Optional<GroupRecord>   record        = groupDatabase.getGroup(GroupUtil.getEncodedId(groupId, false));
    SignalServiceAttachment avatar        = null;

    if (record == null) {
      Log.w(TAG, "No information for group record info request: " + new String(groupId));
      return;
    }

    if (record.get().getAvatar() != null) {
      avatar = SignalServiceAttachmentStream.newStreamBuilder()
                                            .withContentType("image/jpeg")
                                            .withStream(new ByteArrayInputStream(record.get().getAvatar()))
                                            .withLength(record.get().getAvatar().length)
                                            .build();
    }

    List<String> members = new LinkedList<>();

    for (Address member : record.get().getMembers()) {
      members.add(member.serialize());
    }

    SignalServiceGroup groupContext = SignalServiceGroup.newBuilder(Type.UPDATE)
                                                        .withAvatar(avatar)
                                                        .withId(groupId)
                                                        .withMembers(members)
                                                        .withName(record.get().getTitle())
                                                        .build();

    Address   groupAddress   = Address.fromSerialized(GroupUtil.getEncodedId(groupId, false));
    Recipient groupRecipient = Recipient.from(context, groupAddress, false);

    SignalServiceDataMessage message = SignalServiceDataMessage.newBuilder()
                                                               .asGroupMessage(groupContext)
                                                               .withTimestamp(System.currentTimeMillis())
                                                               .withExpiration(groupRecipient.getExpireMessages())
                                                               .build();

    messageSender.sendMessage(new SignalServiceAddress(source),
                              UnidentifiedAccessUtil.getAccessFor(context, Recipient.from(context, Address.fromSerialized(source), false)),
                              message);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    Log.w(TAG, e);
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {

  }
}
