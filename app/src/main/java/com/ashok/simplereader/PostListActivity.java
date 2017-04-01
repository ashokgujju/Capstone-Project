package com.ashok.simplereader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PostListActivity extends AppCompatActivity implements PostsAdapter.OnPostClickListener {

    private boolean mTwoPane;
    private String TAG = PostListActivity.class.getSimpleName();
    private List<Submission> posts = new ArrayList<>();
    private PostsAdapter adapter;
    SubredditPaginator paginator;

    @BindView(R.id.post_list)
    RecyclerView mPostsRV;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.navigation)
    NavigationView mNavigation;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        ButterKnife.bind(this);

        if (findViewById(R.id.post_detail_container) != null) {
            mTwoPane = true;
        }

        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PostListActivity.this, LoginActivity.class));
            }
        });

        adapter = new PostsAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mPostsRV.setLayoutManager(layoutManager);
        mPostsRV.setAdapter(adapter);
        adapter.setOnPostClickListener(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mPostsRV.getContext(),
                layoutManager.getOrientation());
        mPostsRV.addItemDecoration(dividerItemDecoration);

        RedditClient redditClient = AuthenticationManager.get().getRedditClient();
        paginator = new SubredditPaginator(redditClient);

        mNavigation.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_manage_srs:
                        startActivity(new Intent(PostListActivity.this, ManageSubredditsActivity.class));
                        break;
                }
                mDrawer.closeDrawers();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Log.d(TAG, "AuthenticationState for onResume(): " + state);
        switch (state) {
            case READY:
                loadPosts();
                break;
            case NONE:
                Toast.makeText(PostListActivity.this, "Log in first", Toast.LENGTH_SHORT).show();
                break;
            case NEED_REFRESH:
                refreshAccessTokenAsync();
                break;
        }
    }

    private void loadPosts() {
        new AsyncTask<Void, Void, Listing<Submission>>() {
            @Override
            protected Listing<Submission> doInBackground(Void... voids) {
                return paginator.next();
            }

            @Override
            protected void onPostExecute(Listing<Submission> listings) {
                if (listings != null) {
                    posts.addAll(listings);
                    adapter.setPosts(posts);
                }
            }
        }.execute();
    }

    private void refreshAccessTokenAsync() {
        new AsyncTask<Credentials, Void, Void>() {
            @Override
            protected Void doInBackground(Credentials... params) {
                try {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                } catch (NoSuchTokenException | OAuthException e) {
                    Log.e(TAG, "Could not refresh access token", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                Log.d(TAG, "Reauthenticated");
                loadPosts();
            }
        }.execute();
    }

    @Override
    public void onPostClicked(Submission post) {
        Intent i = new Intent(this, PostDetailActivity.class);
        i.putExtra(PostDetailActivity.POST_JSON, post.getDataNode().toString());
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            startActivity(new Intent(this, SearchForSubredditsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}