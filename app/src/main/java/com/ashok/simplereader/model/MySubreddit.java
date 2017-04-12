package com.ashok.simplereader.model;

import net.dean.jraw.models.Subreddit;

/**
 * Created by ashok on 6/4/17.
 */

public class MySubreddit {
    private boolean isFavorite;
    private Subreddit subreddit;

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public Subreddit getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(Subreddit subreddit) {
        this.subreddit = subreddit;
    }
}
