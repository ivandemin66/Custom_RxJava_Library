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
                .subscribe(new Observer<>() {
                    @Override public void onNext(Integer item) { out.add(item); }
                    @Override public void onError(Throwable t) { fail(String.valueOf(t)); }
                    @Override public void onComplete() { /* ok */ }
                });
        assertEquals(List.of(20, 30), out);
    }
}