package org.thoughtcrime.securesm2.qr;

public interface ScanListener {
  public void onQrDataFound(String data);
}
