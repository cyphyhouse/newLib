package ros.msgs.std_msgs;


public class Waypoint {
	public String name;
	public int index;
	public long receivedTime;
	public int x;
	public int y;
	public int z;

	public Waypoint() {
	}

	public Waypoint(String name, int index, long receivedTime, int x, int y, int z) {
		this.name = name;
		this.index = index;
		this.receivedTime = receivedTime;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
