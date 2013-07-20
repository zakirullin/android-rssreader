package ru.oren.RssReader.interfaces;

import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public interface RssFetcherObserver {
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task);

    public boolean isAsync();
}
