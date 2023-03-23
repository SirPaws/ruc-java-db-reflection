package Database;

import java.io.InvalidClassException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DataBase {
    ArrayList<DataBaseTable<?>> tables = new ArrayList<>();
    Connection conn = null;
    boolean drop_on_new_table = false;

    public DataBase() {
        open();
    }

    public void open() {
        try {
            String url = "jdbc:sqlite:listdb.db";
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println("cannot open database: " + e.getMessage());
            if (conn != null) close();
        }
    }

    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.out.println("cannot close");
        }
        conn = null;
    }

    public void dropNewTables(boolean value) {
        drop_on_new_table = value;
    }
    public <T> void addTable(DataBaseTable<T> table) throws InvalidClassException {
        execute(table.dropTable() + ";");
        execute(table.createTable() + ";");
        tables.add(table);
    }

    public<T> void insert(T object, HashMap<Class<?>, Integer> refs) {
        DataBaseTable<T> table = this.find(object.getClass());
        if (table == null) throw new Error("No table with that type!");

        execute(table.insert(object, refs) + ";");
    }

    public<T> ArrayList<T> select(Class<T> class_) {
        DataBaseTable<T> table = this.find(class_);
        if (table == null) return null;

        return table.select(this);
    }

    public<T> void update(T object, HashMap<Class<?>, Integer> refs) {
        DataBaseTable<T> table = find(object.getClass());
        if (table == null) throw new Error("No table with that type!");

        try {
            execute(table.update(object, refs) + ";");
        } catch (IllegalAccessException ignored) {}
    }
    /*
    public <T>
    void insert(T value) throws InvalidClassException {
        DataBaseTable<T> table = this.find(value.getClass());
        if (table == null) throw new Error("No table with that type!");
        HashMap<Class<?>, Integer> refs = table.gatherReferences(this, value);
        execute(table.insert(value, refs));
    }
    */

    /*
    public<T> Integer firstIndexOf(T object) {
        DataBaseTable<T> table = this.find(object.getClass());
        if (table == null) throw new Error("No table with that type!");
    }
    */

    private <T>
    DataBaseTable<T> find(Class<?> class_) {
        for (DataBaseTable<?> table : tables) {
            if (table.is(class_)) {
                @SuppressWarnings("unchecked")
                DataBaseTable<T> result = (DataBaseTable<T>) table;
                return result;
            }
        }
        return null;
    }

    private void execute(String sql) {
        if (conn == null) open();
        if (conn == null) {
            System.out.println("No connection");
            return;
        }
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Error in statement " + sql);
            System.out.println(e.getMessage());
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.out.println("Error in statement " + sql);
            System.out.println(e.getMessage());
        }
    }

    public ResultSet query(String query) {
        ArrayList<Integer> res = new ArrayList<>();
        if (conn == null) open();
        if (conn == null) {
            System.out.println("No connection");
            return null;
        }
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = conn.prepareStatement(query).executeQuery();
        } catch (SQLException e) {
            System.out.println("Error in statement " + query);
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.out.println("Error in statement " + query);
        }
        return rs;
    }
}
