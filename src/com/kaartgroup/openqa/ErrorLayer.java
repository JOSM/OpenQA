package com.kaartgroup.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import org.openstreetmap.josm.actions.LassoModeAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.HighlightUpdateListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

import com.kaartgroup.openqa.profiles.GenericInformation;

public class ErrorLayer extends AbstractModifiableLayer implements MouseListener, DataSelectionListener, HighlightUpdateListener {
	/**
	 * Pattern to detect end of sentences followed by another one, or a link, in western script.
	 * Group 1 (capturing): period, interrogation mark, exclamation mark
	 * Group non capturing: at least one horizontal or vertical whitespace
	 * Group 2 (capturing): a letter (any script), or any punctuation
	 */
	private static final Pattern SENTENCE_MARKS_WESTERN = Pattern.compile("([\\.\\?\\!])(?:[\\h\\v]+)([\\p{L}\\p{Punct}])");

	/**
	 * Pattern to detect end of sentences followed by another one, or a link, in eastern script.
	 * Group 1 (capturing): ideographic full stop
	 * Group 2 (capturing): a letter (any script), or any punctuation
	 */
	private static final Pattern SENTENCE_MARKS_EASTERN = Pattern.compile("(\\u3002)([\\p{L}\\p{Punct}])");

	DataSet ds = new DataSet();
	private Node displayedNode;
	private JPanel displayedPanel;
	private JWindow displayedWindow;
	private PaintWindow window;

	ArrayList<Node> previousNodes;
	int nodeIndex = 0;

	final String CACHE_DIR;

	final GenericInformation type;

	/**
	 * Create a new ErrorLayer using a class that extends {@code GenericInformation}
	 * @param type A class that extends {@code GenericInformation}
	 */
	public ErrorLayer(GenericInformation type) {
		super(type.getLayerName());
		CACHE_DIR = type.getCacheDir();
		this.type = type;
		hookUpMapViewer();
	}

	/**
	 * Add this class to a map viewer. Usually called during initialization.
	 */
	public void hookUpMapViewer() {
		MainApplication.getMap().mapView.addMouseListener(this);
		ds.addHighlightUpdateListener(this);
		ds.addSelectionListener(this);
	}

	@Override
	public synchronized void destroy() {
		MainApplication.getMap().mapView.removeMouseListener(this);
		ds.removeHighlightUpdateListener(this);
		ds.removeSelectionListener(this);
		hideNodeWindow();
		super.destroy();
	}

	/**
	 * Add notes from a {@code DataSet}
	 * @param newDataSet {@code DataSet} with notes
	 * @return true if added
	 */
	public boolean addNotes(DataSet newDataSet) {
		ds.mergeFrom(newDataSet);
		return true;
	}

	@Override
	public boolean isModified() {
		return ds.isModified();
	}

	@Override
	public void paint(Graphics2D g, MapView mv, Bounds bbox) {
		if (window == null) {
			window = new PaintWindow(g, mv);
		} else {
			window.setGraphics2d(g);
			window.setMapView(mv);
		}
		window.run();
	}

	private class PaintWindow implements Runnable {
		Graphics2D g;
		MapView mv;

		public PaintWindow(Graphics2D g, MapView mv) {
			this.g = g;
			this.mv = mv;
		}

		public void setGraphics2d(Graphics2D g) {
			this.g = g;
		}

		public void setMapView(MapView mv) {
			this.mv = mv;
		}

		@Override
		public void run() {
			final ImageSizes size = ImageProvider.ImageSizes.LARGEICON;
			for (Node node : ds.getNodes()) {
				Point p = mv.getPoint(node.getCoor());
				String error = type.getError(node);
				ImageIcon icon = type.getIcon(error, size);
				int width = icon.getIconWidth();
				int height = icon.getIconHeight();
				g.drawImage(icon.getImage(), p.x - (width / 2), p.y - (height / 2), MainApplication.getMap().mapView);
			}
			createNodeWindow(g, mv, size);
		}

		private void createNodeWindow(Graphics2D g, MapView mv, ImageSizes size) {
			ArrayList<Node> selectedNodes = new ArrayList<>(ds.getSelectedNodes());
			selectedNodes.sort(null);
			MapMode mode = MainApplication.getMap().mapMode;
			if (!selectedNodes.isEmpty() && mode != null && (mode instanceof SelectAction || mode instanceof LassoModeAction)) {
				if ((selectedNodes.contains(displayedNode) && nodeIndex >= selectedNodes.size())
						|| !selectedNodes.contains(displayedNode)) {
					nodeIndex = 0;
				}

				Node selectedNode = selectedNodes.get(nodeIndex);

				final int iconHeight = size.getAdjustedHeight();
				final int iconWidth = size.getAdjustedWidth();
				paintSelectedNode(g, mv, iconHeight, iconWidth, selectedNode);
			} else {
				ds.clearSelection();
				nodeIndex = 0;
				hideNodeWindow();
			}
			previousNodes = selectedNodes;
		}

		/**
		 * Create the note window
		 * @param g The {@code Graphics2D} object that will be the note background
		 * @param mv The {@code MapView} object that we are drawing on
		 * @param iconHeight The height of the selection box that we are drawing
		 * @param iconWidth The width of the selection box we are drawing
		 * @param selectedNode The selected node to get information from
		 */
		private void paintSelectedNode(Graphics2D g, MapView mv, int iconHeight, int iconWidth, Node selectedNode) {
			Point p = mv.getPoint(selectedNode.getBBox().getCenter());
			g.setColor(ColorHelper.html2color(Config.getPref().get("color.selected")));
			g.drawRect(p.x - (iconWidth / 2), p.y - (iconHeight / 2), iconWidth - 1, iconHeight - 1);

			if (displayedNode != null && !displayedNode.equals(selectedNode)) {
				hideNodeWindow();
			}

			int xl = p.x - (iconWidth / 2) - 5;
			int xr = p.x + (iconWidth / 2) + 5;
			int yb = p.y - iconHeight - 1;
			int yt = p.y + (iconHeight / 2) + 2;
			Point pTooltip;

			String text = type.getNodeToolTip(selectedNode);
			JPanel actions = type.getActions(selectedNode);

			if (displayedWindow == null) {
				HtmlPanel htmlPanel = new HtmlPanel(text);
				htmlPanel.setBackground(UIManager.getColor("ToolTip.background"));
				htmlPanel.setForeground(UIManager.getColor("ToolTip.foreground"));
				htmlPanel.setFont(UIManager.getFont("ToolTip.font"));
				htmlPanel.setBorder(BorderFactory.createLineBorder(Color.black));
				htmlPanel.enableClickableHyperlinks();
				displayedPanel = new JPanel();
				displayedPanel.add(htmlPanel, GBC.eol());
				displayedPanel.add(actions, GBC.eol());
				pTooltip = fixPanelSizeAndLocation(mv, text, xl, xr, yt, yb);
				displayedWindow = new JWindow(MainApplication.getMainFrame());
				displayedWindow.setAutoRequestFocus(false);
				displayedWindow.add(displayedPanel);
				// Forward mouse wheel scroll event to MapMover
				displayedWindow.addMouseWheelListener(e -> mv.getMapMover().mouseWheelMoved(
						(MouseWheelEvent) SwingUtilities.convertMouseEvent(displayedWindow, e, mv)));
			} else {
				for (Component component : displayedPanel.getComponents()) {
					if (component instanceof HtmlPanel) {
						HtmlPanel htmlPanel = (HtmlPanel) component;
						htmlPanel.setText(text);
					} else if (component instanceof JPanel) {
						component = actions;
					}
				}
				pTooltip = fixPanelSizeAndLocation(mv, text, xl, xr, yt, yb);
			}

			displayedWindow.pack();
			displayedWindow.setLocation(pTooltip);
			displayedWindow.setVisible(mv.contains(p));
			displayedNode = selectedNode;

		}

		/**
		 * Get the location of the note panel
		 * @param mv {@code MapView} that is being drawn on
		 * @param text The text going into the note panel
		 * @param xl The left side of the icon
		 * @param xr The right side of the icon
		 * @param yt The top of the icon
		 * @param yb The bottom of the icon
		 * @return The point at which we are drawing the note panel
		 */
		private Point fixPanelSizeAndLocation(MapView mv, String text, int xl, int xr, int yt, int yb) {
			int leftMaxWidth = (int) (0.95 * xl);
			int rightMaxWidth = (int) (0.95 * mv.getWidth() - xr);
			int topMaxHeight = (int) (0.95 * yt);
			int bottomMaxHeight = (int) (0.95 * mv.getHeight() - yb);
			int maxWidth = Math.max(leftMaxWidth, rightMaxWidth);
			int maxHeight = Math.max(topMaxHeight, bottomMaxHeight);
			HtmlPanel htmlPanel = new HtmlPanel();
			JPanel actions = new JPanel();
			for (Component component : displayedPanel.getComponents()) {
				if (component instanceof HtmlPanel) {
					htmlPanel = (HtmlPanel) component;
				} else if (component instanceof JPanel) {
					actions = (JPanel) component;
				}
			}
			JEditorPane pane = htmlPanel.getEditorPane();
			Dimension d = pane.getPreferredSize();
			Dimension daction = actions.getPreferredSize();
			if ((d.width > maxWidth || d.height > maxHeight) && Config.getPref().getBoolean("note.text.break-on-sentence-mark", false)) {
				// To make sure long notes are displayed correctly
				htmlPanel.setText(insertLineBreaks(text));
			}
			// If still too large, enforce maximum size
			d = pane.getPreferredSize();
			if (d.width > maxWidth || d.height > maxHeight) {
				View v = (View) pane.getClientProperty(BasicHTML.propertyKey);
				if (v == null) {
					BasicHTML.updateRenderer(pane, text);
					v = (View) pane.getClientProperty(BasicHTML.propertyKey);
				}
				if (v != null) {
					v.setSize(maxWidth, 0);
					int w = (int) Math.ceil(v.getPreferredSpan(View.X_AXIS));
					int h = (int) Math.ceil(v.getPreferredSpan(View.Y_AXIS)) + 10;
					pane.setPreferredSize(new Dimension(w, h));
				}
			}
			d = pane.getPreferredSize();
			// place tooltip on left or right side of icon, based on its width
			Point screenloc = mv.getLocationOnScreen();
			d.setSize(Math.max(d.getWidth(), daction.getWidth()), d.getHeight() + daction.getHeight());
			displayedPanel.setPreferredSize(d);
			return new Point(
					screenloc.x + (d.width > rightMaxWidth && d.width <= leftMaxWidth ? xl - d.width : xr),
					screenloc.y + (d.height > bottomMaxHeight && d.height <= topMaxHeight ? yt - d.height - 10 : yb));
		}
	}

	/**
	 * Hide the displayedWindow of the error notes
	 */
	private void hideNodeWindow() {
		if (displayedWindow != null) {
			displayedWindow.setVisible(false);
			for (MouseWheelListener listener : displayedWindow.getMouseWheelListeners()) {
				displayedWindow.removeMouseWheelListener(listener);
			}
			displayedWindow.dispose();
			displayedWindow = null;
			displayedPanel = null;
			displayedNode = null;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}

		ArrayList<Node> closestNode = getClosestNode(e.getPoint(), 10);
		if (!closestNode.isEmpty()) {
			ds.setSelected(closestNode);
		} else {
			ds.clearSelection();
		}
		if (!closestNode.contains(displayedNode)) {
			hideNodeWindow();
		}
		invalidate();
		nodeIndex++;
	}

	/**
	 * Get the closest nodes to a point
	 * @param mousePoint The current location of the mouse
	 * @param snapDistance The maximum distance to find the closest node
	 * @return The closest {@code Node}s
	 */
	private ArrayList<Node> getClosestNode(Point mousePoint, double snapDistance) {
		ArrayList<Node> closestNode = new ArrayList<>();
		for (Node node : ds.getNodes()) {
			Point notePoint = MainApplication.getMap().mapView.getPoint(node.getBBox().getCenter());
			if (mousePoint.distance(notePoint) < snapDistance) {
				closestNode.add(node);
			}
		}
		return closestNode;
	}

	@Override
	public Action[] getMenuEntries() {
		ArrayList<Action> actions = new ArrayList<>();
		actions.add(LayerListDialog.getInstance().createShowHideLayerAction());
		actions.add(LayerListDialog.getInstance().createDeleteLayerAction());
		actions.add(new LayerListPopup.InfoAction(this));
		actions.add(new ForceClear(CACHE_DIR));
		return actions.toArray(new Action[0]);
	}

	/**
	 * Inserts HTML line breaks ({@code <br>} at the end of each sentence mark
	 * (period, interrogation mark, exclamation mark, ideographic full stop).
	 * @param longText a long text that does not fit on a single line without exceeding half of the map view
	 * @return text with line breaks
	 */
	static String insertLineBreaks(String longText) {
		return SENTENCE_MARKS_WESTERN.matcher(SENTENCE_MARKS_EASTERN.matcher(longText).replaceAll("$1<br>$2")).replaceAll("$1<br>$2");
	}

	@Override
	public Icon getIcon() {
		return ImageProvider.get("dialogs/notes", "note_open", ImageProvider.ImageSizes.SMALLICON);
	}

	@Override
	public String getToolTipText() {
		int size = ds.getNodes().size();
		return trn("{0} keepright note", "{0} keepright notes", size, size);
	}

	@Override
	public void mergeFrom(Layer from) {
		if (from instanceof ErrorLayer) {
			ErrorLayer efrom = (ErrorLayer) from;
			ds.mergeFrom(efrom.ds);
		}
	}

	@Override
	public boolean isMergable(Layer other) {
		return (other instanceof ErrorLayer);
	}

	@Override
	public void visitBoundingBox(BoundingXYVisitor v) {
		for (OsmPrimitive osm : ds.allPrimitives()) {
			v.visit(osm.getBBox().getCenter());
		}

	}

	@Override
	public Object getInfoComponent() {
		StringBuilder sb = new StringBuilder();
		sb.append(tr("Keep Right Layer"))
		.append('\n').append(tr("Total notes")).append(' ')
		.append(ds.allPrimitives().size());
		return sb;
	}

	@Override
	public void selectionChanged(SelectionChangeEvent event) {
		Set<OsmPrimitive> selected = event.getAdded();
		ds.setSelected(selected);
		invalidate();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// Do nothing
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// Do nothing
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// Do nothing
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// Do nothing
	}

	@Override
	public void highlightUpdated(HighlightUpdateEvent e) {
		for (OsmPrimitive osmPrimitive : e.getDataSet().allPrimitives()) {
			if (osmPrimitive instanceof Node && (osmPrimitive.hasKey("actionTaken") || "false".equals(osmPrimitive.get("actionTaken")))) {
				type.getNodeToolTip((Node) osmPrimitive);
				if (!osmPrimitive.hasKey("actionTaken")) {
					osmPrimitive.put("actionTaken", "true");
				}
			}
		}
	}
}
