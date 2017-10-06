package ros.msgs.sensor_msgs;

import ros.msgs.std_msgs.Header;


public class LaserScan {

	public Header header;
	public float angle_min;
	public float angle_max;
	public float angle_increment;

	public float time_increment;
	
	public float scan_time;
	
	public float range_min;
	public float range_max;

	public float[] ranges;
	public float[] intensities;

	public LaserScan() {
	}

	public LaserScan(Header header, float angle_min, float angle_max, float angle_increment, float time_increment, float scan_time, float range_min, float range_max, float[] ranges, float[] intensities) {
		this.header = header;
		this.angle_min = angle_min;
		this.angle_max = angle_max;
		this.angle_increment = angle_increment;
		this.time_increment = time_increment;
		this.scan_time = scan_time;
		this.range_min = range_min;
		this.range_max = range_max;
		this.ranges = ranges;
		this.intensities = intensities;
	}
}

