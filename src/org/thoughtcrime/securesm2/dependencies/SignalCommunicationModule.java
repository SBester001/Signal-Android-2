package org.thoughtcrime.securesm2.dependencies;

import android.content.Context;

import org.thoughtcrime.securesm2.gcm.GcmBroadcastReceiver;
import org.thoughtcrime.securesm2.jobs.MultiDeviceConfigurationUpdateJob;
import org.thoughtcrime.securesm2.jobs.RefreshUnidentifiedDeliveryAbilityJob;
import org.thoughtcrime.securesm2.jobs.RotateProfileKeyJob;
import org.thoughtcrime.securesm2.jobs.TypingSendJob;
import org.thoughtcrime.securesm2.logging.Log;

import org.greenrobot.eventbus.EventBus;
import org.thoughtcrime.securesm2.BuildConfig;
import org.thoughtcrime.securesm2.CreateProfileActivity;
import org.thoughtcrime.securesm2.DeviceListFragment;
import org.thoughtcrime.securesm2.crypto.storage.SignalProtocolStoreImpl;
import org.thoughtcrime.securesm2.events.ReminderUpdateEvent;
import org.thoughtcrime.securesm2.jobs.AttachmentDownloadJob;
import org.thoughtcrime.securesm2.jobs.AvatarDownloadJob;
import org.thoughtcrime.securesm2.jobs.CleanPreKeysJob;
import org.thoughtcrime.securesm2.jobs.CreateSignedPreKeyJob;
import org.thoughtcrime.securesm2.jobs.GcmRefreshJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceBlockedUpdateJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceContactUpdateJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceGroupUpdateJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceProfileKeyUpdateJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceReadReceiptUpdateJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceReadUpdateJob;
import org.thoughtcrime.securesm2.jobs.MultiDeviceVerifiedUpdateJob;
import org.thoughtcrime.securesm2.jobs.PushGroupSendJob;
import org.thoughtcrime.securesm2.jobs.PushGroupUpdateJob;
import org.thoughtcrime.securesm2.jobs.PushMediaSendJob;
import org.thoughtcrime.securesm2.jobs.PushNotificationReceiveJob;
import org.thoughtcrime.securesm2.jobs.PushTextSendJob;
import org.thoughtcrime.securesm2.jobs.RefreshAttributesJob;
import org.thoughtcrime.securesm2.jobs.RefreshPreKeysJob;
import org.thoughtcrime.securesm2.jobs.RequestGroupInfoJob;
import org.thoughtcrime.securesm2.jobs.RetrieveProfileAvatarJob;
import org.thoughtcrime.securesm2.jobs.RetrieveProfileJob;
import org.thoughtcrime.securesm2.jobs.RotateCertificateJob;
import org.thoughtcrime.securesm2.jobs.RotateSignedPreKeyJob;
import org.thoughtcrime.securesm2.jobs.SendDeliveryReceiptJob;
import org.thoughtcrime.securesm2.jobs.SendReadReceiptJob;
import org.thoughtcrime.securesm2.preferences.AppProtectionPreferenceFragment;
import org.thoughtcrime.securesm2.push.SecurityEventListener;
import org.thoughtcrime.securesm2.push.SignalServiceNetworkAccess;
import org.thoughtcrime.securesm2.service.IncomingMessageObserver;
import org.thoughtcrime.securesm2.service.WebRtcCallService;
import org.thoughtcrime.securesm2.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.util.RealtimeSleepTimer;
import org.whispersystems.signalservice.api.util.SleepTimer;
import org.whispersystems.signalservice.api.util.UptimeSleepTimer;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {CleanPreKeysJob.class,
                                     CreateSignedPreKeyJob.class,
                                     PushGroupSendJob.class,
                                     PushTextSendJob.class,
                                     PushMediaSendJob.class,
                                     AttachmentDownloadJob.class,
                                     RefreshPreKeysJob.class,
                                     IncomingMessageObserver.class,
                                     PushNotificationReceiveJob.class,
                                     MultiDeviceContactUpdateJob.class,
                                     MultiDeviceGroupUpdateJob.class,
                                     MultiDeviceReadUpdateJob.class,
                                     MultiDeviceBlockedUpdateJob.class,
                                     DeviceListFragment.class,
                                     RefreshAttributesJob.class,
                                     GcmRefreshJob.class,
                                     RequestGroupInfoJob.class,
                                     PushGroupUpdateJob.class,
                                     AvatarDownloadJob.class,
                                     RotateSignedPreKeyJob.class,
                                     WebRtcCallService.class,
                                     RetrieveProfileJob.class,
                                     MultiDeviceVerifiedUpdateJob.class,
                                     CreateProfileActivity.class,
                                     RetrieveProfileAvatarJob.class,
                                     MultiDeviceProfileKeyUpdateJob.class,
                                     SendReadReceiptJob.class,
                                     MultiDeviceReadReceiptUpdateJob.class,
                                     AppProtectionPreferenceFragment.class,
                                     GcmBroadcastReceiver.class,
                                     RotateCertificateJob.class,
                                     SendDeliveryReceiptJob.class,
                                     RotateProfileKeyJob.class,
                                     MultiDeviceConfigurationUpdateJob.class,
                                     RefreshUnidentifiedDeliveryAbilityJob.class,
                                     TypingSendJob.class})
public class SignalCommunicationModule {

  private static final String TAG = SignalCommunicationModule.class.getSimpleName();

  private final Context                      context;
  private final SignalServiceNetworkAccess   networkAccess;

  private SignalServiceAccountManager  accountManager;
  private SignalServiceMessageSender   messageSender;
  private SignalServiceMessageReceiver messageReceiver;

  public SignalCommunicationModule(Context context, SignalServiceNetworkAccess networkAccess) {
    this.context       = context;
    this.networkAccess = networkAccess;
  }

  @Provides
  synchronized SignalServiceAccountManager provideSignalAccountManager() {
    if (this.accountManager == null) {
      this.accountManager = new SignalServiceAccountManager(networkAccess.getConfiguration(context),
                                                            new DynamicCredentialsProvider(context),
                                                            BuildConfig.USER_AGENT);
    }

    return this.accountManager;
  }

  @Provides
  synchronized SignalServiceMessageSender provideSignalMessageSender() {
    if (this.messageSender == null) {
      this.messageSender = new SignalServiceMessageSender(networkAccess.getConfiguration(context),
                                                          new DynamicCredentialsProvider(context),
                                                          new SignalProtocolStoreImpl(context),
                                                          BuildConfig.USER_AGENT,
                                                          TextSecurePreferences.isMultiDevice(context),
                                                          Optional.fromNullable(IncomingMessageObserver.getPipe()),
                                                          Optional.fromNullable(IncomingMessageObserver.getUnidentifiedPipe()),
                                                          Optional.of(new SecurityEventListener(context)));
    } else {
      this.messageSender.setMessagePipe(IncomingMessageObserver.getPipe(), IncomingMessageObserver.getUnidentifiedPipe());
      this.messageSender.setIsMultiDevice(TextSecurePreferences.isMultiDevice(context));
    }

    return this.messageSender;
  }

  @Provides
  synchronized SignalServiceMessageReceiver provideSignalMessageReceiver() {
    if (this.messageReceiver == null) {
      SleepTimer sleepTimer =  TextSecurePreferences.isGcmDisabled(context) ? new RealtimeSleepTimer(context) : new UptimeSleepTimer();

      this.messageReceiver = new SignalServiceMessageReceiver(networkAccess.getConfiguration(context),
                                                              new DynamicCredentialsProvider(context),
                                                              BuildConfig.USER_AGENT,
                                                              new PipeConnectivityListener(),
                                                              sleepTimer);
    }

    return this.messageReceiver;
  }

  @Provides
  synchronized SignalServiceNetworkAccess provideSignalServiceNetworkAccess() {
    return networkAccess;
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public String getUser() {
      return TextSecurePreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return TextSecurePreferences.getPushServerPassword(context);
    }

    @Override
    public String getSignalingKey() {
      return TextSecurePreferences.getSignalingKey(context);
    }
  }

  private class PipeConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
      Log.i(TAG, "onConnected()");
    }

    @Override
    public void onConnecting() {
      Log.i(TAG, "onConnecting()");
    }

    @Override
    public void onDisconnected() {
      Log.w(TAG, "onDisconnected()");
    }

    @Override
    public void onAuthenticationFailure() {
      Log.w(TAG, "onAuthenticationFailure()");
      TextSecurePreferences.setUnauthorizedReceived(context, true);
      EventBus.getDefault().post(new ReminderUpdateEvent());
    }

  }

}
