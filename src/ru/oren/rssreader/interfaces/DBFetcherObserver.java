package ru.oren.rssreader.interfaces;

import android.os.AsyncTask;
import ru.oren.rssreader.entities.Article;

import java.util.ArrayList;

public interface DBFetcherObserver {
    public void onDBFetchingFinished(ArrayList<Article> articles, AsyncTask task);
}
