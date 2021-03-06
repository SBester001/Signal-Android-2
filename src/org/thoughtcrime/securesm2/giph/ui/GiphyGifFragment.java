package org.thoughtcrime.securesm2.giph.ui;


import android.os.Bundle;
import android.support.v4.content.Loader;

import org.thoughtcrime.securesm2.giph.model.GiphyImage;
import org.thoughtcrime.securesm2.giph.net.GiphyGifLoader;

import java.util.List;

public class GiphyGifFragment extends GiphyFragment {

  @Override
  public Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyGifLoader(getActivity(), searchString);
  }

}
