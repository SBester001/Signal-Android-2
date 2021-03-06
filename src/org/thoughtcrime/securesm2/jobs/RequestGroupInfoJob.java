package org.thoughtcrime.securesm2.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesm2.crypto.UnidentifiedAccessUtil;
import org.thoughtcrime.securesm2.database.Address;
import org.thoughtcrime.securesm2.dependencies.InjectableType;
import org.thoughtcrime.securesm2.jobmanager.JobParameters;
import org.thoughtcrime.securesm2.jobmanager.SafeData;
import org.thoughtcrime.securesm2.util.GroupUtil;
import org.thoughtcrime.securesm2.recipients.Recipient;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup.Type;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class RequestGroupInfoJob extends ContextJob implements InjectableType {

  @SuppressWarnings("unused")
  private static final String TAG = RequestGroupInfoJob.class.getSimpleName();

  private static final long serialVersionUID = 0L;

  private static final String KEY_SOURCE   = "source";
  private static final String KEY_GROUP_ID = "group_id";

  @Inject transient SignalServiceMessageSender messageSender;

  private String source;
  private byte[] groupId;

  public RequestGroupInfoJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public RequestGroupInfoJob(@NonNull Context context, @NonNull String source, @NonNull byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withNetworkRequirement()
                                .withRetryCount(50)
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
    SignalServiceGroup       group   = SignalServiceGroup.newBuilder(Type.REQUEST_INFO)
                                                         .withId(groupId)
                                                         .build();

    SignalServiceDataMessage message = SignalServiceDataMessage.newBuilder()
                                                               .asGroupMessage(group)
                                                               .withTimestamp(System.currentTimeMillis())
                                                               .build();

    messageSender.sendMessage(new SignalServiceAddress(source),
                              UnidentifiedAccessUtil.getAccessFor(context, Recipient.from(context, Address.fromExternal(context, source), false)),
                              message);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {

  }
}
