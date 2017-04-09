package com.ashok.simplereader.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by ashok on 3/4/17.
 */

@ContentProvider(authority = PostProvider.AUTHORITY, database = PostDatabase.class)
public class PostProvider {
    public static final String AUTHORITY = "com.ashok.simplereader.data.PostProvider";
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static Uri buildUri(String... paths) {
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path : paths) {
            builder.appendPath(path);
        }
        return builder.build();
    }

    interface Path {
        String POSTS = "posts";
    }

    @TableEndpoint(table = PostDatabase.POSTS)
    public static class Posts {
        @ContentUri(
                path = Path.POSTS,
                type = "vnd.android.cursor.dir/post")
        public static final Uri CONTENT_URI = buildUri(Path.POSTS);

        @InexactContentUri(
                name = "POST_ID",
                path = Path.POSTS + "/#",
                type = "vnd.android.cursor.item/post",
                whereColumn = PostColumns._ID,
                pathSegment = 1)
        public static Uri withId(Integer id) {
            return buildUri(Path.POSTS, String.valueOf(id));
        }
    }
}
