package com.ashok.simplereader.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.ashok.simplereader.R;
import com.ashok.simplereader.data.PostColumns;
import com.ashok.simplereader.data.PostProvider;
import com.ashok.simplereader.ui.PostDetailActivity;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.Submission;

import java.io.IOException;

/**
 * Created by ashok on 3/4/17.
 */

public class PostWidgetRemoteViewService extends RemoteViewsService {
    private Context context;

    public PostWidgetRemoteViewService() {
        this.context = this;
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }


                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(PostProvider.Posts.CONTENT_URI,
                        null, null, null, null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position))
                    return null;

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.item_post_widget_list);

                try {
                    Submission post = new Submission(new ObjectMapper()
                            .readTree(data.getString(data.getColumnIndex(PostColumns.DATA))));

                    views.setTextViewText(R.id.title, post.getTitle());
                    views.setTextViewText(R.id.subreddit, post.data("subreddit_name_prefixed"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent fillIntent = new Intent();
                fillIntent.putExtra(PostDetailActivity.POST_JSON,
                        data.getString(data.getColumnIndex(PostColumns.DATA)));
                views.setOnClickFillInIntent(R.id.list_item, fillIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.item_post_widget_list);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int i) {
                if (data.moveToPosition(i))
                    return data.getLong(data.getColumnIndex(PostColumns._ID));
                return i;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}