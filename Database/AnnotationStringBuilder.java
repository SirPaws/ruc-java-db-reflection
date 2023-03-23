package Database;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationStringBuilder {
    ArrayList<FieldAnnotation> string = new ArrayList<>();

    public void push(Annotation annotation) {
        Class<? extends Annotation> type =  annotation.annotationType();
        FieldAnnotation key = AnnotationRegistry.getKey(type);
        string.add(key);
    }

    public String toString() {
        FieldAnnotation[] keys = string.toArray(new FieldAnnotation[0]);
        Arrays.sort(keys);
        List<FieldAnnotation> sorted_list = List.of(keys);

        ArrayList<String> string_list = new ArrayList<>();

        for (FieldAnnotation key: sorted_list) {
            // string_list.add(key.name);
        }

        return String.join(" ", string_list);
    }

}

