package com.ashok.simplereader.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by ashok on 3/4/17.
 */

@Database(version = PostDatabase.VERSION)
public class PostDatabase {
    public static final int VERSION = 1;

    @Table(PostColumns.class)
    public static final String POSTS = "posts";
}
