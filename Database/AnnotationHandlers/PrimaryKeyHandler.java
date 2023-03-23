package Database.AnnotationHandlers;

import java.lang.annotation.Annotation;

public class PrimaryKeyHandler implements AnnotationHandler {
    @Override
    public void annotate(StringBuilder builder, Annotation self) {
        builder.append("primary key");
    }
}
