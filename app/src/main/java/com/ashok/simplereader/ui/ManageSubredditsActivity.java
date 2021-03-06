package com.ashok.simplereader.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ashok.simplereader.MyApplication;
import com.ashok.simplereader.R;
import com.ashok.simplereader.model.MySubreddit;
import com.ashok.simplereader.utils.PrefUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ManageSubredditsActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks {
    @BindView(R.id.results)
    RecyclerView mSubreddits;
    @BindView(R.id.progressbar)
    ProgressBar mProgressbar;
    @BindView(R.id.empty_msg)
    TextView mEmptyMsg;

    private SubredditAdapter adapter;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_for_subreddits);
        ButterKnife.bind(this);

        MyApplication application = (MyApplication) getApplication();
        mTracker = application.getDefaultTracker();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new SubredditAdapter(this);
        mSubreddits.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSubreddits.setHasFixedSize(true);
        mSubreddits.setAdapter(adapter);

        mProgressbar.setVisibility(View.VISIBLE);
        getSupportLoaderManager().initLoader(1, null, this).forceLoad();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new UserSubredditAsyncTaskLoader(this);
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        mProgressbar.setVisibility(View.GONE);
        mEmptyMsg.setVisibility(View.GONE);
        mSubreddits.setVisibility(View.VISIBLE);

        if (data != null) {
            List<MySubreddit> mysubreddits = (List<MySubreddit>) data;
            adapter.setData(mysubreddits);
        }

        if (adapter.getItemCount() == 0) {
            mEmptyMsg.setVisibility(View.VISIBLE);
            mSubreddits.setVisibility(View.GONE);
            if (!networkUp()) {
                mEmptyMsg.setText(R.string.connect_to_internet);
            } else {
                mEmptyMsg.setText(R.string.subscribe_to_srs);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private static class UserSubredditAsyncTaskLoader extends AsyncTaskLoader {
        private Context context;

        public UserSubredditAsyncTaskLoader(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public Object loadInBackground() {
            try {
                AuthenticationState state = AuthenticationManager.get().checkAuthState();
                if (state == AuthenticationState.NEED_REFRESH) {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                }
                RedditClient client = AuthenticationManager.get().getRedditClient();
                UserSubredditsPaginator paginator = new UserSubredditsPaginator(client, "subscriber");
                Listing<Subreddit> listings = paginator.next();

                Set<String> favSubredditIds = PrefUtils.getFavoriteSubreddits(context);

                List<MySubreddit> mySubreddits = new ArrayList<>();
                Set<String> latestIds = new HashSet<>();

                for (Subreddit subreddit : listings) {
                    MySubreddit mySubreddit = new MySubreddit();
                    mySubreddit.setSubreddit(subreddit);
                    if (favSubredditIds.contains(subreddit.getId())) {
                        mySubreddit.setFavorite(true);
                        latestIds.add(subreddit.getId());
                    } else {
                        mySubreddit.setFavorite(false);
                    }
                    mySubreddits.add(mySubreddit);
                }
                PrefUtils.updateFavorites(context, latestIds);
                return mySubreddits;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName(getString(R.string.manage_srs_scren));
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}