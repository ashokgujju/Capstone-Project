package com.ashok.simplereader.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import timber.log.Timber;

/**
 * Created by ashok on 3/4/17.
 */

public class PostIntentService extends IntentService {
    public PostIntentService() {
        super(PostIntentService.class.getSimpleName());

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Timber.d("Intent handled");
        PostSyncJob.getPosts(getApplicationContext());
    }
}
