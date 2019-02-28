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

				g.setStroke(new BasicStroke(50));
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

		Stack<ItemPosition> static_obs = app.static_obs;
		Iterator<ItemPosition> iter;
		if(static_obs != null) {
			for (int i = static_obs.size() - 1; i > 0; i--) {
				g.setColor(Color.BLACK);
				g.setStroke(new BasicStroke(200));
				g.drawLine(static_obs.get(i).x, static_obs.get(i).y, static_obs.get(i - 1).x, static_obs.get(i - 1).y);
			}
		}


		g.setColor(Color.RED);
		for(ItemPosition dest : app.destinations.values()) {
			g.fillRect(dest.getX(), dest.getY(), 60, 60);
		}

	}

}


