package Database;

import Database.AnnotationHandlers.AnnotationHandler;

import java.lang.annotation.Annotation;

public class FieldAnnotation implements Comparable<FieldAnnotation> {
    public Integer value;
    public Class<? extends Annotation> annotation_class;
    public AnnotationHandler handler;
    public Annotation annotation = null;

    FieldAnnotation(Class<? extends Annotation> annotation_class_, AnnotationHandler handler_, Integer value_) {
        value = value_;
        annotation_class = annotation_class_;
        handler = handler_;
    }

    FieldAnnotation(FieldAnnotation old, Annotation annotation_) {
        value = old.value;
        annotation_class = old.annotation_class;
        handler = old.handler;
        annotation = annotation_;
    }

    @Override
    public int compareTo(FieldAnnotation o) {
        return value.compareTo(o.value);
    }
}
