package com.ashok.simplereader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ManageSubredditsActivity extends AppCompatActivity {
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new SubredditAdapter(this);
        mSubreddits.setAdapter(adapter);

        final UserSubredditsPaginator paginator = new UserSubredditsPaginator(
                AuthenticationManager.get().getRedditClient(), "subscriber");

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
}
