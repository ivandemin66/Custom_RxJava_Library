# Custom RxJava Library

Проект представляет собой облегченную реализацию библиотеки реактивного программирования, аналогичную RxJava, с дополнительными возможностями управления потоками выполнения.

## Структура проекта

Проект организован в следующие основные пакеты:

### core
Содержит базовые интерфейсы и классы реактивной архитектуры:
- `Observable` - главный класс, реализующий паттерн "наблюдатель", позволяет создавать цепочки операторов
- `Observer` - получатель данных из Observable
- `Disposable` - интерфейс для отмены подписки
- `OnSubscribe` - функциональный интерфейс, который вызывается при подписке

### operator
Реализует стандартные операторы для преобразования потоков данных:
- `MapObservable` - преобразует элементы потока с помощью заданной функции
- `FilterObservable` - отфильтровывает элементы потока по определенному предикату
- `FlatMapObservable` - трансформирует элементы потока в новые потоки и объединяет их

### scheduler
Предоставляет планировщики для выполнения операций в различных потоках:
- `Scheduler` - базовый интерфейс для выполнения задач
- `SingleThreadScheduler` - планировщик с одним рабочим потоком
- `ComputationScheduler` - планировщик для CPU-интенсивных операций
- `IOThreadScheduler` - планировщик для I/O операций

### custom
Содержит кастомную реализацию пула потоков с расширенными возможностями:
- `CustomThreadPoolExecutor` - пул потоков с настраиваемыми параметрами core/max, размером очереди, временем хранения и минимальным количеством запасных потоков
- `Worker` - рабочий поток, который выполняет задачи из очереди пула
- `CustomThreadFactory` - фабрика для создания рабочих потоков
- `RejectionPolicy` - перечисление политик отклонения задач при переполнении
- `CustomExecutor` - интерфейс для выполнения задач

## Особенности

1. **Гибкий пул потоков**
   - Настраиваемое количество ядерных и максимальных потоков
   - Управление временем жизни неактивных потоков
   - Различные стратегии отклонения задач

2. **Реактивный подход**
   - Холодные Observable (активируются при подписке)
   - Цепочки операторов для обработки данных
   - Механизм отмены подписки через Disposable

3. **Управление контекстом выполнения**
   - Операторы subscribeOn и observeOn для контроля потоков выполнения
   - Различные типы планировщиков для разных задач

## Пример использования

```java
// Создание и настройка пула потоков
CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor(
        2, 4, 5, TimeUnit.SECONDS,
        5, 1,
        RejectionPolicy.ABORT,
        new CustomThreadFactory("MyPool")
);

// Выполнение задач через пул
for (int i = 0; i < 10; i++) {
    int id = i;
    pool.execute(() -> {
        System.out.printf("[Task %d] started%n", id);
        // работа задачи
        System.out.printf("[Task %d] finished%n", id);
    });
}

// Завершение работы пула
pool.shutdown();
```

## Примеры использования Schedulers

```java
import scheduler.IOThreadScheduler;
import scheduler.ComputationScheduler;
import scheduler.SingleThreadScheduler;

// Использование IOThreadScheduler для асинхронной обработки
Observable.<Integer>create(obs -> {
        obs.onNext(1); obs.onNext(2); obs.onComplete();
    })
    .subscribeOn(new IOThreadScheduler())
    .observeOn(new ComputationScheduler())
    .map(x -> x * 2)
    .subscribe(new Observer<Integer>() {
        @Override public void onSubscribe(core.Disposable d) { }
        @Override public void onNext(Integer item) {
            System.out.println("[Computation] " + item + " в потоке: " + Thread.currentThread().getName());
        }
        @Override public void onError(Throwable t) { t.printStackTrace(); }
        @Override public void onComplete() { System.out.println("Завершено"); }
    });
```

## Тестирование

В проекте реализованы юнит-тесты для проверки всех ключевых компонентов:
- Проверка операторов map, filter, flatMap
- Проверка обработки ошибок (onError)
- Проверка работы Schedulers в многопоточной среде
- Проверка механизма отмены подписки (Disposable)

Пример теста на flatMap:
```java
@Test
public void flatMapTest() {
    List<Integer> out = new ArrayList<>();
    Observable.<Integer>create(obs -> {
                obs.onNext(1); obs.onNext(2); obs.onComplete();
            })
            .flatMap(x -> Observable.create(obs -> {
                obs.onNext(x * 10); obs.onNext(x * 100); obs.onComplete();
            }))
            .subscribe(new Observer<Integer>() {
                @Override public void onSubscribe(core.Disposable d) { }
                @Override public void onNext(Integer item) { out.add(item); }
                @Override public void onError(Throwable t) { fail(String.valueOf(t)); }
                @Override public void onComplete() { }
            });
    assertEquals(List.of(10, 100, 20, 200), out);
}
```

Пример теста на работу Schedulers:
```java
@Test
public void schedulerTest() throws InterruptedException {
    List<String> threadNames = new ArrayList<>();
    var scheduler = new IOThreadScheduler();
    Observable.<Integer>create(obs -> {
                obs.onNext(1); obs.onNext(2); obs.onComplete();
            })
            .subscribeOn(scheduler)
            .observeOn(scheduler)
            .subscribe(new Observer<Integer>() {
                @Override public void onSubscribe(core.Disposable d) { }
                @Override public void onNext(Integer item) { threadNames.add(Thread.currentThread().getName()); }
                @Override public void onError(Throwable t) { fail(); }
                @Override public void onComplete() { }
            });
    Thread.sleep(200);
    scheduler.shutdown();
    boolean notMain = threadNames.stream().anyMatch(name -> !name.contains("main"));
    assertEquals(true, notMain);
}
```

## Требования

- Java 11 или выше
- Maven для сборки проекта
