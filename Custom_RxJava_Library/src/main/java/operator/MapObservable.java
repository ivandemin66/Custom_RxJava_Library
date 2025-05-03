package operator;

import core.Disposable;
import core.Observable;
import core.Observer;

import java.util.function.Function;

public final class MapObservable<T, R> extends Observable<R> {

    public MapObservable(Observable<T> upstream, Function<? super T, ? extends R> mapper) {
        super(observer -> upstream.subscribe(new Observer<T>() {
            @Override public void onSubscribe(Disposable d) { observer.onSubscribe(d); }
            @Override public void onNext(T item) { observer.onNext(mapper.apply(item)); }
            @Override public void onError(Throwable t) { observer.onError(t); }
            @Override public void onComplete() { observer.onComplete(); }
        }));
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) throws Exception {

    }
}