package org.thoughtcrime.securesm2.jobs.requirements;

import android.content.Context;

import org.thoughtcrime.securesm2.jobmanager.dependencies.ContextDependent;
import org.thoughtcrime.securesm2.jobmanager.requirements.SimpleRequirement;
import org.thoughtcrime.securesm2.sms.TelephonyServiceState;

public class ServiceRequirement extends SimpleRequirement implements ContextDependent {

  private static final String TAG = ServiceRequirement.class.getSimpleName();

  private transient Context context;

  public ServiceRequirement(Context context) {
    this.context  = context;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    TelephonyServiceState telephonyServiceState = new TelephonyServiceState();
    return telephonyServiceState.isConnected(context);
  }
}
