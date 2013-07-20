package ru.oren.RssReader.interfaces;

public interface Observable {
    public void addObserver(Object object);

    public void removeAllObservers();
}
