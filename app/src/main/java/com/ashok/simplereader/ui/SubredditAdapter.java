package com.ashok.simplereader.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ashok.simplereader.MySubreddit;
import com.ashok.simplereader.R;
import com.ashok.simplereader.utils.PrefUtils;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Subreddit;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ashok on 31/3/17.
 */

public class SubredditAdapter extends RecyclerView.Adapter<SubredditAdapter.SubredditAdapterViewHolder> {
    private Context context;
    private List<MySubreddit> mySubreddits;

    public SubredditAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<MySubreddit> mySubreddits) {
        this.mySubreddits = mySubreddits;
        notifyDataSetChanged();
    }

    @Override
    public SubredditAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_subreddit_list, parent, false);
        return new SubredditAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SubredditAdapterViewHolder holder, int position) {
        final MySubreddit mySubreddit = mySubreddits.get(position);
        final Subreddit subreddit = mySubreddit.getSubreddit();
        holder.mSubreddit.setText(subreddit.getDisplayName());
        holder.mNumSubscribers.setText(subreddit.getSubscriberCount() + " subscribers");
        try {
            if (subreddit.isUserSubscriber()) {
                holder.mSubreddit.setChecked(true);
            } else {
                holder.mSubreddit.setChecked(false);
            }
        } catch (Exception e) {
        }

        if (mySubreddit.isFavorite()) {
            holder.mFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            holder.mFavorite.setImageResource(R.drawable.ic_not_favorite);
        }

        holder.mFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mySubreddit.setFavorite(!mySubreddit.isFavorite());
                if (mySubreddit.isFavorite()) {
                    holder.mFavorite.setImageResource(R.drawable.ic_favorite);
                    PrefUtils.addFavoriteSubreddit(context, subreddit.getId());
                } else {
                    holder.mFavorite.setImageResource(R.drawable.ic_not_favorite);
                    PrefUtils.removeFavoriteSubreddit(context, subreddit.getId());
                }
            }
        });

        holder.mSubreddit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                manageSubscription(subreddit, b);
            }
        });
    }

    private void manageSubscription(final Subreddit subreddit, final boolean b) {
        final AccountManager manager = new AccountManager(AuthenticationManager.get().getRedditClient());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (b) {
                    manager.subscribe(subreddit);
                } else {
                    manager.unsubscribe(subreddit);
                }
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        if (mySubreddits == null)
            return 0;
        return mySubreddits.size();
    }

    public class SubredditAdapterViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.subreddit)
        CheckBox mSubreddit;
        @BindView(R.id.favorite)
        ImageView mFavorite;
        @BindView(R.id.no_subscribers)
        TextView mNumSubscribers;

        public SubredditAdapterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}