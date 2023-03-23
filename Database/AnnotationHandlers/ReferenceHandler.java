package Database.AnnotationHandlers;

import Database.Annotations.PrimaryKey;
import Database.Annotations.Reference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ReferenceHandler implements AnnotationHandler {
    String name;
    @Override
    public boolean isTypeHandled() { return true; }

    boolean referenceHasPrimaryKey(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        boolean has_primary = false;
        for (Field field: fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation: annotations) {
                if (annotation instanceof PrimaryKey)
                    has_primary = true;
            }
        }
        return has_primary;
    }

    public void annotateReferenceLike(StringBuilder builder, Annotation self_) {
        name = builder.toString();
        int end = name.indexOf(' ');
        name = name.substring(0, end);
        builder.setLength(0);

        builder.append(name);
        builder.append("_key");
        builder.append(" ");
        builder.append("Integer");
    }

    public void lateAnnotateReferenceLike(StringBuilder builder, Class<?> class_) {
        String class_name = class_.getSimpleName();
        builder.append("foreign key")
                .append(" (")
                .append(name)
                .append("_key) references ")
                .append(class_name)
                .append(" (")
                .append(name)
                .append("_key)");
    }

    @Override
    public void annotate(StringBuilder builder, Annotation self_) {
        if (!(self_ instanceof Reference self)) throw new Error();
        annotateReferenceLike(builder, self_);
    }

    @Override
    public void lateAnnotate(StringBuilder builder, Annotation self_) {
        if (!(self_ instanceof Reference self)) throw new Error();

        Class<?> class_ = self.value();
        if (!referenceHasPrimaryKey(class_)) {
            String text = "Missing field with Primary Key in class " + class_.getSimpleName() + "\n";
            text += "\ta table has a reference to class: " + class_.getSimpleName() + "\n";
            text += "\tbut this class does not have a field with a primary key, which is required.\n";
            text += "\ta way this could be solved would be to add a field '@PrimaryKey Integer id'\n";
            text += "\tto the " + class_.getSimpleName() + " class.\n";
            throw new Error(text);
        }

        lateAnnotateReferenceLike(builder, class_);

    }
}
