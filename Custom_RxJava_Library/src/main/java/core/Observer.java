package core;

/** Получатель элементов потока (аналог Observer в RxJava). */
public interface Observer<T> {

    /** Подписка получена — передаём Disposable. */
    default void onSubscribe(Disposable d) { }

    /** Элемент данных. */
    void onNext(T item);

    /** Ошибка в потоке. */
    void onError(Throwable t);

    /** Поток корректно завершён. */
    void onComplete();
}
