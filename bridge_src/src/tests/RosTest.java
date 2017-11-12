package tests;

import com.fasterxml.jackson.databind.JsonNode;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.std_msgs.PrimitiveMsg;
import ros.msgs.sensor_msgs.LaserScan;
import ros.tools.MessageUnpacker;

/**
 * Example of connecting to rosbridge with publish/subscribe messages. Takes one argument:
 * the rosbridge websocket URI; for example: ws://localhost:9090.
 * @author James MacGlashan.
 */
public class RosTest {

	public static void main(String[] args) {
		/*Header header = new Header();
		LaserScan scan = new LaserScan(header,1,1,1,1,1,1,1,1,1);
		System.out.println(scan);
		return;		*/

		if(args.length != 1){
			System.out.println("Need the rosbridge websocket URI provided as argument. For example:\n\tws://localhost:9090");
			System.exit(0);
		}

		RosBridge bridge = new RosBridge();
		bridge.connect(args[0], true);

		bridge.subscribe(SubscriptionRequestMsg.generate("/scan")
					.setType("sensor_msgs/LaserScan")
					.setThrottleRate(1)
					.setQueueLength(1),
				new RosListenDelegate() {

					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<PrimitiveMsg<String>> unpacker = new MessageUnpacker<PrimitiveMsg<String>>(PrimitiveMsg.class);
						PrimitiveMsg<String> msg = unpacker.unpackRosMessage(data);
						System.out.println(msg.data);
					}
				}
		);

		Publisher pub = new Publisher("/java_to_ros", "std_msgs/String", bridge);

		for(int i = 0; i < 100; i++) {
			pub.publish(new PrimitiveMsg<String>("hello from java " + i));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
