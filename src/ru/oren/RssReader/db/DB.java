package ru.oren.RssReader.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;
import ru.oren.RssReader.parser.RssFetcherListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class DB implements RssFetcherListener {
    public static final String DB_NAME = "OrenNewsDB";
    public static final String ARTICLES_TABLE_NAME = "articles";
    public static final int DB_VERSION = 18;

    private ReentrantLock dbAccess = new ReentrantLock();
    private DBOpenHelper dbOpenHelper;
    private SQLiteDatabase db;
    private static DB dbInstance = null;

    public static void init(Context context) {
        dbInstance = new DB(context);
    }

    public static DB getInstance() {
        return dbInstance;
    }

    public ArrayList<Article> getArticles() {
        dbAccess.lock();
        open();

        String query = String.format("SELECT * FROM %1$s ORDER BY %2$s DESC", ARTICLES_TABLE_NAME, Article.DATE);
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<Article> articles = new ArrayList<Article>();
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndex(Article.ID);
            int titleColumnIndex = cursor.getColumnIndex(Article.TITLE);
            int descriptionColumnIndex = cursor.getColumnIndex(Article.DESCRIPTION);
            int textColumnIndex = cursor.getColumnIndex(Article.TEXTDB);
            int imageColumnIndex = cursor.getColumnIndex(Article.IMAGE);
            int dateColumnIndex = cursor.getColumnIndex(Article.DATE);
            int viewedColumnIndex = cursor.getColumnIndex(Article.VIEWED);

            do {
                Article article = new Article();
                article.setId(cursor.getLong(idColumnIndex));
                article.setTitle(cursor.getString(titleColumnIndex));
                article.setDescription(cursor.getString(descriptionColumnIndex));
                article.setText(cursor.getString(textColumnIndex));
                article.setDate(new Date(cursor.getLong(dateColumnIndex) * 100));
                article.setViewed(cursor.getInt(viewedColumnIndex) == 1);

                byte[] imageBytes = cursor.getBlob(imageColumnIndex);
                ByteArrayInputStream imageStream = new ByteArrayInputStream(imageBytes);
                article.setImage(BitmapFactory.decodeStream(imageStream));

                articles.add(article);
            } while (cursor.moveToNext());
        }

        cursor.close();
        close();
        dbAccess.unlock();

        return articles;
    }

    public Date getLastArticleDate() {
        dbAccess.lock();
        open();

        String query = String.format("SELECT * FROM %1$s WHERE %2$s = (SELECT MAX( %2$s ) FROM %1$s )", ARTICLES_TABLE_NAME, Article.DATE);
        Cursor cursor = db.rawQuery(query, null);

        Date date = null;
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            date = new Date(cursor.getLong(cursor.getColumnIndex(Article.DATE)) * 100);
        }

        cursor.close();
        close();
        dbAccess.unlock();

        return date;
    }

    public void setViewed(ArrayList<Long> ids) {
        dbAccess.lock();
        open();

        for (long id : ids) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Article.VIEWED, 1);
            db.update(ARTICLES_TABLE_NAME, contentValues, Article.ID + " = " + Long.toString(id), null);
        }

        close();
        dbAccess.unlock();
    }

    @Override
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        dbAccess.lock();
        open();

        for (Article article : articles) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Article.TITLE, article.getTitle());
            contentValues.put(Article.DESCRIPTION, article.getDescription());
            contentValues.put(Article.TEXTDB, article.getText());
            contentValues.put(Article.DATE, article.getDate().getTime() / 100);
            contentValues.put(Article.VIEWED, 0);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            article.getImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] imageBytes = stream.toByteArray();
            contentValues.put(Article.IMAGE, imageBytes);

            db.insert(ARTICLES_TABLE_NAME, null, contentValues);
        }

        close();
        dbAccess.unlock();
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    private DB(Context context) {
        dbOpenHelper = new DBOpenHelper(context, DB_NAME, null, DB_VERSION);
    }

    private void open() {
        db = dbOpenHelper.getWritableDatabase();
    }

    private void close() {
        db.close();
    }
}
