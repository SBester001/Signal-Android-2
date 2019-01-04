package org.thoughtcrime.securesm2.push;

import android.content.Context;

import org.thoughtcrime.securesm2.R;
import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.InputStream;

public class IasTrustStore implements TrustStore {

  private final Context context;

  public IasTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.ias);
  }

  @Override
  public String getKeyStorePassword() {
    return "whisper";
  }
}
