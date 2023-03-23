package Database.Annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {
    public Class<?> value();
}
