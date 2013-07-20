package ru.oren.RssReader.activities;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import ru.oren.RssReader.R;
import ru.oren.RssReader.adapters.ListAdapter;
import ru.oren.RssReader.db.DB;
import ru.oren.RssReader.db.DBFetcher;
import ru.oren.RssReader.entities.Article;
import ru.oren.RssReader.interfaces.DBFetcherObserver;
import ru.oren.RssReader.interfaces.Observable;
import ru.oren.RssReader.interfaces.RssFetcherObserver;
import ru.oren.RssReader.parser.RssFetcher;
import ru.oren.RssReader.utils.NetworkUtil;

import java.util.ArrayList;

public class ArticleListActivity extends Activity implements DBFetcherObserver, RssFetcherObserver {
    private final int TOAST_TIMEOUT = 3;
    private final int ANIMATION_DURATION = 700;

    private enum VisualState {START_FETCHING_FROM_DB, END_FETCHING_FROM_DB, EMPTY_DB, START_FETCHING_FROM_RSS, END_FETCHING_FROM_RSS, NEW_CONTENT_FROM_RSS}

    ;

    private ListAdapter listAdapter;
    private boolean refreshEnabled = false;
    private ArrayList<AsyncTask> unfinishedTasks = new ArrayList<AsyncTask>();
    private ArrayList<Long> viewedArticles = new ArrayList<Long>();

    public void refreshList(View view) {
        if (!refreshEnabled) {
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet), TOAST_TIMEOUT).show();
        } else {
            setVisualState(VisualState.START_FETCHING_FROM_RSS);

            RssFetcher rssFetcher = new RssFetcher();
            rssFetcher.addObserver(this);
            rssFetcher.addObserver(DB.getInstance());
            rssFetcher.execute();
            unfinishedTasks.add(rssFetcher);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onDBFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        setVisualState(VisualState.END_FETCHING_FROM_DB);

        unfinishedTasks.remove(task);

        if (articles.size() != 0) {
            this.listAdapter.addArticles(articles);
            this.listAdapter.notifyDataSetChanged();
        } else {
            setVisualState(VisualState.EMPTY_DB);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        setVisualState(VisualState.END_FETCHING_FROM_RSS);

        unfinishedTasks.remove(task);

        if (articles.size() != 0) {
            setVisualState(VisualState.NEW_CONTENT_FROM_RSS);

            this.listAdapter.addArticles(articles);
            this.listAdapter.notifyDataSetChanged();

            Toast.makeText(getApplicationContext(), getString(R.string.new_articles) + Integer.toString(articles.size()), TOAST_TIMEOUT).show();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_new_articles), TOAST_TIMEOUT).show();
        }

        this.refreshEnabled = true;
    }

    private void setVisualState(VisualState visualState) {
        switch (visualState) {
            case START_FETCHING_FROM_DB: {
                setProcessAnimation(true);
                this.refreshEnabled = false;
                break;
            }

            case END_FETCHING_FROM_DB: {
                findViewById(R.id.tvLoadArticlesMessage).setVisibility(View.GONE);

                setProcessAnimation(false);
                this.refreshEnabled = true;
                break;
            }

            case EMPTY_DB: {
                findViewById(R.id.tvNoArticlesMessage).setVisibility(View.VISIBLE);
                break;
            }

            case START_FETCHING_FROM_RSS: {
                setProcessAnimation(true);
                this.refreshEnabled = false;
                break;
            }

            case END_FETCHING_FROM_RSS: {
                setProcessAnimation(false);
                this.refreshEnabled = true;
                break;
            }

            case NEW_CONTENT_FROM_RSS: {
                findViewById(R.id.tvNoArticlesMessage).setVisibility(View.GONE);
                ((ListView) findViewById(R.id.lvArticles)).smoothScrollToPosition(0);
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_article_list);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

        this.listAdapter = new ListAdapter(getApplicationContext());
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
        DBFetcher dbFetcher = new DBFetcher();
        dbFetcher.addObserver(this);
        dbFetcher.execute();
        unfinishedTasks.add(dbFetcher);
        setVisualState(VisualState.START_FETCHING_FROM_DB);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (viewedArticles.size() != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DB.getInstance().setViewed(viewedArticles);
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (AsyncTask unfinishedTask : unfinishedTasks) {
            unfinishedTask.cancel(true);
            ((Observable) unfinishedTask).removeAllObservers();
        }
    }

    private void setProcessAnimation(boolean flag) {
        if (flag) {
            RotateAnimation animation = new RotateAnimation(0.0f, 360.0f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            animation.setDuration(ANIMATION_DURATION);
            animation.setRepeatCount(RotateAnimation.INFINITE);
            animation.setInterpolator(new LinearInterpolator());
            ((ImageView) findViewById(R.id.ivUpdate)).startAnimation(animation);
        } else {
            ((ImageView) findViewById(R.id.ivUpdate)).clearAnimation();
        }
    }
}