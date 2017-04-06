package com.ashok.simplereader.data;

import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.Constraints;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.UniqueConstraint;

/**
 * Created by ashok on 3/4/17.
 */
@Constraints(unique = @UniqueConstraint(
        name = "UNQ_TAG_FOR_ID",
        columns = {PostColumns._ID},
        onConflict = ConflictResolutionType.REPLACE
))
public interface PostColumns {
    @DataType(DataType.Type.INTEGER)
    @PrimaryKey
    String _ID = "_id";

    @DataType(DataType.Type.TEXT)
    @NotNull
    String DATA = "data";
}
