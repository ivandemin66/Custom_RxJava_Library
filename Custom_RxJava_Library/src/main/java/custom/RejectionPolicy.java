package custom;

public enum RejectionPolicy {
    ABORT,           // бросаем RejectedExecutionException
    CALLER_RUNS,     // выполняем в вызывающем потоке
    DISCARD_OLDEST,  // удаляем старую задачу, вставляем новую
    DISCARD          // тихо отбрасываем новую задачу
}