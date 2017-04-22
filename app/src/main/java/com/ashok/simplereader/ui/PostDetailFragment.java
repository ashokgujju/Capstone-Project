package com.ashok.simplereader.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
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
import com.ashok.simplereader.utils.DateTimeUtil;
import com.ashok.simplereader.utils.RedditApiKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.picasso.Picasso;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.KarmaBreakdown;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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
    @BindView(R.id.progressbar)
    ProgressBar mProgressbar;

    private Submission post = null;
    private CommentsAdapter adapter;
    private Boolean isPostLiked = null;

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

        mSubreddit.setText(post.data(RedditApiKeys.SUBREDDIT_NAME_PREFIXED)
                .concat("   u/").concat(post.getAuthor())
                .concat("   ").concat(DateTimeUtil.convert(post.getCreated().getTime())));
        mTitle.setText(post.getTitle());

        adapter = new CommentsAdapter(getActivity());
        LinearLayoutManager manager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        mCommentsRecyclerView.setLayoutManager(manager);
        mCommentsRecyclerView.setAdapter(adapter);

        if (post.data("likes") != null) {
            if (Boolean.parseBoolean(post.data("likes"))) {
                isPostLiked = true;
                setDrawableLeft(mUpVotes, R.drawable.ic_arrow_upward_red);
            } else {
                isPostLiked = false;
                setDrawableLeft(mDownVotes, R.drawable.ic_arrow_downward_red);
            }
        } else {
            isPostLiked = null;
            setDrawableLeft(mUpVotes, R.drawable.ic_arrow_upward);
            setDrawableLeft(mDownVotes, R.drawable.ic_arrow_downward);
        }

        mUpVotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPostLiked == null || !isPostLiked) {
                    isPostLiked = true;
                    votePost(post, VoteDirection.UPVOTE,
                            R.drawable.ic_arrow_upward_red, R.drawable.ic_arrow_downward);
                } else {
                    isPostLiked = null;
                    votePost(post, VoteDirection.NO_VOTE,
                            R.drawable.ic_arrow_upward, R.drawable.ic_arrow_downward);
                }
            }
        });

        mUpVotes.setText(post.data(RedditApiKeys.UPS));
        mDownVotes.setText(post.data(RedditApiKeys.DOWNS));
        mDownVotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPostLiked == null || isPostLiked) {
                    isPostLiked = false;
                    votePost(post, VoteDirection.DOWNVOTE,
                            R.drawable.ic_arrow_upward, R.drawable.ic_arrow_downward_red);
                } else {
                    isPostLiked = null;
                    votePost(post, VoteDirection.NO_VOTE,
                            R.drawable.ic_arrow_upward, R.drawable.ic_arrow_downward);
                }
            }
        });
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
            try {
                RedditClient client = AuthenticationManager.get().getRedditClient();
                Submission post = client.getSubmission(postId);
                return post.getComments();
            } catch (Exception e) {
                return null;
            }
        }
    }

    @OnClick(R.id.share)
    public void sharePost() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, post.getPermalink());
        getActivity().startActivity(shareIntent);
    }

    private void votePost(Submission submission,
                          VoteDirection voteDirection, int upward_arrow, int downward_arrow) {
        vote(submission, voteDirection);
        setDrawableLeft(mUpVotes, upward_arrow);
        setDrawableLeft(mDownVotes, downward_arrow);
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

    private void setDrawableLeft(TextView view, int drawableId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setCompoundDrawablesWithIntrinsicBounds(
                    getActivity().getDrawable(drawableId),
                    null, null, null);
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(
                    getActivity().getResources().getDrawable(drawableId),
                    null, null, null
            );
        }
    }

}