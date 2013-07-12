package ru.oren.RssReader.parser;

import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public interface RssFetcherListener {
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task);

    public boolean isAsync();
}
