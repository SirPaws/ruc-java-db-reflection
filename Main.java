import Database.Annotations.*;
import Database.DataBase;
import Database.DataBaseTable;

import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static void print(String string) {
        System.out.print(string);
    }
    public static void println(String string) {
        System.out.println(string);
    }

    public static void main(String[] args) {
        DataBaseTable<Lecturer> lecturer_table = new DataBaseTable<>(Lecturer.class);
        DataBaseTable<LecturerList> lecturer_list_table = new DataBaseTable<>(LecturerList.class);

        DataBaseTable<Room> room_table = new DataBaseTable<>(Room.class);
        DataBaseTable<RoomList> room_list_table = new DataBaseTable<>(RoomList.class);

        DataBaseTable<TimeTable> time_table_table = new DataBaseTable<>(TimeTable.class);
        DataBaseTable<TimeSlot> time_slot_table = new DataBaseTable<>(TimeSlot.class);
        DataBaseTable<Course> course_table = new DataBaseTable<>(Course.class);

        try {
            println(lecturer_table.createTable());
            println(lecturer_list_table.createTable());
            println(room_table.createTable());
            println(room_list_table.createTable());
            println(course_table.createTable());
            println(time_slot_table.createTable());
            println(time_table_table.createTable());
            println(lecturer_table.insert(new Lecturer(1, "Geralt of Rivia")));
            println(lecturer_table.insert(new Lecturer(2, "Ulfric Stormcloak")));

            HashMap<Class<?>, Integer> refs = new HashMap<>();
            refs.put(Lecturer.class, 1);
            println(lecturer_list_table.insert(new LecturerList(0), refs));
            refs.put(Lecturer.class, 2);
            println(lecturer_list_table.insert(new LecturerList(0), refs));
            refs.clear();

            println(room_table.insert(new Room(1, "4.206.9", 420), null));
            println(room_table.insert(new Room(2, "10.42.1", 420), null));
            refs.put(Room.class, 1);
            println(room_list_table.insert(new RoomList(0), refs));
            refs.put(Room.class, 2);
            println(room_list_table.insert(new RoomList(0), refs));
            refs.clear();

            refs.put(RoomList.class, 0);
            refs.put(LecturerList.class, 0);
            println(course_table.insert(new Course(1, "SD", 120), refs));
            refs.clear();

            refs.put(Course.class, 1);
            println(time_slot_table.insert(new TimeSlot(1), refs));
            refs.clear();

            refs.put(TimeSlot.class, 1);
            println(time_table_table.insert(new TimeTable(1), refs));

            /*
            DataBase db = new DataBase();
            db.dropNewTables(true);

            db.addTable(lecturer_list_table);
            db.addTable(lecturer_table);
            db.addTable(room_table);
            db.addTable(room_list_table);
            db.addTable(course_table);
            db.addTable(time_slot_table);
            db.addTable(time_table_table);

            db.insert(new Lecturer(1, "Geralt of Rivia"), null);
            db.insert(new Lecturer(2, "Ulfric Stormcloak"), null);

            HashMap<Class<?>, Integer> refs = new HashMap<>();
            refs.put(Lecturer.class, 1);
            db.insert(new LecturerList(0), refs);
            refs.put(Lecturer.class, 2);
            db.insert(new LecturerList(0), refs);
            refs.clear();

            db.insert(new Room(1, "4.206.9", 420), null);
            db.insert(new Room(2, "10.42.1", 420), null);
            refs.put(Room.class, 1);
            db.insert(new RoomList(0), refs);
            refs.put(Room.class, 2);
            db.insert(new RoomList(0), refs);
            refs.clear();

            refs.put(RoomList.class, 0);
            refs.put(LecturerList.class, 0);
            db.insert(new Course(1, "SD", 120), refs);
            refs.clear();

            refs.put(Course.class, 1);
            db.insert(new TimeSlot(1), refs);
            refs.clear();

            refs.put(TimeSlot.class, 1);
            db.insert(new TimeTable(1), refs);

            db.update(new Room(2, "10.42.1", 50), null);

            ArrayList<TimeTable> tables = db.select(TimeTable.class);
            for (TimeTable t: tables) {
                println("time table: " + t.time);
                Course c = t.slot.course;
                println("    course: " + c.name);
                println("        expected students: " + c.expected_student_count);
                println("        lecturers: ");
                for (Lecturer l: c.lecturers) {
                    print("            ");
                    println(l.id + ": " + l.name + "(" + l.email + ")");
                }
                println("        rooms: ");
                for (Room r: c.rooms) {
                    print("            ");
                    println(r.id + ": " + r.name + "(" + r.capacity + ")");
                }
            }
             */

        } catch(InvalidClassException ignored) {
        }
    }
}


class TimeTable {
    public TimeTable() {}
    public TimeTable(Integer time_) { time = time_; }
    // 0-9
    Integer time;
    @NotNull @Reference(TimeSlot.class)
    TimeSlot slot;
}

class TimeSlot {
    public TimeSlot() {}
    public TimeSlot(Integer id_) { id = id_; }
    @PrimaryKey Integer id;
    @NotNull @Reference(Course.class)
    Course course;
}

class Course {

    public Course() {}
    public Course(Integer id_, String name_, Integer num_students) {
        id = id_;
        name = name_;
        expected_student_count = num_students;
    }

    @PrimaryKey Integer id;

    @NotNull
    String name;

    @NotNull
    Integer expected_student_count;

    @NotNull @Array(RoomList.class)
    Room[] rooms;

    @NotNull @Array(LecturerList.class)
    Lecturer[] lecturers;
}


/*
create table Lecturer (
    lecturer_id integer primary key autoincrement not null,
    name text not null,
    email text
);

create table Rooms (
    room_id integer primary key autoincrement not null,
    name text not null,
    capacity int not null
);

create table TimeSlotValue (
    timeslot_value_id integer primary key autoincrement not null,
    room_key int,
    lecturer_key int,
    foreign key(room_key) references Rooms(room_key),
    foreign key(lecturer_key) references Lecturer(lecturer_key)
);
*/

class Room {
    public Room() {}
    public Room(Integer _id, String _name, Integer _capacity) {
        id       = _id;
        name     = _name;
        capacity = _capacity;
    }
    @NotNull
    @PrimaryKey
    Integer id;

    @NotNull
    String name;

    @NotNull
    Integer capacity;
}

class Lecturer {
    public Lecturer() { }
    public Lecturer(Integer _id, String _name, String _email) {
        id    = _id;
        name  = _name;
        email = _email;
    }
    public Lecturer(Integer _id, String _name) {
        id    = _id;
        name  = _name;
    }
    @NotNull
    @PrimaryKey
    Integer id;

    @NotNull
    String name;

    @Optional
    String email;
}

class LecturerList {
    public LecturerList() {}
    public LecturerList(Integer i) { id = i; }
    @NotNull @Key
    Integer id;
    @Value
    Lecturer value;
}

class RoomList {
    public RoomList() {}
    public RoomList(Integer i) { id = i; }
    @NotNull @Key
    Integer id;
    @Value
    Room value;
}

class Slot {
    public Slot() { }
    public Slot(Integer _id) { id = _id; }

    @NotNull @PrimaryKey
    Integer id;

    // @Reference(Room.class)
    // Room room;
    @Array(RoomList.class)
    Room[] rooms;

    @Reference(Lecturer.class)
    Lecturer lecturer;
}

