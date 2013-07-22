package ru.oren.rssreader.db;

import android.os.AsyncTask;
import ru.oren.rssreader.entities.Article;
import ru.oren.rssreader.interfaces.DBFetcherObserver;
import ru.oren.rssreader.interfaces.Observable;

import java.util.ArrayList;

public class DBFetcher extends AsyncTask<String, Void, ArrayList<Article>> implements Observable {
    private ArrayList<DBFetcherObserver> observers = new ArrayList<DBFetcherObserver>();

    public ArrayList<Article> doInBackground(String... params) {
        return DB.getInstance().getArticles();
    }

    public void onPostExecute(ArrayList<Article> articles) {
        for (DBFetcherObserver observer : observers) {
            observer.onDBFetchingFinished(articles, this);
        }
    }

    public void addObserver(Object observer) {
        observers.add((DBFetcherObserver) observer);
    }

    public void removeAllObservers() {
        observers.clear();
    }
}
