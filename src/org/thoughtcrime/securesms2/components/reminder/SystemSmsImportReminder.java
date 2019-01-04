package org.thoughtcrime.securesm2.components.reminder;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

import org.thoughtcrime.securesm2.ConversationListActivity;
import org.thoughtcrime.securesm2.DatabaseMigrationActivity;
import org.thoughtcrime.securesm2.R;
import org.thoughtcrime.securesm2.service.ApplicationMigrationService;

public class SystemSmsImportReminder extends Reminder {

  public SystemSmsImportReminder(final Context context) {
    super(context.getString(R.string.reminder_header_sms_import_title),
          context.getString(R.string.reminder_header_sms_import_text));

    final OnClickListener okListener = v -> {
      Intent intent = new Intent(context, ApplicationMigrationService.class);
      intent.setAction(ApplicationMigrationService.MIGRATE_DATABASE);
      context.startService(intent);

      Intent nextIntent = new Intent(context, ConversationListActivity.class);
      Intent activityIntent = new Intent(context, DatabaseMigrationActivity.class);
      activityIntent.putExtra("next_intent", nextIntent);
      context.startActivity(activityIntent);
    };
    final OnClickListener cancelListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        ApplicationMigrationService.setDatabaseImported(context);
      }
    };
    setOkListener(okListener);
    setDismissListener(cancelListener);
  }

  public static boolean isEligible(Context context) {
    return !ApplicationMigrationService.isDatabaseImported(context);
  }
}
