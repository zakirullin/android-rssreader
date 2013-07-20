package ru.oren.RssReader.db;

import android.os.AsyncTask;
import ru.oren.RssReader.entities.Article;
import ru.oren.RssReader.interfaces.DBFetcherObserver;
import ru.oren.RssReader.interfaces.Observable;

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
