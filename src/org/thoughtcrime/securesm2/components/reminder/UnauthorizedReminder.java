package org.thoughtcrime.securesm2.components.reminder;


import android.content.Context;
import android.content.Intent;

import org.thoughtcrime.securesm2.R;
import org.thoughtcrime.securesm2.RegistrationActivity;
import org.thoughtcrime.securesm2.util.TextSecurePreferences;

public class UnauthorizedReminder extends Reminder {

  public UnauthorizedReminder(final Context context) {
    super(context.getString(R.string.UnauthorizedReminder_device_no_longer_registered),
          context.getString(R.string.UnauthorizedReminder_this_is_likely_because_you_registered_your_phone_number_with_Signal_on_a_different_device));

    setOkListener(v -> {
      Intent intent = new Intent(context, RegistrationActivity.class);
      intent.putExtra(RegistrationActivity.RE_REGISTRATION_EXTRA, true);
      context.startActivity(intent);
    });
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  public static boolean isEligible(Context context) {
    //todo todo: app doesn't notice that you use your phone number for another device/app
    //return false;
    return TextSecurePreferences.isUnauthorizedRecieved(context);

  }
}
