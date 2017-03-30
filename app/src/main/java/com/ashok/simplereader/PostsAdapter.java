package com.ashok.simplereader;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import net.dean.jraw.models.Submission;

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

    interface OnPostClickListener {
        void onPostClicked(Submission post);
    }

    public void setOnPostClickListener(OnPostClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public PostsAdapter(Context context) {
        this.context = context;
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


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onPostClicked(submission);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (posts == null)
            return 0;
        return posts.size();
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
