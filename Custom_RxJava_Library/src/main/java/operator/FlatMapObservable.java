package operator;

import core.Disposable;
import core.Observable;
import core.Observer;

import java.util.function.Function;

public final class FlatMapObservable<T, R> extends Observable<R> {

    public FlatMapObservable(Observable<T> upstream, Function<? super T, Observable<R>> mapper) {
        super(observer -> upstream.subscribe(new Observer<T>() {
            @Override public void onSubscribe(Disposable d) { observer.onSubscribe(d); }
            @Override public void onNext(T item) {
                mapper.apply(item).subscribe(new Observer<R>() {
                    @Override public void onSubscribe(Disposable d) { /* ничего */ }
                    @Override public void onNext(R r) { observer.onNext(r); }
                    @Override public void onError(Throwable t) { observer.onError(t); }
                    @Override public void onComplete() { /* дочерний поток молча завершается */ }
                });
            }
            @Override public void onError(Throwable t) { observer.onError(t); }
            @Override public void onComplete() { observer.onComplete(); }
        }));
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) throws Exception {

    }
}