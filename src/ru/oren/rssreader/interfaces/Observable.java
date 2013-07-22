package ru.oren.rssreader.interfaces;

public interface Observable {
    public void addObserver(Object object);

    public void removeAllObservers();
}
