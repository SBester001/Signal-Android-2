package org.thoughtcrime.securesm2.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesm2.ApplicationContext;
import org.thoughtcrime.securesm2.jobs.PushNotificationReceiveJob;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    ApplicationContext.getInstance(context).getJobManager().add(new PushNotificationReceiveJob(context));
  }
}
