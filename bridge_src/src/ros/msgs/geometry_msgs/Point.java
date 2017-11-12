package ros.msgs.geometry_msgs;

/**
 * A Java Bean for the Vector3 ROS geometry_msgs/Vector3 message type. This can be used both for publishing Vector3 messages to
 * {@link ros.RosBridge} and unpacking Vector3 messages received from {@link ros.RosBridge} (see the {@link ros.tools.MessageUnpacker}
 * documentation for how to easily unpack a ROS Bridge message into a Java object).
 * @author James MacGlashan.
 */
public class Point {
	public double x;
	public double y;
	public double z;

	public Point(){}

	public Point(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
