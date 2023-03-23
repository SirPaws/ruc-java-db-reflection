# ruc-java-db-reflection
a shitty sql thingy I overengineered for a uni assignment

# **Please don't use this, it was made for fun**
if you do want to create something from this go ahead
I won't even put a license on it, it's publicly stealable (CC0 i guess ¯\_(ツ)_/¯)

anyways here's an example of what it can do
```java
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
```check the `Main.java` file for more examples
