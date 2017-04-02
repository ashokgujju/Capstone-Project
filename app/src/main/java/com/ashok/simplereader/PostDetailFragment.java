package com.ashok.simplereader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.squareup.picasso.Picasso;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PostDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private Submission post = null;
    private CommentsAdapter adapter;

    @BindView(R.id.subreddit)
    TextView mSubreddit;
    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.body)
    TextView mBody;
    @BindView(R.id.photo)
    ImageView mPhoto;
    @BindView(R.id.commentsList)
    RecyclerView mCommentsRecyclerView;
    @BindView(R.id.ups)
    TextView mUpVotes;
    @BindView(R.id.downs)
    TextView mDownVotes;
    @BindView(R.id.comments)
    TextView mNumComments;
    @BindView(R.id.share)
    TextView mShare;
    @BindView(R.id.progressbar)
    ProgressBar mProgressbar;

    public PostDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String postJson = getArguments().getString(ARG_ITEM_ID);
            try {
                post = new Submission(new ObjectMapper().readTree(postJson));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.post_detail, container, false);
        ButterKnife.bind(this, rootView);

        mSubreddit.setText(post.data("subreddit_name_prefixed"));
        mTitle.setText(post.getTitle());

        adapter = new CommentsAdapter(getActivity());
        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mCommentsRecyclerView.setLayoutManager(manager);
        mCommentsRecyclerView.setAdapter(adapter);
        mCommentsRecyclerView.setNestedScrollingEnabled(false);
        mUpVotes.setText(post.data("ups"));
        mDownVotes.setText(post.data("downs"));
        mNumComments.setText(String.valueOf(post.getCommentCount()));

        switch (post.getPostHint()) {
            case SELF:
                try {
                    mBody.setVisibility(View.VISIBLE);
                    mBody.setText(Html.fromHtml(StringEscapeUtils.unescapeHtml4(post.data("selftext_html"))));
                    mBody.setMovementMethod(LinkMovementMethod.getInstance());
                } catch (Exception e) {
                }
                break;
            case LINK:
                break;
            case IMAGE:
                mPhoto.setVisibility(View.VISIBLE);
                Picasso.with(getActivity()).load(post.getThumbnail()).into(mPhoto);
                break;
            case VIDEO:
                break;
            case UNKNOWN:
                try {
                    mBody.setVisibility(View.VISIBLE);
                    mBody.setText(Html.fromHtml(StringEscapeUtils.unescapeHtml4(post.data("selftext_html"))));
                    mBody.setMovementMethod(LinkMovementMethod.getInstance());
                } catch (Exception e) {
                }
                break;
        }

        final RedditClient client = AuthenticationManager.get().getRedditClient();
        new AsyncTask<Void, Void, CommentNode>() {
            @Override
            protected void onPreExecute() {
                mProgressbar.setVisibility(View.VISIBLE);
            }

            @Override
            protected CommentNode doInBackground(Void... voids) {
                Submission submission = client.getSubmission(post.getId());
                return submission.getComments();
            }

            @Override
            protected void onPostExecute(CommentNode root) {
                mProgressbar.setVisibility(View.GONE);
                adapter.setCommentNodes(root.walkTree());
            }
        }.execute();

        return rootView;
    }
}
