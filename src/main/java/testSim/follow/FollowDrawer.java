package testSim.follow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.*;

import edu.illinois.mitra.cyphyhouse.gvh.SimGlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import testSim.draw.Drawer;

public class FollowDrawer extends Drawer {

	private Stroke stroke = new BasicStroke(8);
	private Color selectColor = new Color(0,0,255,100);
	
	@Override
	public void draw(LogicThread lt, Graphics2D g) {
		FollowApp app = (FollowApp) lt;
		ItemPosition model = (ItemPosition)app.gvh.plat.getModel();

		/* Get robot path and display */
		Stack<ItemPosition> path = app.path;
		if(path != null) {
			if(!path.empty()) {
				if(((FollowApp) lt).blocked_path == true)
					g.setColor(Color.RED);
				else
					g.setColor(Color.BLUE);

				g.setStroke(new BasicStroke(20));
				//get point at the top of stack and draw a line from robot to it
				g.drawLine(path.peek().x, path.peek().y, model.x, model.y);

				//check if there are more points in the path, if there are, draw their path onto the screen
				if (path.size() > 1) {
					for (int i = path.size() - 1; i > 0; i--) {
						g.drawLine(path.get(i).x, path.get(i).y, path.get(i - 1).x, path.get(i - 1).y);
					}
				}
			}
		}

		g.setColor(Color.RED);
		for(ItemPosition dest : app.destinations.values()) {
			g.fillRect(dest.getX(), dest.getY(), 60, 60);
		}

	}

}


