package com.ashok.simplereader.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Created by ashok on 3/4/17.
 */

public class PostIntentService extends IntentService {
    public PostIntentService() {
        super(PostIntentService.class.getSimpleName());

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        PostSyncJob.getPosts(getApplicationContext());
    }
}
