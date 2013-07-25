package ru.oren.rssreader.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import ru.oren.rssreader.R;
import ru.oren.rssreader.adapters.ListAdapter;
import ru.oren.rssreader.db.DB;
import ru.oren.rssreader.db.DBFetcher;
import ru.oren.rssreader.entities.Article;
import ru.oren.rssreader.interfaces.DBFetcherObserver;
import ru.oren.rssreader.interfaces.Observable;
import ru.oren.rssreader.interfaces.RssFetcherObserver;
import ru.oren.rssreader.parser.RssFetcher;
import ru.oren.rssreader.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class ArticleListActivity extends Activity implements DBFetcherObserver, RssFetcherObserver {
    private final int TOAST_TIMEOUT = 3;
    private final int ANIMATION_DURATION = 700;
    private final int RETAIN_INSTANCE_RSS_FETCHER = 0;
    private final int RETAIN_INSTANCE_ARTICLES = 1;

    private enum State {START_FETCHING_FROM_DB, END_FETCHING_FROM_DB, EMPTY_DB, START_FETCHING_FROM_RSS, END_FETCHING_FROM_RSS, NEW_CONTENT_FROM_RSS};
    private State currentState = null;

    private ListAdapter listAdapter;
    private boolean refreshEnabled = false;
    private boolean configurationChanged = false;
    private ArrayList<AsyncTask> unfinishedTasks = new ArrayList<AsyncTask>();
    private AsyncTask rssFetcherTask = null;
    private ArrayList<Long> viewedArticles = new ArrayList<Long>();

    public void refreshList(View view) {
        if (!refreshEnabled) {
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet), TOAST_TIMEOUT).show();
        } else {
            setState(State.START_FETCHING_FROM_RSS);

            RssFetcher rssFetcher = new RssFetcher();
            rssFetcher.addObserver(this);
            rssFetcher.addObserver(DB.getInstance());
            rssFetcher.execute();
            unfinishedTasks.add(rssFetcher);
            rssFetcherTask = rssFetcher;
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        configurationChanged = true;

        HashMap<Integer, Object> retainInstance = new HashMap<Integer, Object>();
        retainInstance.put(RETAIN_INSTANCE_RSS_FETCHER, rssFetcherTask);
        retainInstance.put(RETAIN_INSTANCE_ARTICLES, listAdapter.getArticles());

        return retainInstance;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onDBFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        setState(State.END_FETCHING_FROM_DB);

        unfinishedTasks.remove(task);

        if (articles.size() != 0) {
            listAdapter.addArticles(articles);
            listAdapter.notifyDataSetChanged();
        } else {
            setState(State.EMPTY_DB);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        setState(State.END_FETCHING_FROM_RSS);

        unfinishedTasks.remove(task);
        rssFetcherTask = null;

        if (articles.size() != 0) {
            setState(State.NEW_CONTENT_FROM_RSS);

            listAdapter.prependArticles(articles);
            listAdapter.notifyDataSetChanged();

            Toast.makeText(getApplicationContext(), getString(R.string.new_articles) + Integer.toString(articles.size()), TOAST_TIMEOUT).show();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_new_articles), TOAST_TIMEOUT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_article_list);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

        listAdapter = new ListAdapter(getApplicationContext());
        ((ListView) findViewById(R.id.lvArticles)).setAdapter(listAdapter);

        ((ListView) findViewById(R.id.lvArticles)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                Article article = ((Article) listAdapter.getItem(index));

                if (!article.isViewed()) {
                    viewedArticles.add(listAdapter.getItemId(index));
                    article.setViewed(true);
                    listAdapter.notifyDataSetChanged();
                }

                Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                intent.putExtra(Article.TITLE, article.getTitle());
                intent.putExtra(Article.TEXT, article.getText());
                startActivity(intent);
            }
        });

        DB.init(getApplicationContext());

        // Restore state if screen was rotated previously
        boolean mustFetchFromDB = true;
        HashMap<Integer, Object> retainInstance = (HashMap<Integer, Object>) getLastNonConfigurationInstance();
        if (retainInstance != null) {
            // Restore RssFetcher task
            rssFetcherTask = (AsyncTask) retainInstance.get(RETAIN_INSTANCE_RSS_FETCHER);
            if (rssFetcherTask != null) {
                unfinishedTasks.add(rssFetcherTask);
                ((Observable) rssFetcherTask).addObserver(this);
                ((Observable) rssFetcherTask).addObserver(DB.getInstance());
                setState(State.START_FETCHING_FROM_RSS);
            }

            // Restore data fetched from DB
            ArrayList<Article> articles = (ArrayList<Article>) retainInstance.get(RETAIN_INSTANCE_ARTICLES);
            if (articles.size() != 0) {
                mustFetchFromDB = false;
                listAdapter.addArticles(articles);
                listAdapter.notifyDataSetChanged();
                setState(State.END_FETCHING_FROM_DB);
            }
        }
        if (mustFetchFromDB) {
            DBFetcher dbFetcher = new DBFetcher();
            dbFetcher.addObserver(this);
            dbFetcher.execute();
            unfinishedTasks.add(dbFetcher);
            setState(State.START_FETCHING_FROM_DB);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (viewedArticles.size() != 0) {
            final ArrayList<Long> copiedViewedArticles = new ArrayList<Long>(viewedArticles);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DB.getInstance().setArticlesViewed(copiedViewedArticles);
                }
            }).start();
            viewedArticles.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (AsyncTask unfinishedTask : unfinishedTasks) {
            ((Observable) unfinishedTask).removeAllObservers();

            boolean crossConfigurationTask = ((unfinishedTask == rssFetcherTask) && configurationChanged);
            if (!crossConfigurationTask) {
                unfinishedTask.cancel(false);
            }
        }
    }

    private void setState(State state) {
        switch (state) {
            case START_FETCHING_FROM_DB: {
                showProcessAnimation(true);
                break;
            }

            case END_FETCHING_FROM_DB: {
                findViewById(R.id.tvLoadArticlesMessage).setVisibility(View.GONE);

                boolean backgroundTaskRunning = rssFetcherTask != null;
                if (!backgroundTaskRunning) {
                    showProcessAnimation(false);
                }
                break;
            }

            case EMPTY_DB: {
                findViewById(R.id.tvNoArticlesMessage).setVisibility(View.VISIBLE);
                break;
            }

            case START_FETCHING_FROM_RSS: {
                showProcessAnimation(true);
                break;
            }

            case END_FETCHING_FROM_RSS: {
                boolean backgroundTaskRunning = currentState == State.START_FETCHING_FROM_DB;
                if (!backgroundTaskRunning) {
                    showProcessAnimation(false);
                }
                break;
            }

            case NEW_CONTENT_FROM_RSS: {
                findViewById(R.id.tvNoArticlesMessage).setVisibility(View.GONE);
                ((ListView) findViewById(R.id.lvArticles)).smoothScrollToPosition(0);
                break;
            }
        }

        currentState = state;
    }

    private void showProcessAnimation(boolean flag) {
        if (flag) {
            RotateAnimation animation = new RotateAnimation(0.0f, 360.0f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            animation.setDuration(ANIMATION_DURATION);
            animation.setRepeatCount(RotateAnimation.INFINITE);
            animation.setInterpolator(new LinearInterpolator());
            findViewById(R.id.ivUpdate).startAnimation(animation);
        } else {
            findViewById(R.id.ivUpdate).clearAnimation();
        }

        refreshEnabled = !flag;
    }
}