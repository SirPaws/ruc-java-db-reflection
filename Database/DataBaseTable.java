package Database;

import Database.Annotations.*;

import java.io.InvalidClassException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DataBaseTable<T> {
    private final Class<T> class_;

    public DataBaseTable(Class<T> c) {
        class_ = c;
    }

    public boolean isTypeSupported(Class<?> t) {
        // if (t.isArray()) return isTypeSupported(t.arrayType());
        if (t.isArray()) return false;

        if (t == Integer.class) return true;
        if (t == int.class) return true;
        return t == String.class;
    }

    public <ExpectedType>
    boolean is(Class<ExpectedType> c) {
        return c.equals(class_);
    }

    public String createTable() throws InvalidClassException {
        if (class_ == null) return "";

        String header = "create table if not exists ";
        StringBuilder statement = new StringBuilder(header + class_.getSimpleName() + "(\n");
        List<Field> fields = List.of(class_.getDeclaredFields());
        if (isTableArray()) {
            Pair<Field, Field> arr = getArrayKeyAndValue(class_);
            statement.append("    ");
            statement.append(createField(arr.first, getFieldAnnotations(arr.first)));
            statement.append(",\n");

            List<FieldAnnotation> annotations = getFieldAnnotations(arr.second);
            FieldAnnotation value_annotation = null;
            for (FieldAnnotation annotation: annotations) {
                if (annotation.annotation instanceof Value) {
                    value_annotation = annotation;
                    break;
                }
            }
            assert value_annotation != null;

            StringBuilder arr_builder = new StringBuilder();
            arr_builder.append(getFieldName(arr.second));
            String name = arr_builder.toString();

            arr_builder.append("_key");
            arr_builder.append(" ");
            arr_builder.append("Integer,\n");

            Class<?> class_ = arr.second.getType();
            String class_name = class_.getSimpleName();
            arr_builder.append("    ");
            arr_builder.append("foreign key")
                    .append(" (")
                    .append(name)
                    .append("_key) references ")
                    .append(class_name)
                    .append(" (")
                    .append(name)
                    .append("_key)");
            statement.append("    ");
            statement.append(arr_builder);

            statement.append('\n');
            statement.append(')');
            return statement.toString();
        }


        ArrayList<FieldAnnotation> all_annotations = new ArrayList<>();
        for (Field f : fields) {
            List<FieldAnnotation> annotations = getFieldAnnotations(f);
            String field = createField(f, annotations);
            statement.append("    ").append(field);
            if (fields.indexOf(f) != fields.size() - 1)
                statement.append(",\n");

            all_annotations.addAll(annotations);
        }

        StringBuilder db_late  = new StringBuilder();
        int old_length = 0;
        for (FieldAnnotation annotation: all_annotations) {
            annotation.handler.lateAnnotate(db_late, annotation.annotation);
            if (db_late.length() != old_length) {
                db_late.insert(old_length, ",\n    ");
                old_length = db_late.length();
            }
        }

        String late = db_late.toString();
        if (late.length() != 0) {
            statement.append(late);
        }
        statement.append('\n');
        statement.append(')');
        return statement.toString();
    }

    public String dropTable() {
        if (class_ == null) return "";
        return "drop table if exists " + class_.getSimpleName();
    }

    public String insert(T object) {
        return insert(object, null);
    }

    public String insert(T object, Map<Class<?>, Integer> references) {
        if (class_ == null) return "";

        // insert into #ClassName (#ColumNames...) values ( ... values );
        StringBuilder builder = new StringBuilder("insert into ");
        builder.append(class_.getSimpleName());
        builder.append('(');

        List<Field> fields = List.of(class_.getDeclaredFields());

        for (Field f : fields) {
            if (isReference(f) || isArrayValue(f) || isArray(f))
                 builder.append(getFieldName(f)).append("_key");
            else builder.append(getFieldName(f));
            if (fields.indexOf(f) != fields.size() - 1)
                builder.append(", ");
        }



        builder.append(") values (");
        for (Field f : fields) {
            if (isReference(f) || isArrayValue(f)) {
                Integer id = references.get(f.getType());
                assert id != null;

                builder.append(id);
                if (fields.indexOf(f) != fields.size() - 1)
                    builder.append(", ");
                continue;
            }
            else if (isArray(f)) {
                FieldAnnotation arr = getArrayAnnotation(f);
                Database.Annotations.Array annotation = (Database.Annotations.Array)arr.annotation;
                Integer id = references.get(annotation.value());

                builder.append(id);
                if (fields.indexOf(f) != fields.size() - 1)
                    builder.append(", ");
                continue;
            }


            try {
                f.setAccessible(true);
                Object value = f.get(object);
                if (value instanceof String s)
                    builder .append('\'')
                            .append(s)
                            .append('\'');
                else if (value == null)
                    builder.append("null");
                else builder.append(value);
            } catch (IllegalAccessException ignored) {}
            if (fields.indexOf(f) != fields.size() - 1)
                builder.append(", ");
        }
        builder.append(")");

        return builder.toString();
    }

    public String getIDString(Class<?> class_) {
        Field f = getID(class_);
        return f == null ? null : getFieldName(class_, f);
    }

    Object getFieldValue(ResultSet rs, Class<?> field_type, String name) throws SQLException {
        if (field_type == Integer.class || field_type == int.class)
            return rs.getInt(name);
        else if (field_type == String.class)
            return rs.getString(name);
        throw new Error("unsupported type!");
    }

    public ArrayList<T> select(DataBase db) {
        ArrayList<T> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder("select * from ");

        boolean has_reference = false;
        int offset = builder.length();
        List<Field> fields = List.of(class_.getDeclaredFields());
        for (Field f : fields) {
            FieldAnnotation annotation;
            if ((annotation = getReferenceAnnotation(f)) != null) {
                if (!has_reference)
                    builder.append(class_.getSimpleName());
                builder.append(queryAppendReference(annotation, f));
                has_reference = true;
            }
        }

        if (!has_reference)
            builder.append(class_.getSimpleName());
        ResultSet rs = db.query(builder.toString());

        try {
            Constructor<T> constructor = class_.getConstructor();
            constructor.setAccessible(true);
            while (rs.next()) {
                boolean is_set = false;
                T value = constructor.newInstance();

                for (Field f : fields) {
                    handleFieldValue(db, rs, value, f);
                    is_set = true;
                }

                if (is_set) list.add(value);
            }
        } catch (SQLException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        if (list.size() == 0) return null;
        return list;
    }

    public String update(T object, Map<Class<?>, Integer> references) throws IllegalAccessException {
        // update Course set course_expected_student_count = 120 where course_id = 1
        StringBuilder builder = new StringBuilder();

        builder.append("update ");
        builder.append(class_.getSimpleName());
        builder.append(" set ");

        Field id_field = null;
        Field[] fields = class_.getDeclaredFields();
        for (Field field: fields) {
            if (isID(field)) {
                id_field = field;
                continue;
            }

            FieldAnnotation annotation = null;
            if ((annotation = getReferenceAnnotation(field)) != null) {
                Reference ref = (Reference)annotation.annotation;

                builder.append(getFieldName(field));
                builder.append(" = ");

                Integer value = references.get(ref.value());
                builder.append(value);
                if (field != fields[fields.length - 1])
                    builder.append(',');
                continue;
            }
            else if ((annotation = getArrayAnnotation(field)) != null) {
                Array ref = (Array)annotation.annotation;

                builder.append(getFieldName(field));
                builder.append(" = ");

                Integer value = references.get(ref.value());
                builder.append(value);
                if (field != fields[fields.length - 1])
                    builder.append(',');
                continue;
            }

            field.setAccessible(true);
            Object o = field.get(object);

            builder.append(getFieldName(field));
            builder.append(" = ");
            if (o instanceof String s)
                 builder.append("'").append(o).append("'");
            else builder.append(o);
            if (field != fields[fields.length - 1])
                builder.append(", ");
        }

        if (id_field != null) {
            builder.append(" where ");
            builder.append(getFieldName(id_field));
            builder.append(" == ");

            id_field.setAccessible(true);
            Object o = id_field.get(object);

            builder.append(o);
        }

        return builder.toString();
    }

    String queryAppendReference(FieldAnnotation reference, Field f) {
        return queryAppendReference(class_, reference, f);
    }

    String queryAppendReference(Class<?> class_, FieldAnnotation reference, Field f) {
        if (!(reference.annotation instanceof Reference ref)) throw new Error("wrong value passed into function");
        Class<?> ref_class = ref.value();

        StringBuilder builder = new StringBuilder();
        Field[] fields = ref_class.getDeclaredFields();
        for (Field ref_f: fields) {
            FieldAnnotation annotation;
            if ((annotation = getReferenceAnnotation(ref_f)) != null) {
                String s = queryAppendReference(ref_class, annotation, ref_f);
                builder.append(s);
            }
        }

        builder.append(" inner join ");
        builder.append(ref_class.getSimpleName());
        builder.append(" on ");
        builder.append(class_.getSimpleName());
        builder.append('.');
        builder.append(getFieldName(class_, f));
        builder.append("_key == ");
        builder.append(ref_class.getSimpleName());
        builder.append('.');
        builder.append(getIDString(ref.value()));
        return builder.toString();
    }

    String queryGenerateArray(FieldAnnotation array, Field f) {
        if (!(array.annotation instanceof Database.Annotations.Array ref)) throw new Error("wrong value passed into function");
        Class<?> arr_class = ref.value();
        Pair<Field, Field> array_pair = getArrayKeyAndValue(arr_class);

        StringBuilder builder = new StringBuilder();
        // select * from RoomList inner join Room on RoomList.roomlist_value_key == Room.room_id
        builder.append("select * from ");
        builder.append(arr_class.getSimpleName());
        builder.append(" inner join ");
        builder.append(array_pair.second.getType().getSimpleName());
        builder.append(" on ");
        builder.append(arr_class.getSimpleName());
        builder.append('.');
        builder.append(getFieldName(arr_class, array_pair.second));
        builder.append("_key");
        builder.append(" == ");
        builder.append(f.getType().getComponentType().getSimpleName());
        builder.append('.');
        builder.append(getIDString(f.getType().getComponentType()));
        return builder.toString();
    }

    private void handleFieldValue(DataBase db, ResultSet rs, Object value, Field f) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        handleFieldValue(db, rs, value, f, class_);
    }
    private void handleFieldValue(DataBase db, ResultSet rs, Object value, Field f, Class<?> class_) throws SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        FieldAnnotation annotation = null;
        if ((annotation = getReferenceAnnotation(f)) != null) {
            Reference ref = (Reference) annotation.annotation;
            Class<?> ref_class = ref.value();

            Constructor<?> constructor = ref_class.getConstructor();
            constructor.setAccessible(true);

            Object o = constructor.newInstance();
            for (Field ref_field : ref_class.getDeclaredFields())
                handleFieldValue(db, rs, o, ref_field, ref_class);

            f.setAccessible(true);
            f.set(value, o);
            return;
        }
        else if ((annotation = getArrayAnnotation(f)) != null) {
            Database.Annotations.Array ref = (Database.Annotations.Array) annotation.annotation;
            Pair<Field, Field> array = getArrayKeyAndValue(ref.value());
            Class<?> field_type = f.getType().componentType();
            Constructor<?> constructor = field_type.getConstructor();
            constructor.setAccessible(true);

            ArrayList<Object> list = new ArrayList<>();
            String query = queryGenerateArray(annotation, f);
            ResultSet array_result = db.query(query);

            while (array_result.next()) {
                Object o = constructor.newInstance();
                for (Field ref_field : field_type.getDeclaredFields()) {
                    handleFieldValue(db, array_result, o, ref_field, field_type);
                }
                list.add(field_type.cast(o));
            }

            Object arr = java.lang.reflect.Array.newInstance(field_type, list.size());
            for (int i = 0; i < java.lang.reflect.Array.getLength(arr); i++) {
                java.lang.reflect.Array.set(arr, i, list.get(i));
            }
            f.setAccessible(true);
            f.set(value, arr);
            return;
        }
        /*
        else if (isArray(f)) {
            Integer key = rs.getInt(getFieldName(f) + "_key");

            ArrayList<Object> o = db.selectAllWithID(f.getArrayCollectionType(), key);
            fieldAppend(f, o);
            f.setAccessible(true);
            f.set(value, o);
            return;
        }
         */
        Object o = getFieldValue(rs, f.getType(), getFieldName(class_, f));
        f.setAccessible(true);
        f.set(value, o);
    }

    Field getID(Class<?> class_) {
        List<Field> fields = List.of(class_.getDeclaredFields());

        for (Field f : fields) {
            if (!isID(f)) continue;

            return f;
        }
        return null;
    }

    boolean isTableArray(Class<?> c) {
        boolean found_key = false;
        boolean found_value = false;

        Field[] fields = c.getDeclaredFields();
        for (Field f: fields) {
            List<FieldAnnotation> annotations = getFieldAnnotations(f);
            for (FieldAnnotation annotation: annotations) {
                if (!found_key && annotation.annotation instanceof Key) {
                    found_key = true;
                }
                else if (!found_value && annotation.annotation instanceof Value) {
                    found_value = true;
                }

                if (found_key && found_value) break;
            }

            if (found_key && found_value) break;
        }


        return found_key && found_value;
    }

    boolean isTableArray() {
        return isTableArray(class_);
    }

    String javaTypeToSQLType(Class<?> type) throws InvalidClassException {
        if (type.isArray()) throw new InvalidClassException("arrays are not supported yet");

        if (type == Integer.class) return "integer";
        if (type == int.class) return "int";
        if (type == String.class) return "text";
        throw new InvalidClassException("");
    }

    boolean typeHandled(List<FieldAnnotation> list) {
        for (FieldAnnotation annotation: list)
            if(annotation.handler.isTypeHandled())
                return true;
        return false;
    }

    FieldAnnotation getReferenceAnnotation(Field f) {
        List<FieldAnnotation> annotations = getFieldAnnotations(f);
        for (FieldAnnotation annotation: annotations) {
            if (annotation.annotation instanceof Reference)
                return annotation;
        }
        return null;
    }

    FieldAnnotation getArrayAnnotation(Field f) {
        List<FieldAnnotation> annotations = getFieldAnnotations(f);
        for (FieldAnnotation annotation: annotations) {
            if (annotation.annotation instanceof Database.Annotations.Array)
                return annotation;
        }
        return null;
    }

    Pair<Field, Field> getArrayKeyAndValue(Class<?> c) {
        Field key = null;
        Field value = null;

        Field[] fields = c.getDeclaredFields();
        for (Field f: fields) {
            List<FieldAnnotation> annotations = getFieldAnnotations(f);
            for (FieldAnnotation annotation: annotations) {
                if (key == null && annotation.annotation instanceof Key) {
                    key = f;
                }
                else if (value == null && annotation.annotation instanceof Value) {
                    value = f;
                }
            }

            // this is a check to see if both 'key' and 'value' have been set
            if (key != null && value != null) {
                break;
            }
        }

        if (key == null || value == null)
            throw new Error("expected class " + c.getSimpleName() + " to have two members, one with @Key and the other with @Value");

        return new Pair<>(key, value);
    }

    boolean isReference(Field f) {
        List<FieldAnnotation> annotations = getFieldAnnotations(f);
        for (FieldAnnotation annotation: annotations) {
            if (annotation.annotation instanceof Reference)
                return true;
        }
        return false;
    }

    boolean isArray(Field f) {
        List<FieldAnnotation> annotations = getFieldAnnotations(f);
        for (FieldAnnotation annotation: annotations) {
            if (annotation.annotation instanceof Database.Annotations.Array)
                return true;
        }
        return false;
    }

    boolean isArrayValue(Field f) {
        List<FieldAnnotation> annotations = getFieldAnnotations(f);
        for (FieldAnnotation annotation: annotations) {
            if (annotation.annotation instanceof Value)
                return true;
        }
        return false;
    }

    boolean isID(Field f) {
        List<FieldAnnotation> annotations = getFieldAnnotations(f);
        for (FieldAnnotation annotation: annotations) {
            if (annotation.annotation instanceof PrimaryKey)
                return true;
        }
        return false;
    }

    List<FieldAnnotation> getFieldAnnotations(Field f) {
        Annotation[] field_annotations = f.getDeclaredAnnotations();
        ArrayList<FieldAnnotation> unsorted_annotations = new ArrayList<>();

        for (Annotation field_annotation : field_annotations) {
            FieldAnnotation annotation = AnnotationRegistry.getKey(field_annotation.annotationType());
            unsorted_annotations.add(new FieldAnnotation(annotation, field_annotation));
        }

        return unsorted_annotations.stream().sorted().toList();
    }

    String getFieldName(Field f) {
        return getFieldName(class_, f);
    }
    String getFieldName(Class<?> class_, Field f) {
        final String class_name = class_.getName();

        String str = f.toString();
        int begin = str.indexOf(class_name);
        begin += class_name.length() + 1;
        String name = str.substring(begin);

        return (class_name + '_' + name).toLowerCase();
    }

    String createField(Field f, List<FieldAnnotation> annotations) throws InvalidClassException {
        String field_name = getFieldName(f);
        boolean type_handled = typeHandled(annotations);

        StringBuilder db_field = new StringBuilder(field_name);

        if (!type_handled) {
            Class<?> type = f.getType();
            if (!isTypeSupported(type))
                throw new InvalidClassException("");

            db_field.append(" ").append(javaTypeToSQLType(type));
        }

        for (FieldAnnotation annotation: annotations) {
            db_field.append(' ');
            annotation.handler.annotate(db_field, annotation.annotation);
        }

        return db_field.toString();
    }
}

class Pair<X, Y> {
    public X first;
    public Y second;
    Pair(X a, Y b) {
        first = a;
        second = b;
    }
}