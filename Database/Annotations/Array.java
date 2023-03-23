package Database.Annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Array {
    public Class<?> value();
}
