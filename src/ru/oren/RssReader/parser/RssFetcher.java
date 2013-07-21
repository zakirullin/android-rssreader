package ru.oren.RssReader.parser;

import android.os.AsyncTask;
import ru.oren.RssReader.db.DB;
import ru.oren.RssReader.entities.Article;
import ru.oren.RssReader.interfaces.Observable;
import ru.oren.RssReader.interfaces.RssFetcherObserver;

import java.util.ArrayList;

public class RssFetcher extends AsyncTask<String, Void, ArrayList<Article>> implements Observable {
    private ArrayList<RssFetcherObserver> observers = new ArrayList<RssFetcherObserver>();

    @Override
    public ArrayList<Article> doInBackground(String... params) {
        RssParser parser = new RssParser();
        return parser.parse(DB.getInstance().getLastArticleDate(), DB.getInstance().getLastId());
    }

    @Override
    protected void onPostExecute(final ArrayList<Article> articles) {
        super.onPostExecute(articles);

        for (final RssFetcherObserver observer : observers) {
            if (observer.isAsync()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        observer.onRssFetchingFinished(articles, RssFetcher.this);
                    }
                }).start();
            } else {
                observer.onRssFetchingFinished(articles, this);
            }
        }
    }

    public void addObserver(Object observer) {
        observers.add((RssFetcherObserver) observer);
    }

    public void removeAllObservers() {
        observers.clear();
    }
}
