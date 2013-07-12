package ru.oren.RssReader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.oren.RssReader.entities.Article;

public class DBOpenHelper extends SQLiteOpenHelper {

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + DB.ARTICLES_TABLE_NAME + " (" +
                Article.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Article.TITLE + " TEXT," +
                Article.DESCRIPTION + " TEXT," +
                Article.TEXTDB + " TEXT," +
                Article.IMAGE + " BLOB," +
                Article.DATE + " INTEGER," +
                Article.VIEWED + " INTEGER)";
        sqLiteDatabase.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DB.ARTICLES_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
