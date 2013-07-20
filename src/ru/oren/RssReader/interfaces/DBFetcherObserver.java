package ru.oren.RssReader.interfaces;

import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public interface DBFetcherObserver {
    public void onDBFetchingFinished(ArrayList<Article> articles, AsyncTask task);
}
