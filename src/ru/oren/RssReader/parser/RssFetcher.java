package ru.oren.RssReader.parser;

import android.os.AsyncTask;
import ru.oren.RssReader.db.DB;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public class RssFetcher extends AsyncTask<String, Void, ArrayList<Article>> {
    private ArrayList<RssFetcherListener> listeners = new ArrayList<RssFetcherListener>();

    @Override
    public ArrayList<Article> doInBackground(String... params) {
        RssParser parser = new RssParser();
        return parser.parse(DB.getInstance().getLastArticleDate());
    }

    @Override
    protected void onPostExecute(final ArrayList<Article> articles) {
        super.onPostExecute(articles);

        for (final RssFetcherListener listener : listeners) {
            if (listener.isAsync()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRssFetchingFinished(articles, RssFetcher.this);
                    }
                }).start();
            } else {
                listener.onRssFetchingFinished(articles, this);
            }
        }
    }

    public void addListener(RssFetcherListener listener) {
        listeners.add(listener);
    }
}
