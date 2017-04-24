package com.ashok.simplereader.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ashok.simplereader.data.PostColumns;
import com.ashok.simplereader.data.PostProvider;
import com.ashok.simplereader.ui.LoginActivity;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;

/**
 * Created by ashok on 3/4/17.
 */

public class PostSyncJob {
    private static final int PERIODIC_ID = 1;
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    public static final String ACTION_DATA_UPDATED = "com.ashok.simplereader.ACTION_DATA_UPDATED";

    public static void getPosts(Context context) {
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        if (state == AuthenticationState.NEED_REFRESH) {
            try {
                AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        RedditClient redditClient = AuthenticationManager.get().getRedditClient();
        if (redditClient.isAuthenticated()) {
            SubredditPaginator paginator = new SubredditPaginator(redditClient);
            Listing<Submission> posts = paginator.next();

            ArrayList<ContentValues> postCVs = new ArrayList<>();
            for (Submission post : posts) {
                ContentValues postCV = new ContentValues();
                postCV.put(PostColumns.DATA, post.getDataNode().toString());
                postCVs.add(postCV);
            }

            if (postCVs.size() > 0) {
                context.getContentResolver().delete(PostProvider.Posts.CONTENT_URI, null, null);
                context.getContentResolver().bulkInsert(PostProvider.Posts.CONTENT_URI, postCVs.toArray(
                        new ContentValues[postCVs.size()]
                ));
            }

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);
        }
    }

    public static synchronized void initialize(final Context context) {
        schedulePeriodic(context);
    }

    private static void schedulePeriodic(Context context) {
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, PostJobService.class));

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }
}
