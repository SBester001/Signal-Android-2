package org.thoughtcrime.securesm2.jobs.requirements;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.thoughtcrime.securesm2.jobmanager.requirements.RequirementListener;
import org.thoughtcrime.securesm2.jobmanager.requirements.RequirementProvider;
import org.thoughtcrime.securesm2.service.KeyCachingService;

public class MasterSecretRequirementProvider implements RequirementProvider {

  private final BroadcastReceiver newKeyReceiver;

  private RequirementListener listener;

  public MasterSecretRequirementProvider(Context context) {
    this.newKeyReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (listener != null) {
          listener.onRequirementStatusChanged();
        }
      }
    };

    IntentFilter filter = new IntentFilter(KeyCachingService.NEW_KEY_EVENT);
    context.registerReceiver(newKeyReceiver, filter, KeyCachingService.KEY_PERMISSION, null);
  }

  @Override
  public void setListener(RequirementListener listener) {
    this.listener = listener;
  }
}
