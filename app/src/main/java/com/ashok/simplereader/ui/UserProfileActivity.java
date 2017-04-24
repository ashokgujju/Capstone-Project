package com.ashok.simplereader.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ashok.simplereader.MyApplication;
import com.ashok.simplereader.R;
import com.ashok.simplereader.utils.DateTimeUtil;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.LoggedInAccount;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UserProfileActivity extends AppCompatActivity {
    @BindView(R.id.name)
    TextView mUsername;
    @BindView(R.id.karma)
    TextView mKarma;
    @BindView(R.id.gold_credits)
    TextView mGoldCredits;
    @BindView(R.id.reddit_age)
    TextView mRedditAge;
    @BindView(R.id.progressbar)
    ProgressBar mProgressbar;
    @BindView(R.id.cardview)
    CardView mCardView;
    @BindView(R.id.empty_msg)
    TextView mEmptyMsg;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ButterKnife.bind(this);

        MyApplication application = (MyApplication) getApplication();
        mTracker = application.getDefaultTracker();

        new AsyncTask<Void, Void, Object>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressbar.setVisibility(View.VISIBLE);
                mCardView.setVisibility(View.GONE);
                mEmptyMsg.setVisibility(View.GONE);
            }

            @Override
            protected Object doInBackground(Void... voids) {
                try {
                    AuthenticationState state = AuthenticationManager.get().checkAuthState();
                    if (state == AuthenticationState.NEED_REFRESH) {
                        AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                    }
                    return AuthenticationManager.get().getRedditClient().me();
                } catch (Exception e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object data) {
                mProgressbar.setVisibility(View.GONE);
                if (data != null) {
                    mCardView.setVisibility(View.VISIBLE);
                    LoggedInAccount account = (LoggedInAccount) data;
                    mUsername.append(account.getFullName());
                    mKarma.setText(String.valueOf(account.getLinkKarma()).concat(getString(R.string.karma)));
                    mGoldCredits.setText(String.valueOf(account.getCreddits())
                            .concat(getString(R.string.gold_credits)));
                    mRedditAge.setText(DateTimeUtil.convert(account.getCreated().getTime())
                            .concat(getString(R.string.reddit_age)));
                } else {
                    if (!networkUp()) {
                        mEmptyMsg.setVisibility(View.VISIBLE);
                        mEmptyMsg.setText(R.string.connect_to_internet);
                    }
                }
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName(getString(R.string.user_profile_screen));
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}
