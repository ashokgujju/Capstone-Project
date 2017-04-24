package com.ashok.simplereader.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
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
import net.dean.jraw.paginators.SubredditSearchPaginator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchForSubredditsActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks {

    public static final String KEY_QUERY = "query";
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

        adapter = new SubredditAdapter(this);
        mSubreddits.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSubreddits.setHasFixedSize(true);
        mSubreddits.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName(getString(R.string.search_srs));
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        MenuItemCompat.expandActionView(item);
        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                finish();
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mProgressbar.setVisibility(View.VISIBLE);
                mSubreddits.setVisibility(View.GONE);
                mEmptyMsg.setVisibility(View.GONE);
                Bundle bundle = new Bundle();
                bundle.putString(KEY_QUERY, query);
                getSupportLoaderManager()
                        .restartLoader(1, bundle, SearchForSubredditsActivity.this).forceLoad();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new SearchSubredditAsyncTaskLoader(this, args.getString(KEY_QUERY));
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
                mEmptyMsg.setText(R.string.no_results_found);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private static class SearchSubredditAsyncTaskLoader extends AsyncTaskLoader {
        private String query;
        private Context context;

        public SearchSubredditAsyncTaskLoader(Context context, String query) {
            super(context);
            this.context = context;
            this.query = query;
        }

        @Override
        public Object loadInBackground() {
            try {
                AuthenticationState state = AuthenticationManager.get().checkAuthState();
                if (state == AuthenticationState.NEED_REFRESH) {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                }

                RedditClient client = AuthenticationManager.get().getRedditClient();
                SubredditSearchPaginator paginator = new SubredditSearchPaginator(client, query);
                Listing<Subreddit> listings = paginator.next();

                Set<String> favSubredditIds = PrefUtils.getFavoriteSubreddits(context);
                List<MySubreddit> mySubreddits = new ArrayList<>();

                for (Subreddit subreddit : listings) {
                    MySubreddit mySubreddit = new MySubreddit();
                    mySubreddit.setSubreddit(subreddit);
                    if (favSubredditIds.contains(subreddit.getId())) {
                        mySubreddit.setFavorite(true);
                    } else {
                        mySubreddit.setFavorite(false);
                    }
                    mySubreddits.add(mySubreddit);
                }
                return mySubreddits;
            } catch (Exception e) {
            }
            return null;
        }
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}
