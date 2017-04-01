package com.ashok.simplereader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.UserSubredditsPaginator;

public class ManageSubredditsActivity extends AppCompatActivity {
    private ListView mSubreddits;
    private SubredditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_for_subreddits);

        mSubreddits = (ListView) findViewById(R.id.results);
        adapter = new SubredditAdapter(this);
        mSubreddits.setAdapter(adapter);


        final UserSubredditsPaginator paginator = new UserSubredditsPaginator(AuthenticationManager.get().getRedditClient(), "subscriber");

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
    }
}
