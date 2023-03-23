package Database.AnnotationHandlers;

import Database.Annotations.Value;

import java.lang.annotation.Annotation;

public class ValueHandler implements AnnotationHandler {
    @Override
    public void annotate(StringBuilder builder, Annotation self_) {
        if (!(self_ instanceof Value self)) throw new Error();

    }
}
