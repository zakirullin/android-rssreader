package ru.oren.RssReader.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ru.oren.RssReader.R;
import ru.oren.RssReader.entities.Article;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {
    Context context;
    LayoutInflater lInflater;
    ArrayList<Article> articles = new ArrayList<Article>();

    public ListAdapter(Context context) {
        this.context = context;
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addArticles(ArrayList<Article> articles) {
        this.articles.addAll(0, articles);
    }

    public ArrayList<Article> getArticles() {
        return this.articles;
    }

    @Override
    public int getCount() {
        return articles.size();
    }

    @Override
    public Object getItem(int i) {
        return articles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return articles.get(i).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if (view == null) {
            view = lInflater.inflate(R.layout.list_item, parent, false);
        }

        Article article = articles.get(i);

        ((TextView) view.findViewById(R.id.tvTitle)).setText(Html.fromHtml(article.getTitle()));
        ((TextView) view.findViewById(R.id.tvDate)).setText(article.getDateString());
        ((ImageView) view.findViewById(R.id.ivImage)).setImageBitmap(article.getImage());
        boolean largeScreen = (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE;
        if (largeScreen) {
            ((TextView) view.findViewById(R.id.tvDescription)).setText(Html.fromHtml(article.getDescription()));
        }

        int typeface;
        if (article.isViewed()) {
            typeface = Typeface.ITALIC;
        } else {
            typeface = Typeface.BOLD_ITALIC;
        }
        ((TextView) view.findViewById(R.id.tvTitle)).setTypeface(null, typeface);

        return view;
    }
}
