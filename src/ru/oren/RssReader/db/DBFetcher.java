package ru.oren.RssReader.db;

import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public class DBFetcher extends AsyncTask<String, Void, ArrayList<Article>> {
    private ArrayList<DBFetcherListener> listeners = new ArrayList<DBFetcherListener>();

    public ArrayList<Article> doInBackground(String... params) {
        return DB.getInstance().getArticles();
    }

    public void onPostExecute(ArrayList<Article> articles) {
        for (DBFetcherListener listener : listeners) {
            listener.onDBFetchingFinished(articles, this);
        }
    }

    public void addListener(DBFetcherListener listener) {
        listeners.add(listener);
    }
}
