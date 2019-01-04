package org.thoughtcrime.securesm2.jobs.requirements;

import android.content.Context;

import org.thoughtcrime.securesm2.jobmanager.dependencies.ContextDependent;
import org.thoughtcrime.securesm2.jobmanager.requirements.SimpleRequirement;
import org.thoughtcrime.securesm2.service.KeyCachingService;

public class MasterSecretRequirement extends SimpleRequirement implements ContextDependent {

  private transient Context context;

  public MasterSecretRequirement(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    return KeyCachingService.getMasterSecret(context) != null;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }
}
