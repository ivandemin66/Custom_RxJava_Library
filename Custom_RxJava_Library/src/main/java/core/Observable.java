package core;

import operator.*;
import scheduler.Scheduler;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Базовый «холодный» поток данных. Не хранит состояния.
 *
 * @param <T> тип элементов потока
 */
public abstract class Observable<T> {

    /* ===== Интерфейсы и поля =========================================== */

    /**
     * Источник событий. Вызывается при подписке.
     */
    @FunctionalInterface
    public interface OnSubscribe<T> {
        void subscribe(Observer<? super T> observer) throws Exception;
    }

    /** Храним лямбду‑источник для дальнейших операторов. */
    protected final OnSubscribe<T> source;

    protected Observable(OnSubscribe<T> source) {
        this.source = source;
    }

    /* ===== Фабрика ====================================================== */

    /** Создать Observable напрямую из лямбды‑источника. */
    public static <T> Observable<T> create(OnSubscribe<T> source) {
        return new Observable<T>(source) {
            @Override
            protected void subscribeActual(Observer<? super T> observer) throws Exception {
                source.subscribe(observer);
            }
        };
    }

    /* ===== Публичная подписка ========================================== */

    /**
     * Подписка на поток.<br>
     * 1) Передаём Disposable.<br>
     * 2) Доверяем дальнейшую работу {@link #subscribeActual}.<br>
     *
     * @return Disposable для отмены
     */
    public Disposable subscribe(Observer<? super T> observer) {
        DisposableImpl disposable = new DisposableImpl();
        observer.onSubscribe(disposable);
        try {
            subscribeActual(observer);
        } catch (Throwable t) {
            observer.onError(t);
        }
        return disposable;
    }

    /** Реальная логика подписки. Переопределяется операторами. */
    protected abstract void subscribeActual(Observer<? super T> observer) throws Exception;

    /* ===== Операторы ==================================================== */

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return new MapObservable<>(this, mapper);
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return new FilterObservable<>(this, predicate);
    }

    public <R> Observable<R> flatMap(Function<? super T, Observable<R>> mapper) {
        return new FlatMapObservable<>(this, mapper);
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        return create(observer ->
                scheduler.execute(() -> Observable.this.subscribe(observer)));
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        return create(observer -> Observable.this.subscribe(new Observer<T>() {
            @Override public void onSubscribe(Disposable d) { observer.onSubscribe(d); }
            @Override public void onNext(T item) { scheduler.execute(() -> observer.onNext(item)); }
            @Override public void onError(Throwable t) { scheduler.execute(() -> observer.onError(t)); }
            @Override public void onComplete() { scheduler.execute(observer::onComplete); }
        }));
    }

    /* ===== Disposable внутренний ======================================= */

    /** Простейшая реализация Disposable. */
    private static final class DisposableImpl implements Disposable {
        private volatile boolean disposed;

        @Override public void dispose() { disposed = true; }
        @Override public boolean isDisposed() { return disposed; }
    }
}
