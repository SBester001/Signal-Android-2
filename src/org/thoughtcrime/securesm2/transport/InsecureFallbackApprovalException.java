package org.thoughtcrime.securesm2.transport;

public class InsecureFallbackApprovalException extends Exception {
  public InsecureFallbackApprovalException(String detailMessage) {
    super(detailMessage);
  }

  public InsecureFallbackApprovalException(Throwable e) {
    super(e);
  }
}
