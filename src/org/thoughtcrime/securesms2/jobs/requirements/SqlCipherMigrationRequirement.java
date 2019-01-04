package org.thoughtcrime.securesm2.jobs.requirements;


import android.content.Context;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesm2.jobmanager.dependencies.ContextDependent;
import org.thoughtcrime.securesm2.jobmanager.requirements.SimpleRequirement;
import org.thoughtcrime.securesm2.util.TextSecurePreferences;

public class SqlCipherMigrationRequirement extends SimpleRequirement implements ContextDependent {

  @SuppressWarnings("unused")
  private static final String TAG = SqlCipherMigrationRequirement.class.getSimpleName();

  private transient Context context;

  public SqlCipherMigrationRequirement(@NonNull Context context) {
    this.context = context;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    return !TextSecurePreferences.getNeedsSqlCipherMigration(context);
  }
}
