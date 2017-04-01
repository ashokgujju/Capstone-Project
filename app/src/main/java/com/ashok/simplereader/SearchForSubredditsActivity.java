package com.ashok.simplereader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditSearchPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

public class SearchForSubredditsActivity extends AppCompatActivity {

    private ListView mSubreddits;
    private SubredditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_for_subreddits);

        mSubreddits = (ListView) findViewById(R.id.results);
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
        UserSubredditsPaginator k;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                RedditClient client = AuthenticationManager.get().getRedditClient();
                final SubredditSearchPaginator paginator = new SubredditSearchPaginator(client, query);
                new AsyncTask<Void, Void, Listing<Subreddit>>() {
                    @Override
                    protected Listing<Subreddit> doInBackground(Void... voids) {
                        return paginator.next();
                    }

                    @Override
                    protected void onPostExecute(Listing<Subreddit> subreddits) {
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
