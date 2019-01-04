package org.thoughtcrime.securesm2.notifications;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.thoughtcrime.securesm2.ConversationListActivity;
import org.thoughtcrime.securesm2.R;
import org.thoughtcrime.securesm2.database.RecipientDatabase;
import org.thoughtcrime.securesm2.preferences.widgets.NotificationPrivacyPreference;
import org.thoughtcrime.securesm2.util.TextSecurePreferences;

public class PendingMessageNotificationBuilder extends AbstractNotificationBuilder {

  public PendingMessageNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
    super(context, privacy);

    Intent intent = new Intent(context, ConversationListActivity.class);

    setSmallIcon(R.drawable.icon_notification);
    setColor(context.getResources().getColor(R.color.textsecure_primary));
    setCategory(NotificationCompat.CATEGORY_MESSAGE);

    setContentTitle(context.getString(R.string.MessageNotifier_pending_signal_messages));
    setContentText(context.getString(R.string.MessageNotifier_you_have_pending_signal_messages));
    setTicker(context.getString(R.string.MessageNotifier_you_have_pending_signal_messages));

    setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
    setAutoCancel(true);
    setAlarms(null, RecipientDatabase.VibrateState.DEFAULT);

    if (!NotificationChannels.supported()) {
      setPriority(TextSecurePreferences.getNotificationPriority(context));
    }
  }
}
