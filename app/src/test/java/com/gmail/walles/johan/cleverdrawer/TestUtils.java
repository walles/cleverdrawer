package com.gmail.walles.johan.cleverdrawer;

import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

public class TestUtils {
    public static DataSource getMigratedFileDataSource(File dbFile) throws IOException {
        dbFile.deleteOnExit();

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        DatabaseUtils.migrate(dataSource);

        return dataSource;
    }

}
