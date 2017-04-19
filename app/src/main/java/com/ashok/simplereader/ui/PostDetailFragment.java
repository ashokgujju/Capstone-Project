package com.ashok.simplereader.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ashok.simplereader.R;
import com.ashok.simplereader.utils.RedditApiKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class PostDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks {
    public static final String ARG_ITEM_ID = "item_id";
    public static final int COMMENTS_LOADER_ID = 12;

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

    private Submission post = null;
    private CommentsAdapter adapter;

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

        mSubreddit.setText(post.data(RedditApiKeys.SUBREDDIT_NAME_PREFIXED).concat("   u/")
                .concat(post.getAuthor()));
        mTitle.setText(post.getTitle());

        adapter = new CommentsAdapter(getActivity());
        LinearLayoutManager manager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        mCommentsRecyclerView.setLayoutManager(manager);
        mCommentsRecyclerView.setAdapter(adapter);

        if (post.data(RedditApiKeys.LIKES) != null) {
            if (Boolean.parseBoolean(post.data(RedditApiKeys.LIKES))) {
                mUpVotes.setTextColor(Color.RED);
            } else {
                mUpVotes.setTextColor(Color.parseColor("#" + Integer
                        .toHexString(ContextCompat.getColor(getActivity(), R.color.secondary_text))));
            }
        }
        mUpVotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mUpVotes.setText(post.data(RedditApiKeys.UPS));
        mDownVotes.setText(post.data(RedditApiKeys.DOWNS));
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
            case VIDEO:
                try {
                    mBody.setVisibility(View.VISIBLE);
                    mBody.setText(post.getUrl());
                    mBody.setTextColor(Color.BLUE);
                    mBody.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                            customTabsIntent.launchUrl(getActivity(), Uri.parse(post.getUrl()));
                        }
                    });
                } catch (Exception e) {
                }
                break;
            case IMAGE:
                mPhoto.setVisibility(View.VISIBLE);

                String imageRosolutions = post.getDataNode().get("preview").get("images").get(0)
                        .get("resolutions").toString();
                try {
                    JSONArray resolutionsArr = new JSONArray(imageRosolutions);
                    for (int i = 0; i < resolutionsArr.length(); i++) {
                        JSONObject imageObject = resolutionsArr.getJSONObject(i);
                        if (imageObject.getInt("width") == 216) {
                            String imgUrl = Html.fromHtml(StringEscapeUtils
                                    .unescapeHtml4(imageObject.getString("url"))).toString();
                            Picasso.with(getActivity()).load(imgUrl).into(mPhoto);
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();

                }
                break;
            case UNKNOWN:
                try {
                    mBody.setVisibility(View.VISIBLE);
                    mBody.setText(Html.fromHtml(StringEscapeUtils.unescapeHtml4(post.data(RedditApiKeys.SELFTEXT_HTML))));
                    mBody.setMovementMethod(LinkMovementMethod.getInstance());
                } catch (Exception e) {
                }
                break;
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getSupportLoaderManager().initLoader(COMMENTS_LOADER_ID, null, this).forceLoad();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new CommentsAsyncTaskLoader(getActivity(), post.getId());
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        mProgressbar.setVisibility(View.GONE);
        if (data != null) {
            if (data instanceof CommentNode) {
                CommentNode root = (CommentNode) data;
                adapter.setCommentNodes(root.walkTree());
                mCommentsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    private static class CommentsAsyncTaskLoader extends AsyncTaskLoader {
        private String postId;

        public CommentsAsyncTaskLoader(Context context, String postId) {
            super(context);
            this.postId = postId;
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
        }

        @Override
        public Object loadInBackground() {
            RedditClient client = AuthenticationManager.get().getRedditClient();
            Submission post = client.getSubmission(postId);
            return post.getComments();
        }
    }
}