package com.ashok.simplereader.data;

import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by ashok on 3/4/17.
 */

public class PostColumns {
    @DataType(DataType.Type.INTEGER)
    @PrimaryKey
    public static final String _ID = "_id";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String DATA = "data";
}
