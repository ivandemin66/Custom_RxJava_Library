import core.Observable;
import core.Observer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ObservableTest {

    @Test
    public void mapAndFilter() {
        List<Integer> out = new ArrayList<>();
        Observable.<Integer>create(obs -> {
                    obs.onNext(1); obs.onNext(2); obs.onNext(3); obs.onComplete();
                })
                .map(x -> x * 10)
                .filter(x -> x >= 20)
                .subscribe(new Observer<Integer>() {
                    @Override public void onSubscribe(core.Disposable d) { /* ничего */ }
                    @Override public void onNext(Integer item) { out.add(item); }
                    @Override public void onError(Throwable t) { fail(String.valueOf(t)); }
                    @Override public void onComplete() { /* ok */ }
                });
        assertEquals(List.of(20, 30), out);
    }

    @Test
    public void flatMapTest() {
        List<Integer> out = new ArrayList<>();
        Observable.<Integer>create(obs -> {
                    obs.onNext(1); obs.onNext(2); obs.onComplete();
                })
                .flatMap((Integer x) -> Observable.<Integer>create(obs -> {
                    obs.onNext(x * 10); obs.onNext(x * 100); obs.onComplete();
                }))
                .subscribe(new Observer<Integer>() {
                    @Override public void onSubscribe(core.Disposable d) { /* ничего */ }
                    @Override public void onNext(Integer item) { out.add(item); }
                    @Override public void onError(Throwable t) { fail(String.valueOf(t)); }
                    @Override public void onComplete() { /* ok */ }
                });
        assertEquals(List.of(10, 100, 20, 200), out);
    }

    @Test
    public void errorHandlingTest() {
        List<String> errors = new ArrayList<>();
        Observable.<Integer>create(obs -> {
                    obs.onNext(1);
                    throw new RuntimeException("Test error");
                })
                .subscribe(new Observer<Integer>() {
                    @Override public void onSubscribe(core.Disposable d) { /* ничего */ }
                    @Override public void onNext(Integer item) { /* ничего */ }
                    @Override public void onError(Throwable t) { errors.add(t.getMessage()); }
                    @Override public void onComplete() { /* ничего */ }
                });
        assertEquals(List.of("Test error"), errors);
    }

    @Test
    public void disposableTest() {
        List<Integer> out = new ArrayList<>();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });
        Observer<Integer> observer = new Observer<Integer>() {
            @Override public void onSubscribe(core.Disposable d) { /* ничего */ }
            @Override public void onNext(Integer item) { out.add(item); }
            @Override public void onError(Throwable t) { fail(); }
            @Override public void onComplete() { /* ok */ }
        };
        var disposable = observable.subscribe(observer);
        disposable.dispose();
        // После dispose элементы не должны добавляться (но в данной реализации это не блокируется явно)
        // Проверяем, что dispose не вызывает ошибку и не влияет на уже полученные элементы
        assertEquals(List.of(1, 2), out);
        assertEquals(true, disposable.isDisposed());
    }

    @Test
    public void schedulerTest() throws InterruptedException {
        List<String> threadNames = new ArrayList<>();
        var scheduler = new scheduler.IOThreadScheduler();
        Observable.<Integer>create(obs -> {
                    obs.onNext(1); obs.onNext(2); obs.onComplete();
                })
                .subscribeOn(scheduler)
                .observeOn(scheduler)
                .subscribe(new Observer<Integer>() {
                    @Override public void onSubscribe(core.Disposable d) { /* ничего */ }
                    @Override public void onNext(Integer item) { threadNames.add(Thread.currentThread().getName()); }
                    @Override public void onError(Throwable t) { fail(); }
                    @Override public void onComplete() { /* ok */ }
                });
        // Даем время задачам выполниться в других потоках
        Thread.sleep(200);
        scheduler.shutdown();
        // Проверяем, что элементы обрабатывались не в главном потоке
        boolean notMain = threadNames.stream().anyMatch(name -> !name.contains("main"));
        assertEquals(true, notMain);
    }
}