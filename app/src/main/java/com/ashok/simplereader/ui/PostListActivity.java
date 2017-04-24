package com.ashok.simplereader.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
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
import android.widget.TextView;

import com.ashok.simplereader.MyApplication;
import com.ashok.simplereader.R;
import com.ashok.simplereader.data.PostColumns;
import com.ashok.simplereader.data.PostProvider;
import com.ashok.simplereader.sync.PostSyncJob;
import com.ashok.simplereader.utils.PrefUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.ashok.simplereader.sync.PostSyncJob.ACTION_DATA_UPDATED;

public class PostListActivity extends AppCompatActivity implements PostsAdapter.OnPostClickListener,
        LoaderManager.LoaderCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int POSTS_LOADER_ID = 11;

    @BindView(R.id.post_list)
    RecyclerView mPostsRV;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.adView)
    AdView mAdView;
    @BindView(R.id.empty_msg)
    TextView mEmptyMsg;

    private String TAG = PostListActivity.class.getSimpleName();
    private List<Submission> posts = new ArrayList<>();
    private PostsAdapter adapter;
    private SubredditPaginator paginator;
    private SharedPreferences preferences;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        ButterKnife.bind(this);

        MyApplication application = (MyApplication) getApplication();
        mTracker = application.getDefaultTracker();

        MobileAds.initialize(getApplicationContext(), getString(R.string.test_id));
        mAdView.loadAd(new AdRequest.Builder().build());

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getTitle());

        adapter = new PostsAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        mPostsRV.setLayoutManager(layoutManager);
        mPostsRV.setAdapter(adapter);
        adapter.setOnPostClickListener(this);
        mPostsRV.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
        mPostsRV.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                getSupportLoaderManager().restartLoader(POSTS_LOADER_ID, null,
                        PostListActivity.this).forceLoad();
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPosts();
            }
        });

        PostSyncJob.initialize(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);

        mTracker.setScreenName(getString(R.string.posts_list_screen));
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        checkAuthentication();
    }

    private void checkAuthentication() {
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Log.d(TAG, "AuthenticationState for onResume(): " + state);
        switch (state) {
            case READY:
                if (posts.size() == 0) {
                    loadPosts();
                }
                break;
            case NONE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.login);
                builder.setCancelable(false);
                builder.setMessage(R.string.login_with_reddit);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        startActivity(new Intent(PostListActivity.this, LoginActivity.class));
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case NEED_REFRESH:
                refreshAccessTokenAsync();
                break;
        }
    }

    private void refreshAccessTokenAsync() {
        new AsyncTask<Credentials, Void, Void>() {
            @Override
            protected Void doInBackground(Credentials... params) {
                try {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                } catch (Exception e) {
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
        i.putExtra(PostDetailActivity.ARG_ITEM_ID, post.getDataNode().toString());
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String sortType = preferences.getString(getString(R.string.key_sort_type),
                getString(R.string.sort_hot));
        if (sortType.equals(getString(R.string.sort_hot))) {
            menu.findItem(R.id.hot).setChecked(true);
        } else if (sortType.equals(getString(R.string.sort_new))) {
            menu.findItem(R.id.latest).setChecked(true);
        } else if (sortType.equals(getString(R.string.sort_top))) {
            menu.findItem(R.id.top).setChecked(true);
        } else if (sortType.equals(getString(R.string.sort_controversial))) {
            menu.findItem(R.id.controversial).setChecked(true);
        } else {
            return super.onPrepareOptionsMenu(menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                startActivity(new Intent(this, SearchForSubredditsActivity.class));
                break;
            case R.id.manage_srs:
                startActivity(new Intent(this, ManageSubredditsActivity.class));
                break;
            case R.id.account:
                startActivity(new Intent(this, UserProfileActivity.class));
                break;
            case R.id.latest:
                item.setChecked(true);
                saveSortPreference(getString(R.string.sort_new));
                break;
            case R.id.top:
                item.setChecked(true);
                saveSortPreference(getString(R.string.sort_top));
                break;
            case R.id.hot:
                item.setChecked(true);
                saveSortPreference(getString(R.string.sort_hot));
                break;
            case R.id.controversial:
                item.setChecked(true);
                saveSortPreference(getString(R.string.sort_controversial));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void saveSortPreference(String sortType) {
        preferences.edit().putString(getString(R.string.key_sort_type), sortType).apply();
    }

    private void loadPosts() {
        mSwipeRefreshLayout.setRefreshing(true);
        this.posts.clear();
        adapter.setPosts(null);
        paginator = new SubredditPaginator(AuthenticationManager.get().getRedditClient());
        paginator.setSorting(getSortType());
        getSupportLoaderManager().restartLoader(POSTS_LOADER_ID, null, this).forceLoad();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new PostsAsyncTaskLoader(this, paginator);
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyMsg.setVisibility(View.GONE);
        mPostsRV.setVisibility(View.VISIBLE);

        if (data != null) {
            List<Submission> posts = (List<Submission>) data;
            this.posts.addAll(posts);
            adapter.setPosts(this.posts);
        }


        if (adapter.getItemCount() == 0) {
            mEmptyMsg.setVisibility(View.VISIBLE);
            mPostsRV.setVisibility(View.GONE);
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

    public Sorting getSortType() {
        String type = preferences.getString(getString(R.string.key_sort_type),
                getString(R.string.sort_hot));
        if (type.equals(getString(R.string.sort_hot))) {
            return Sorting.HOT;
        } else if (type.equals(getString(R.string.sort_new))) {
            return Sorting.NEW;
        } else if (type.equals(getString(R.string.sort_top))) {
            return Sorting.TOP;
        } else if (type.equals(getString(R.string.sort_controversial))) {
            return Sorting.CONTROVERSIAL;
        }
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.key_sort_type))) {
            loadPosts();
        }
    }

    private static class PostsAsyncTaskLoader extends AsyncTaskLoader {
        private Paginator paginator;
        private Context context;

        public PostsAsyncTaskLoader(Context context, Paginator paginator) {
            super(context);
            this.paginator = paginator;
            this.context = context;
        }

        @Override
        public Object loadInBackground() {
            try {
                AuthenticationState state = AuthenticationManager.get().checkAuthState();
                if (state == AuthenticationState.NEED_REFRESH) {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                }

                List<Submission> favSRPosts = new ArrayList<>();
                List<Submission> notFavSRPosts = new ArrayList<>();
                Set<String> favSubredditIds = PrefUtils.getFavoriteSubreddits(context);
                List<Submission> posts = paginator.next();

                //cache posts for widget
                if (paginator.getPageIndex() == 1) {
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

                //show posts from favorite subreddit at the top
                for (Submission post : posts) {
                    if (favSubredditIds.contains(post.getSubredditId()
                            .replace(context.getString(R.string.subreddit_prefix), ""))) {
                        favSRPosts.add(post);
                    } else {
                        notFavSRPosts.add(post);
                    }
                }
                favSRPosts.addAll(notFavSRPosts);
                return favSRPosts;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onStop() {
        super.onStop();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}