package com.ashok.simplereader.ui;

import android.content.Context;
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
import android.widget.ProgressBar;

import com.ashok.simplereader.MySubreddit;
import com.ashok.simplereader.utils.PrefUtils;
import com.ashok.simplereader.R;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
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
    private SubredditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_for_subreddits);
        ButterKnife.bind(this);

        adapter = new SubredditAdapter(this);
        mSubreddits.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSubreddits.setHasFixedSize(true);
        mSubreddits.setAdapter(adapter);
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
                Bundle bundle = new Bundle();
                bundle.putString(KEY_QUERY, query);
                getSupportLoaderManager().restartLoader(1, bundle, SearchForSubredditsActivity.this).forceLoad();

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
        if (data != null) {
            List<MySubreddit> mysubreddits = (List<MySubreddit>) data;
            adapter.setData(mysubreddits);
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
        }
    }
}
