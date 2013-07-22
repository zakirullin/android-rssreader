package ru.oren.rssreader.interfaces;

import android.os.AsyncTask;
import ru.oren.rssreader.entities.Article;

import java.util.ArrayList;

public interface RssFetcherObserver {
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task);

    public boolean isAsync();
}
