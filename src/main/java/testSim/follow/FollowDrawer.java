package testSim.follow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

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

		g.setColor(Color.RED);
		for(ItemPosition dest : app.destinations.values()) {
			g.fillRect(dest.getX(), dest.getY(), 26, 26);
		}

		ItemPosition model = (ItemPosition)app.gvh.plat.getModel();
		if(app.currentDestination != null)
			g.drawLine(app.currentDestination.getX(), app.currentDestination.getY(), model.x, model.y);
		/*g.setColor(selectColor);
		g.setStroke(stroke);
		if(app.currentDestination != null)
			g.drawOval(app.currentDestination.getX() - 20, app.currentDestination.getY() - 20, 40, 40);*/
	}

}
