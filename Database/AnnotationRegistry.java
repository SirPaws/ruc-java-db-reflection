package Database;

import Database.AnnotationHandlers.*;
import Database.Annotations.*;

import java.lang.annotation.Annotation;
import java.util.HashMap;

public class AnnotationRegistry {
    private static HashMap<Class<? extends Annotation>, FieldAnnotation> map = new HashMap<>();

    public static final FieldAnnotation primaryKey = registerAnnotation(PrimaryKey.class, new PrimaryKeyHandler(), Integer.MIN_VALUE);
    public static final FieldAnnotation notNull    = registerAnnotation(NotNull.class,    new NotNullHandler(),    Integer.MAX_VALUE);
    public static final FieldAnnotation optional   = registerAnnotation(Optional.class,   new OptionalHandler(),   Integer.MAX_VALUE);
    public static final FieldAnnotation reference  = registerAnnotation(Reference.class,  new ReferenceHandler(),  Integer.MIN_VALUE);
    public static final FieldAnnotation array      = registerAnnotation(Array.class,      new ArrayHandler(),      Integer.MIN_VALUE);
    public static final FieldAnnotation key        = registerAnnotation(Key.class,        new KeyHandler(),        Integer.MAX_VALUE);
    public static final FieldAnnotation value      = registerAnnotation(Value.class,      new ValueHandler(),      Integer.MAX_VALUE);

    public static FieldAnnotation getKey(Class<? extends Annotation> annotation) {
        return map.get(annotation);
    }

    private static FieldAnnotation registerAnnotation(Class<? extends Annotation> annotation, AnnotationHandler handler, Integer value) {
        if (map == null) map = new HashMap<>();
        map.put(annotation, new FieldAnnotation(annotation, handler, value));
        return map.get(annotation);
    }
}
