package org.thoughtcrime.securesm2.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesm2.BuildConfig;
import org.thoughtcrime.securesm2.R;
import org.thoughtcrime.securesm2.TextSecureExpiredException;
import org.thoughtcrime.securesm2.attachments.Attachment;
import org.thoughtcrime.securesm2.database.AttachmentDatabase;
import org.thoughtcrime.securesm2.database.DatabaseFactory;
import org.thoughtcrime.securesm2.jobmanager.JobParameters;
import org.thoughtcrime.securesm2.logging.Log;
import org.thoughtcrime.securesm2.mms.MediaConstraints;
import org.thoughtcrime.securesm2.mms.MediaStream;
import org.thoughtcrime.securesm2.mms.MmsException;
import org.thoughtcrime.securesm2.transport.UndeliverableMessageException;
import org.thoughtcrime.securesm2.util.MediaUtil;
import org.thoughtcrime.securesm2.util.Util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import androidx.work.WorkerParameters;

public abstract class SendJob extends ContextJob {

  @SuppressWarnings("unused")
  private final static String TAG = SendJob.class.getSimpleName();

  protected SendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public SendJob(Context context, JobParameters parameters) {
    super(context, parameters);
  }

  @Override
  protected String getDescription() {
    return context.getString(R.string.SendJob_sending_a_message);
  }

  @Override
  public final void onRun() throws Exception {
    if (Util.getDaysTillBuildExpiry() <= 0) {
      throw new TextSecureExpiredException(String.format("TextSecure expired (build %d, now %d)",
                                                         BuildConfig.BUILD_TIMESTAMP,
                                                         System.currentTimeMillis()));
    }

    Log.i(TAG, "Starting message send attempt");
    onSend();
    Log.i(TAG, "Message send completed");
  }

  protected abstract void onSend() throws Exception;

  protected void markAttachmentsUploaded(long messageId, @NonNull List<Attachment> attachments) {
    AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);

    for (Attachment attachment : attachments) {
      database.markAttachmentUploaded(messageId, attachment);
    }
  }

  protected List<Attachment> scaleAndStripExifFromAttachments(@NonNull MediaConstraints constraints,
                                                              @NonNull List<Attachment> attachments)
      throws UndeliverableMessageException
  {
    AttachmentDatabase attachmentDatabase = DatabaseFactory.getAttachmentDatabase(context);
    List<Attachment>   results            = new LinkedList<>();

    for (Attachment attachment : attachments) {
      try {
        if (constraints.isSatisfied(context, attachment)) {
          if (MediaUtil.isJpeg(attachment)) {
            MediaStream stripped = constraints.getResizedMedia(context, attachment);
            results.add(attachmentDatabase.updateAttachmentData(attachment, stripped));
          } else {
            results.add(attachment);
          }
        } else if (constraints.canResize(attachment)) {
          MediaStream resized = constraints.getResizedMedia(context, attachment);
          results.add(attachmentDatabase.updateAttachmentData(attachment, resized));
        } else {
          throw new UndeliverableMessageException("Size constraints could not be met!");
        }
      } catch (IOException | MmsException e) {
        throw new UndeliverableMessageException(e);
      }
    }

    return results;
  }

  protected void log(@NonNull String tag, @NonNull String message) {
    Log.i(tag, "[" + getId().toString() + "] " + message + logSuffix());
  }

  protected void warn(@NonNull String tag, @NonNull String message) {
    warn(tag, message, null);
  }

  protected void warn(@NonNull String tag, @Nullable Throwable t) {
    warn(tag, "", t);
  }

  protected void warn(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
    Log.w(tag, "[" + getId().toString() + "] " + message + logSuffix(), t);
  }
}