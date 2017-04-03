package com.ashok.simplereader.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.ashok.simplereader.R;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditSearchPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchForSubredditsActivity extends AppCompatActivity {

    @BindView(R.id.results)
    ListView mSubreddits;
    @BindView(R.id.progressbar)
    ProgressBar mProgressbar;
    private SubredditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_for_subreddits);
        ButterKnife.bind(this);

        adapter = new SubredditAdapter(this);
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
                RedditClient client = AuthenticationManager.get().getRedditClient();
                final SubredditSearchPaginator paginator = new SubredditSearchPaginator(client, query);
                new AsyncTask<Void, Void, Listing<Subreddit>>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        mProgressbar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    protected Listing<Subreddit> doInBackground(Void... voids) {
                        return paginator.next();
                    }

                    @Override
                    protected void onPostExecute(Listing<Subreddit> subreddits) {
                        mProgressbar.setVisibility(View.GONE);
                        adapter.setData(subreddits);
                    }
                }.execute();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }
}
