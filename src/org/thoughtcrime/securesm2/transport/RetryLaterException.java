package org.thoughtcrime.securesm2.transport;

public class RetryLaterException extends Exception {
  public RetryLaterException() {}

  public RetryLaterException(Exception e) {
    super(e);
  }
}
