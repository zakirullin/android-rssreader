package ru.oren.RssReader.db;

import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public interface DBFetcherListener {
    public void onDBFetchingFinished(ArrayList<Article> articles, AsyncTask task);
}
