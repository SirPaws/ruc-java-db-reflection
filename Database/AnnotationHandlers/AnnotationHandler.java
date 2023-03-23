package Database.AnnotationHandlers;

import java.lang.annotation.Annotation;

public interface AnnotationHandler {
    public default boolean isTypeHandled() { return false; }
    public default void annotate(StringBuilder builder, Annotation self) {  }
    public default void lateAnnotate(StringBuilder builder, Annotation self) { }
}
