package com.ashok.simplereader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;

/**
 * Created by ashok on 31/3/17.
 */

public class SubredditAdapter extends BaseAdapter {
    private Context context;
    private Listing<Subreddit> subreddits;

    public SubredditAdapter(Context context) {
        this.context = context;
    }

    public void setData(Listing<Subreddit> subreddits) {
        this.subreddits = subreddits;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (subreddits == null)
            return 0;
        return subreddits.size();
    }

    @Override
    public Object getItem(int i) {
        return subreddits.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).
                    inflate(R.layout.item_subreddit_list, viewGroup, false);
        }

        final Subreddit subreddit = subreddits.get(i);

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.subreddit);
        checkBox.setText(subreddit.getDisplayName());
        if (subreddit.isUserSubscriber()) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                RedditClient client = AuthenticationManager.get().getRedditClient();
                final AccountManager manager = new AccountManager(client);
                try {
                    if (b) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                manager.subscribe(subreddit);
                            }
                        }).start();
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                manager.unsubscribe(subreddit);
                            }
                        }).start();
                    }
                } catch (Exception e) {
                }
            }
        });

        return view;
    }
}
