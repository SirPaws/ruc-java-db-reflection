package Database.AnnotationHandlers;

import java.lang.annotation.Annotation;

public class NotNullHandler implements AnnotationHandler {
    @Override
    public void annotate(StringBuilder builder, Annotation self) {
        builder.append("not null");
    }
}
