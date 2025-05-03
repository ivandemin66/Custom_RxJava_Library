package operator;

import core.Disposable;
import core.Observable;
import core.Observer;

import java.util.function.Predicate;

public final class FilterObservable<T> extends Observable<T> {

    public FilterObservable(Observable<T> upstream, Predicate<? super T> predicate) {
        super(observer -> upstream.subscribe(new Observer<T>() {
            @Override public void onSubscribe(Disposable d) { observer.onSubscribe(d); }
            @Override public void onNext(T item) {
                if (predicate.test(item)) observer.onNext(item);
            }
            @Override public void onError(Throwable t) { observer.onError(t); }
            @Override public void onComplete() { observer.onComplete(); }
        }));
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) throws Exception {

    }
}