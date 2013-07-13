package ru.oren.RssReader.parser;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import ru.oren.RssReader.entities.Article;
import ru.oren.RssReader.utils.NetworkUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class RssParser {
    private final String ENCODING = "windows-1251";
    private final String RSS_FEED = "http://www.oren.ru/rss/";
    private final String ITEM_TAG = "item";
    private final String URL_ATTR = "url";

    public ArrayList<Article> parse(Date lastArticleDate) {
        ArrayList<Article> articles = new ArrayList<Article>();
        XmlPullParser parser = Xml.newPullParser();
        try {
            URL rssUrl = new URL(RSS_FEED);
            InputStream is = rssUrl.openConnection().getInputStream();
            InputStreamReader reader = new InputStreamReader(is, ENCODING);
            parser.setInput(reader);

            boolean done = false;
            int eventType = parser.getEventType();
            Article currentArticle = null;
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                String tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equals(ITEM_TAG)) {
                            currentArticle = new Article();
                        } else if (currentArticle == null) {
                            break;
                        } else if (tagName.equals(Article.TITLE)) {
                            currentArticle.setTitle(parser.nextText());
                        } else if (tagName.equals(Article.DESCRIPTION)) {
                            currentArticle.setDescription(parser.nextText());
                        } else if (tagName.equals(Article.IMAGE)) {
                            currentArticle.setImage(NetworkUtil.downloadImage(parser.getAttributeValue("", URL_ATTR)));
                        } else if (tagName.equals(Article.TEXT)) {
                            currentArticle.setText(parser.nextText());
                        } else if (tagName.equals(Article.DATE)) {
                            currentArticle.setDateStringRFC(parser.nextText());

                            done = (lastArticleDate != null) && (currentArticle.getDate().compareTo(lastArticleDate) <= 0);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equals(ITEM_TAG)) {
                            articles.add(currentArticle);
                        }
                        break;
                }

                eventType = parser.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return articles;
    }
}