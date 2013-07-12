package ru.oren.RssReader.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import ru.oren.RssReader.R;
import ru.oren.RssReader.adapters.ListAdapter;
import ru.oren.RssReader.db.DB;
import ru.oren.RssReader.db.DBFetcher;
import ru.oren.RssReader.db.DBFetcherListener;
import ru.oren.RssReader.entities.Article;
import ru.oren.RssReader.parser.RssFetcher;
import ru.oren.RssReader.parser.RssFetcherListener;
import ru.oren.RssReader.utils.NetworkUtil;

import java.util.ArrayList;

public class ArticleListActivity extends Activity implements DBFetcherListener, RssFetcherListener {
    private final int TOAST_TIMEOUT = 3;
    private final int ANIMATION_DURATION = 700;

    private ListAdapter listAdapter;
    private boolean refreshEnabled = false;
    private ArrayList<AsyncTask> unfinishedTasks = new ArrayList<AsyncTask>();
    private ArrayList<Long> viewedArticles = new ArrayList<Long>();

    public void refreshList(View view) {
        if (!refreshEnabled) {
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Отсутствует подключение к сети Интернет", TOAST_TIMEOUT).show();
        } else {
            setProcessAnimation(true);
            this.refreshEnabled = false;

            RssFetcher rssFetcher = new RssFetcher();
            rssFetcher.addListener(this);
            rssFetcher.addListener(DB.getInstance());
            rssFetcher.execute();
            unfinishedTasks.add(rssFetcher);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                refreshList(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDBFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        ((TextView) findViewById(R.id.tvLoadArticlesMessage)).setVisibility(View.GONE);

        unfinishedTasks.remove(task);
        setProcessAnimation(false);

        if (articles.size() != 0) {
            this.listAdapter.addArticles(articles);
            this.listAdapter.notifyDataSetChanged();
        } else {
            ((TextView) findViewById(R.id.tvNoArticlesMessage)).setVisibility(View.VISIBLE);
        }

        this.refreshEnabled = true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void onRssFetchingFinished(ArrayList<Article> articles, AsyncTask task) {
        unfinishedTasks.remove(task);
        setProcessAnimation(false);

        if (articles.size() != 0) {
            this.listAdapter.addArticles(articles);
            this.listAdapter.notifyDataSetChanged();

            ((TextView) findViewById(R.id.tvNoArticlesMessage)).setVisibility(View.GONE);

            ((ListView) findViewById(R.id.lvArticles)).smoothScrollToPosition(0);
        } else {
            Toast.makeText(getApplicationContext(), "Новых новостей нет", TOAST_TIMEOUT).show();
        }

        this.refreshEnabled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_article_list);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);


        this.listAdapter = new ListAdapter(this);
        ((ListView) findViewById(R.id.lvArticles)).setAdapter(listAdapter);

        ((ListView) findViewById(R.id.lvArticles)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                viewedArticles.add(listAdapter.getItemId(index));
                ((Article) listAdapter.getItem(index)).setViewed(true);
                listAdapter.notifyDataSetChanged();

                Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                intent.putExtra(Article.TITLE, ((Article) listAdapter.getItem(index)).getTitle());
                intent.putExtra(Article.TEXT, ((Article) listAdapter.getItem(index)).getText());
                startActivity(intent);
            }
        });

        DB.getInstance().init(getApplicationContext());
        DBFetcher dbFetcher = new DBFetcher();
        dbFetcher.addListener(this);
        dbFetcher.execute();
        unfinishedTasks.add(dbFetcher);
        setProcessAnimation(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (AsyncTask unfinishedTask : unfinishedTasks) {
            unfinishedTask.cancel(true);
        }

        if (viewedArticles.size() != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DB.getInstance().setViewed(viewedArticles);
                }
            }).start();
        }
    }

    private void setProcessAnimation(boolean flag) {
        if (flag == true) {
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