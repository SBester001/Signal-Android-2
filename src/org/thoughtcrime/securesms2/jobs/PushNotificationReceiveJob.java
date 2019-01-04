package org.thoughtcrime.securesm2.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesm2.R;
import org.thoughtcrime.securesm2.dependencies.InjectableType;
import org.thoughtcrime.securesm2.jobmanager.JobParameters;
import org.thoughtcrime.securesm2.jobmanager.SafeData;
import org.thoughtcrime.securesm2.logging.Log;
import org.thoughtcrime.securesm2.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class PushNotificationReceiveJob extends PushReceivedJob implements InjectableType {

  private static final String TAG = PushNotificationReceiveJob.class.getSimpleName();

  @Inject transient SignalServiceMessageReceiver receiver;

  public PushNotificationReceiveJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public PushNotificationReceiveJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withNetworkRequirement()
                                .withGroupId("__notification_received")
                                .create());
  }

  @Override
  protected void initialize(@NonNull SafeData data) {
  }

  @Override
  protected @NonNull Data serialize(@NonNull Data.Builder dataBuilder) {
    return dataBuilder.build();
  }

  @Override
  protected String getDescription() {
    return context.getString(R.string.PushNotificationReceiveJob_retrieving_a_message);
  }

  @Override
  public void onRun() throws IOException {
    pullAndProcessMessages(receiver, TAG, System.currentTimeMillis());
  }

  public void pullAndProcessMessages(SignalServiceMessageReceiver receiver, String tag, long startTime) throws IOException {
    synchronized (PushReceivedJob.RECEIVE_LOCK) {
      receiver.retrieveMessages(envelope -> {
        Log.i(tag, "Retrieved an envelope." + timeSuffix(startTime));
        processEnvelope(envelope);
        Log.i(tag, "Successfully processed an envelope." + timeSuffix(startTime));
      });
      TextSecurePreferences.setNeedsMessagePull(context, false);
    }
  }
  @Override
  public boolean onShouldRetry(Exception e) {
    Log.w(TAG, e);
    return e instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "***** Failed to download pending message!");
//    MessageNotifier.notifyMessagesPending(getContext());
  }

  private static String timeSuffix(long startTime) {
    return " (" + (System.currentTimeMillis() - startTime) + " ms elapsed)";
  }
}
