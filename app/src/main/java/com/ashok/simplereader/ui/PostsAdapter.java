package com.ashok.simplereader.ui;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ashok.simplereader.R;
import com.squareup.picasso.Picasso;

import net.dean.jraw.ApiException;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ashok on 29/3/17.
 */

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostsAdapterViewHolder> {

    private Context context;
    private List<Submission> posts;
    private OnPostClickListener clickListener;

    public PostsAdapter(Context context) {
        this.context = context;
    }

    public void setOnPostClickListener(OnPostClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setPosts(List<Submission> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    @Override
    public PostsAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_list, parent, false);
        return new PostsAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostsAdapterViewHolder holder, int position) {
        final Submission submission = posts.get(position);
        holder.mSubredditName.setText(submission.data("subreddit_name_prefixed"));
        holder.mTitle.setText(submission.getTitle());
        holder.mNumComments.setText(String.valueOf(submission.getCommentCount()));
        holder.mUpVotes.setText(submission.data("ups"));
        holder.mDownVotes.setText(submission.data("downs"));

        if (submission.getThumbnailType().equals(Submission.ThumbnailType.URL)) {
            holder.mThumbnail.setVisibility(View.VISIBLE);
            Picasso.with(context).load(submission.getThumbnail()).fit().centerCrop().into(holder.mThumbnail);
        } else {
            holder.mThumbnail.setVisibility(View.GONE);
        }

        holder.mUpVotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vote(submission, VoteDirection.UPVOTE);
            }
        });
        holder.mDownVotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vote(submission, VoteDirection.DOWNVOTE);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onPostClicked(submission);
            }
        });

        holder.mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, submission.getPermalink());
                context.startActivity(shareIntent);
            }
        });
    }

    private void vote(final Submission submission, final VoteDirection voteDirection) {
        final AccountManager manager = new AccountManager(AuthenticationManager.get().getRedditClient());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    manager.vote(submission, voteDirection);
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        if (posts == null)
            return 0;
        return posts.size();
    }

    interface OnPostClickListener {
        void onPostClicked(Submission post);
    }

    public class PostsAdapterViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.subreddit)
        TextView mSubredditName;
        @BindView(R.id.title)
        TextView mTitle;
        @BindView(R.id.thumbnail)
        ImageView mThumbnail;
        @BindView(R.id.ups)
        TextView mUpVotes;
        @BindView(R.id.downs)
        TextView mDownVotes;
        @BindView(R.id.comments)
        TextView mNumComments;
        @BindView(R.id.share)
        TextView mShare;

        public PostsAdapterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
