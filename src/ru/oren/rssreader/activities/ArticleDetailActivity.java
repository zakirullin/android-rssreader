package ru.oren.rssreader.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;
import ru.oren.rssreader.R;
import ru.oren.rssreader.entities.Article;

public class ArticleDetailActivity extends Activity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_detail);
        setTitle(" " + getIntent().getStringExtra(Article.TITLE));

        ((TextView) findViewById(R.id.tvDetailText)).setText(Html.fromHtml(getIntent().getStringExtra(Article.TEXT)));
        ((TextView) findViewById(R.id.tvDetailText)).setMovementMethod(new ScrollingMovementMethod());
    }
}