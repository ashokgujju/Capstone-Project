package com.ashok.simplereader.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ashok.simplereader.R;
import com.ashok.simplereader.utils.DateTimeUtil;

import net.dean.jraw.auth.AuthenticationManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ButterKnife.bind(this);

        new AsyncTask<Void, Void, Object>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressbar.setVisibility(View.VISIBLE);
                mCardView.setVisibility(View.GONE);
            }

            @Override
            protected Object doInBackground(Void... voids) {
                try {
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
                    mKarma.setText(account.getLinkKarma() + getString(R.string.karma));
                    mGoldCredits.setText(account.getCreddits() + getString(R.string.gold_credits));
                    mRedditAge.setText(DateTimeUtil.convert(account.getCreated().getTime())
                            .concat(getString(R.string.reddit_age)));
                }
            }
        }.execute();
    }
}
