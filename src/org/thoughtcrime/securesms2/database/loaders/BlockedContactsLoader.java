package org.thoughtcrime.securesm2.database.loaders;

import android.content.Context;
import android.database.Cursor;

import org.thoughtcrime.securesm2.database.DatabaseFactory;
import org.thoughtcrime.securesm2.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext())
                          .getBlocked();
  }

}
