package org.thoughtcrime.securesm2.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesm2.database.DatabaseFactory;
import org.thoughtcrime.securesm2.database.GroupDatabase;
import org.thoughtcrime.securesm2.database.GroupDatabase.GroupRecord;
import org.thoughtcrime.securesm2.dependencies.InjectableType;
import org.thoughtcrime.securesm2.jobmanager.JobParameters;
import org.thoughtcrime.securesm2.jobmanager.SafeData;
import org.thoughtcrime.securesm2.logging.Log;
import org.thoughtcrime.securesm2.mms.AttachmentStreamUriLoader.AttachmentModel;
import org.thoughtcrime.securesm2.util.BitmapDecodingException;
import org.thoughtcrime.securesm2.util.BitmapUtil;
import org.thoughtcrime.securesm2.util.GroupUtil;
import org.thoughtcrime.securesm2.util.Hex;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class AvatarDownloadJob extends ContextJob implements InjectableType {

  private static final int MAX_AVATAR_SIZE = 20 * 1024 * 1024;
  private static final long serialVersionUID = 1L;

  private static final String TAG = AvatarDownloadJob.class.getSimpleName();

  private static final String KEY_GROUP_ID = "group_id";

  @Inject transient SignalServiceMessageReceiver receiver;

  private byte[] groupId;

  public AvatarDownloadJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public AvatarDownloadJob(Context context, @NonNull byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withNetworkRequirement()
                                .create());

    this.groupId = groupId;
  }

  @Override
  protected void initialize(@NonNull SafeData data) {
    try {
      groupId = GroupUtil.getDecodedId(data.getString(KEY_GROUP_ID));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  protected @NonNull Data serialize(@NonNull Data.Builder dataBuilder) {
    return dataBuilder.putString(KEY_GROUP_ID, GroupUtil.getEncodedId(groupId, false)).build();
  }

  @Override
  public void onRun() throws IOException {
    String                encodeId   = GroupUtil.getEncodedId(groupId, false);
    GroupDatabase         database   = DatabaseFactory.getGroupDatabase(context);
    Optional<GroupRecord> record     = database.getGroup(encodeId);
    File                  attachment = null;

    try {
      if (record.isPresent()) {
        long             avatarId    = record.get().getAvatarId();
        String           contentType = record.get().getAvatarContentType();
        byte[]           key         = record.get().getAvatarKey();
        String           relay       = record.get().getRelay();
        Optional<byte[]> digest      = Optional.fromNullable(record.get().getAvatarDigest());
        Optional<String> fileName    = Optional.absent();

        if (avatarId == -1 || key == null) {
          return;
        }

        if (digest.isPresent()) {
          Log.i(TAG, "Downloading group avatar with digest: " + Hex.toString(digest.get()));
        }

        attachment = File.createTempFile("avatar", "tmp", context.getCacheDir());
        attachment.deleteOnExit();

        SignalServiceAttachmentPointer pointer     = new SignalServiceAttachmentPointer(avatarId, contentType, key, Optional.of(0), Optional.absent(), 0, 0, digest, fileName, false, Optional.absent());
        InputStream                    inputStream = receiver.retrieveAttachment(pointer, attachment, MAX_AVATAR_SIZE);
        Bitmap                         avatar      = BitmapUtil.createScaledBitmap(context, new AttachmentModel(attachment, key, 0, digest), 500, 500);

        database.updateAvatar(encodeId, avatar);
        inputStream.close();
      }
    } catch (BitmapDecodingException | NonSuccessfulResponseCodeException | InvalidMessageException e) {
      Log.w(TAG, e);
    } finally {
      if (attachment != null)
        attachment.delete();
    }
  }

  @Override
  public void onCanceled() {}

  @Override
  public boolean onShouldRetry(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

}
