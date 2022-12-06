// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.actions.mapmode.SelectLassoAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSourceChangeEvent;
import org.openstreetmap.josm.data.osm.DataSourceListener;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

import com.kaart.openqa.profiles.GenericInformation;
import com.kaart.openqa.profiles.OpenQANode;
import com.kaart.openqa.profiles.keepright.KeepRightInformation;
import com.kaart.openqa.profiles.osmose.OsmoseInformation;

/**
 * The layer to display issues
 */
public class ErrorLayer extends AbstractModifiableLayer
        implements MouseListener, MouseMotionListener, LayerChangeListener, DataSourceListener {
    // This could be a record
    private static final class DataSetPairs<I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> {
        private final D dataset;
        private final GenericInformation<I, N, D> genericInformation;

        /**
         * Constructs a new {@code Pair}.
         *
         * @param genericInformation The first item
         * @param dataset            The second item
         */
        public DataSetPairs(GenericInformation<I, N, D> genericInformation, D dataset) {
            Objects.requireNonNull(genericInformation);
            this.genericInformation = genericInformation;
            this.dataset = dataset == null ? genericInformation.createNewDataSet() : dataset;
        }

        /**
         * Get the dataset for this pair
         *
         * @return the dataset
         */
        public D dataset() {
            return this.dataset;
        }

        /**
         * Get the generic information for this pair
         *
         * @return The info
         */
        public GenericInformation<I, N, D> genericInformation() {
            return this.genericInformation;
        }

        /**
         * Update the dataset of this pair
         *
         * @param progressMonitor The progress monitor to use
         * @param data            The data to get errors from
         */
        public void update(ProgressMonitor progressMonitor, Data data) {
            progressMonitor.indeterminateSubTask(tr("Updating {0}", genericInformation.getLayerName()));
            OpenQADataSet<I, N> mergeFrom = genericInformation.getErrors(data, progressMonitor);
            dataset.mergeFrom(mergeFrom);
        }
    }

    /**
     * Pattern to detect end of sentences followed by another one, or a link, in
     * western script. Group 1 (capturing): period, interrogation mark, exclamation
     * mark Group non capturing: at least one horizontal or vertical whitespace
     * Group 2 (capturing): a letter (any script), or any punctuation
     */
    private static final Pattern SENTENCE_MARKS_WESTERN = Pattern.compile("([.?!])(?:[\\h\\v]+)([\\p{L}\\p{IsPunct}])");

    /**
     * Pattern to detect end of sentences followed by another one, or a link, in
     * eastern script. Group 1 (capturing): ideographic full stop Group 2
     * (capturing): a letter (any script), or any punctuation
     */
    private static final Pattern SENTENCE_MARKS_EASTERN = Pattern.compile("(\\u3002)([\\p{L}\\p{IsPunct}])");

    private static final String STRING_ACTION_TAKEN = "actionTaken";

    final List<DataSetPairs<?, ?, ?>> dataSets = new ArrayList<>(2);
    final HashMap<GenericInformation<?, ?, ?>, Boolean> enabledSources = new HashMap<>();

    private OpenQANode<?> displayedNode;
    private JScrollPane displayedPanel;
    private JWindow displayedWindow;
    private PaintWindow window;

    private boolean updateCanceled;

    private final List<DataSet> listeningDataSets = new ArrayList<>();

    /**
     * Create a new ErrorLayer using a class that extends {@code GenericInformation}
     *
     */
    public ErrorLayer() {
        super(tr("{0} Layers", OpenQA.NAME));
        hookUpMapViewer();
        MainApplication.getLayerManager().addAndFireLayerChangeListener(this);
    }

    /**
     * Set the error classes
     *
     * @param types The types of class to get errors for. Must extend
     *              GenericInformation.
     */
    public void setErrorClasses(Class<?>... types) {
        for (Class<?> type : types) {
            if (!GenericInformation.class.isAssignableFrom(type))
                continue;
            try {
                Constructor<?> constructor = type.getConstructor();
                Object obj = constructor.newInstance();
                if (!(obj instanceof GenericInformation))
                    continue;
                if (obj instanceof OsmoseInformation) {
                    OsmoseInformation info = (OsmoseInformation) obj;
                    dataSets.add(new DataSetPairs<>(info, info.createNewDataSet()));
                } else if (obj instanceof KeepRightInformation) {
                    KeepRightInformation info = (KeepRightInformation) obj;
                    dataSets.add(new DataSetPairs<>(info, info.createNewDataSet()));
                } else {
                    throw new UnsupportedOperationException("Unknown type: " + obj.getClass().getName());
                }
                GenericInformation<?, ?, ?> info = (GenericInformation<?, ?, ?>) obj;
                enabledSources.put(info, true);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                Logging.error(e);
                new BugReport(new ReportedException(e));
            }
        }
        addListeners();
    }

    /**
     * Cancel the update
     */
    public void cancel() {
        updateCanceled = true;
    }

    /**
     * Update the backing information data stores
     *
     * @param monitor The monitor to show updates with. Can be null.
     */
    public void update(ProgressMonitor monitor) {
        if (monitor == null)
            monitor = NullProgressMonitor.INSTANCE;
        List<OsmDataLayer> dataLayers = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class);
        ProgressMonitor progressMonitor = monitor.createSubTaskMonitor(0, false);
        progressMonitor.beginTask(tr("Updating {0} layers", OpenQA.NAME));
        for (DataSetPairs<?, ?, ?> entry : dataSets) {
            if (updateCanceled) {
                progressMonitor.cancel();
                break;
            }
            if (!Boolean.TRUE.equals(this.enabledSources.getOrDefault(entry.genericInformation, false))) {
                continue;
            }
            for (OsmDataLayer layer : dataLayers) {
                if (updateCanceled)
                    break;
                entry.update(monitor, layer.getData());
            }
        }
        progressMonitor.finishTask();
        invalidate();
    }

    /**
     * Add this class to a map viewer. Usually called during initialization.
     */
    public void hookUpMapViewer() {
        MainApplication.getMap().mapView.addMouseListener(this);
        addListeners();
    }

    private void addListeners() {
        dataSets.forEach(pair -> pair.dataset.addHighlightUpdateListener(this));
    }

    @Override
    public synchronized void destroy() {
        MainApplication.getMap().mapView.removeMouseListener(this);
        for (OpenQADataSet<?, ?> ds : dataSets.stream().map(pair -> pair.dataset).collect(Collectors.toList())) {
            try {
                if (ds == null)
                    continue;
                ds.removeHighlightUpdateListener(this);
            } catch (IllegalArgumentException e) {
                Logging.error(e);
            }
        }
        hideNodeWindow();
        MainApplication.getLayerManager().removeLayerChangeListener(this);
        super.destroy();
    }

    @Override
    public boolean isModified() {
        for (DataSetPairs<?, ?, ?> ds : dataSets) {
            if (ds.dataset().isModified()) {
                return true;
            }
        }
        return false;
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

    private class PaintWindow implements DataSelectionListener, Runnable {
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
            for (DataSetPairs<?, ?, ?> type : dataSets) {
                if (enabledSources.containsKey(type.genericInformation())
                        && Boolean.TRUE.equals(!enabledSources.get(type.genericInformation())))
                    continue;
                realRun(type);
            }
        }

        private <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> void realRun(DataSetPairs<I, N, D> type) {
            final ImageSizes size = ImageProvider.ImageSizes.LARGEICON;
            D ds = type.dataset();
            for (N node : ds.getNodes()) {
                Point p = mv.getPoint(node);
                String error = type.genericInformation().getError(node);
                final ImageIcon icon;
                // See #22249
                if (error == null) {
                    icon = ImageProvider.createBlankIcon(ImageSizes.MAP);
                    new Notification(tr("Image Error: {0}: {1}", type.genericInformation().getName(),
                            node.getKeys().entrySet().stream()
                                    .map(entry -> String.join("=", entry.getKey(), entry.getValue()))
                                    .collect(Collectors.joining(", ")))).show();
                } else {
                    icon = type.genericInformation().getIcon(error, size);
                }
                int width = icon.getIconWidth();
                int height = icon.getIconHeight();
                g.drawImage(icon.getImage(), p.x - (width / 2), p.y - (height / 2), MainApplication.getMap().mapView);
            }
            createNodeWindow(g, mv, size);
        }

        private void createNodeWindow(Graphics2D g, MapView mv, ImageSizes size) {
            Map<GenericInformation<?, ?, ?>, List<OpenQANode<?>>> selectedErrors = new HashMap<>();

            for (DataSetPairs<?, ?, ?> entry : dataSets) {
                OpenQADataSet<?, ?> ds = entry.dataset();
                if (ds == null)
                    continue;
                List<OpenQANode<?>> selectedNodes = new ArrayList<>(ds.getSelectedNodes());
                selectedNodes.sort(null);
                if (!selectedNodes.isEmpty()) {
                    selectedErrors.put(entry.genericInformation(), selectedNodes);
                }
            }
            MapMode mode = MainApplication.getMap().mapMode;
            if (!selectedErrors.isEmpty() && (mode instanceof SelectAction || mode instanceof SelectLassoAction)) {
                final int iconHeight = size.getAdjustedHeight();
                final int iconWidth = size.getAdjustedWidth();
                paintSelectedNode(g, mv, iconHeight, iconWidth, selectedErrors);
            } else {
                for (DataSetPairs<?, ?, ?> ds : dataSets) {
                    ds.dataset().clearSelection();
                }
            }
        }

        /**
         * Create the note window
         *
         * @param g              The {@code Graphics2D} object that will be the note
         *                       background
         * @param mv             The {@code MapView} object that we are drawing on
         * @param iconHeight     The height of the selection box that we are drawing
         * @param iconWidth      The width of the selection box we are drawing
         * @param selectedErrors The errors that have been selected from which to get
         *                       information from
         */
        private void paintSelectedNode(Graphics2D g, MapView mv, int iconHeight, int iconWidth,
                Map<GenericInformation<?, ?, ?>, List<OpenQANode<?>>> selectedErrors) {
            double averageEast = 0.0;
            double averageNorth = 0.0;
            int number = 0;
            for (List<OpenQANode<?>> nodes : selectedErrors.values()) {
                for (OpenQANode<?> node : nodes) {
                    number++;
                    EastNorth ten = node.getEastNorth(ProjectionRegistry.getProjection());
                    averageEast += ten.east();
                    averageNorth += ten.north();
                }
            }
            if (number == 0) {
                return;
            }
            EastNorth currentClick = new EastNorth(averageEast / number, averageNorth / number);
            Point p = mv.getPoint(currentClick);
            g.setColor(ColorHelper.html2color(Config.getPref().get("color.selected")));
            g.drawRect(p.x - (iconWidth / 2), p.y - (iconHeight / 2), iconWidth - 1, iconHeight - 1);

            int xl = p.x - (iconWidth / 2) - 5;
            int xr = p.x + (iconWidth / 2) + 5;
            int yb = p.y - iconHeight - 1;
            int yt = p.y + (iconHeight / 2) + 2;
            Point pTooltip;

            if (displayedWindow == null) {
                JPanel interiorPanel = new JPanel();
                displayedPanel = new JScrollPane(interiorPanel);
                displayedPanel.getVerticalScrollBar().setUnitIncrement(30);
                interiorPanel.setLayout(new BoxLayout(interiorPanel, BoxLayout.Y_AXIS));
                displayedWindow = new JWindow(MainApplication.getMainFrame());
                displayedWindow.setAutoRequestFocus(false);
                displayedWindow.add(displayedPanel);
                // Forward mouse wheel scroll event to MapMover
                displayedWindow.addMouseWheelListener(e -> mv.getMapMover()
                        .mouseWheelMoved((MouseWheelEvent) SwingUtilities.convertMouseEvent(displayedWindow, e, mv)));
                for (DataSetPairs<?, ?, ?> entry : dataSets) {
                    paintSelectedNode(entry, interiorPanel);
                }
            }

            pTooltip = fixPanelSizeAndLocation(mv, (JComponent) displayedPanel.getComponent(0), xl, xr, yt, yb);

            Dimension d = displayedPanel.getPreferredSize();
            d.setSize(d.getWidth(), Math.min(d.getHeight(), 450));

            int topMaxHeight = (int) (0.95 * yt);
            int bottomMaxHeight = (int) (0.95 * mv.getHeight() - yb);
            int maxHeight = Math.max(topMaxHeight, bottomMaxHeight);
            d.setSize(d.getWidth(), Math.min(d.getHeight(), maxHeight));
            displayedPanel.setPreferredSize(d);

            displayedWindow.pack();
            displayedWindow.setLocation(pTooltip);
            displayedWindow.setVisible(mv.contains(p));
            if (!mv.contains(p))
                hideNodeWindow();
        }

        private <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> void paintSelectedNode(
                DataSetPairs<I, N, D> entry, JPanel interiorPanel) {
            for (N selectedNode : entry.dataset().getSelectedNodes()) {
                String text = entry.genericInformation().getNodeToolTip(selectedNode);

                HtmlPanel htmlPanel = new HtmlPanel(text);
                htmlPanel.setBackground(UIManager.getColor("ToolTip.background"));
                htmlPanel.setForeground(UIManager.getColor("ToolTip.foreground"));
                htmlPanel.setFont(UIManager.getFont("ToolTip.font"));
                htmlPanel.setBorder(BorderFactory.createLineBorder(Color.black));
                htmlPanel.enableClickableHyperlinks();
                JPanel tPanel = new JPanel();
                tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.Y_AXIS));
                tPanel.add(htmlPanel);

                List<JButton> actions = entry.genericInformation().getActions(selectedNode);
                JPanel pActions = new JPanel();
                double minWidth = 0.0;
                for (JButton action : actions) {
                    pActions.add(action);
                    minWidth += action.getPreferredSize().getWidth();
                }
                Dimension d = pActions.getPreferredSize();
                d.setSize(minWidth, d.getHeight());
                pActions.setPreferredSize(d);
                tPanel.add(pActions);
                interiorPanel.add(tPanel);
            }
        }

        /**
         * Get the location of the note panel
         *
         * @param mv    {@code MapView} that is being drawn on
         * @param panel The JPanel we are drawing
         * @param xl    The left side of the icon
         * @param xr    The right side of the icon
         * @param yt    The top of the icon
         * @param yb    The bottom of the icon
         * @return The point at which we are drawing the note panel
         */
        private Point fixPanelSizeAndLocation(MapView mv, JComponent panel, int xl, int xr, int yt, int yb) {
            int leftMaxWidth = (int) (0.95 * xl);
            int rightMaxWidth = (int) (0.95 * mv.getWidth() - xr);
            int topMaxHeight = (int) (0.95 * yt);
            int bottomMaxHeight = (int) (0.95 * mv.getHeight() - yb);
            int maxWidth = Math.max(leftMaxWidth, rightMaxWidth);
            int maxHeight = Math.max(topMaxHeight, bottomMaxHeight);
            for (Component sComponent : panel.getComponents()) {
                if (sComponent instanceof JPanel) {
                    JPanel tPanel = (JPanel) sComponent;
                    for (Component component : tPanel.getComponents()) {
                        if (component instanceof HtmlPanel) {
                            HtmlPanel htmlPanel = (HtmlPanel) component;
                            JEditorPane pane = htmlPanel.getEditorPane();
                            Dimension d = pane.getPreferredSize();

                            if ((d.width > maxWidth || d.height > maxHeight)
                                    && Config.getPref().getBoolean("note.text.break-on-sentence-mark", true)) {
                                // To make sure long notes are displayed correctly
                                htmlPanel.setText(insertLineBreaks(pane.getText()));
                            }
                            // If still too large, enforce maximum size
                            d = pane.getPreferredSize();

                            if (d.width > maxWidth || d.height > maxHeight) {
                                View v = (View) pane.getClientProperty(BasicHTML.propertyKey);
                                if (v == null) {
                                    BasicHTML.updateRenderer(pane, pane.getText());
                                    v = (View) pane.getClientProperty(BasicHTML.propertyKey);
                                }
                                if (v != null) {
                                    v.setSize(maxWidth, 0);
                                    int w = (int) Math.ceil(v.getPreferredSpan(View.X_AXIS));
                                    int h = (int) Math.ceil(v.getPreferredSpan(View.Y_AXIS)) + 10;
                                    pane.setPreferredSize(new Dimension(w, h));
                                }
                            }
                            Dimension daction = htmlPanel.getPreferredSize();
                            d = pane.getPreferredSize();
                            d.setSize(Math.max(d.getWidth(), daction.getWidth()),
                                    Math.max(d.getHeight(), daction.getHeight()));
                            pane.setPreferredSize(d);
                        }
                    }
                }
            }
            Dimension d = panel.getPreferredSize();
            // place tooltip on left or right side of icon, based on its width
            Point screenloc = mv.getLocationOnScreen();
            return new Point(screenloc.x + (d.width > rightMaxWidth && d.width <= leftMaxWidth ? xl - d.width : xr),
                    screenloc.y + (d.height > bottomMaxHeight && d.height <= topMaxHeight ? yt - d.height - 10 : yb));
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            hideNodeWindow();
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
        invalidate();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
        if (ds != null && !ds.isModified()) {
            GenericInformation.addChangeSetTag(null, null);
        }
        new GetClosestNode(e).run();
    }

    private class GetClosestNode implements Runnable {
        final MouseEvent e;

        GetClosestNode(MouseEvent e) {
            this.e = e;
        }

        /**
         * Get the closest nodes to a point
         *
         * @param mousePoint   The current location of the mouse
         * @param snapDistance The maximum distance to find the closest node
         * @return The closest {@code Node}s
         */
        private Map<GenericInformation<?, ?, ?>, List<OpenQANode<?>>> getClosestNode(Point mousePoint,
                double snapDistance) {
            Map<GenericInformation<?, ?, ?>, List<OpenQANode<?>>> closestNodes = new HashMap<>();
            for (DataSetPairs<?, ?, ?> entry : dataSets) {
                GenericInformation<?, ?, ?> type = entry.genericInformation();
                OpenQADataSet<?, ?> ds = entry.dataset();
                if (ds == null)
                    continue;
                List<OpenQANode<?>> closestNode = new ArrayList<>();
                for (OpenQANode<?> node : ds.getNodes()) {
                    Point notePoint = MainApplication.getMap().mapView.getPoint(node);
                    if (mousePoint.distance(notePoint) < snapDistance) {
                        closestNode.add(node);
                    }
                }
                if (!closestNode.isEmpty()) {
                    closestNodes.put(type, closestNode);
                }
            }
            return closestNodes;
        }

        private void getAdditionalInformation() {
            for (DataSetPairs<?, ?, ?> entry : dataSets) {
                getAdditionalInformation(entry);
            }
        }

        private <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> void getAdditionalInformation(
                DataSetPairs<I, N, D> entry) {
            boolean hasAdditionalInformation;
            D ds = entry.dataset();
            GenericInformation<I, N, D> type = entry.genericInformation();
            for (N node : ds.getSelectedNodes()) {
                hasAdditionalInformation = type.cacheAdditionalInformation(node);
                if (!hasAdditionalInformation)
                    break;
            }
        }

        @Override
        public void run() {
            Map<GenericInformation<?, ?, ?>, List<OpenQANode<?>>> closestNode = getClosestNode(e.getPoint(), 10);
            for (DataSetPairs<?, ?, ?> entry : dataSets) {
                selectNode(closestNode, entry);
            }
            boolean gotNode = false;
            if (displayedNode != null) {
                for (DataSetPairs<?, ?, ?> ds : dataSets) {
                    if (ds != null && ds.dataset().containsNode(displayedNode)) {
                        gotNode = true;
                        break;
                    }
                }
            }
            getAdditionalInformation();
            if (!gotNode) {
                hideNodeWindow();
            } else {
                invalidate();
            }
        }

        @SuppressWarnings("unchecked")
        private <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> void selectNode(
                Map<GenericInformation<?, ?, ?>, List<OpenQANode<?>>> closestNode, DataSetPairs<I, N, D> entry) {
            GenericInformation<I, N, D> type = entry.genericInformation();
            D ds = entry.dataset();
            if (closestNode.containsKey(type)) {
                ds.setSelected((List<N>) closestNode.get(type));
            } else {
                ds.clearSelection();
            }
            if (!closestNode.containsKey(type)) {
                ds.clearSelection();
            }
        }
    }

    @Override
    public Action[] getMenuEntries() {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(LayerListDialog.getInstance().createShowHideLayerAction());
        actions.add(LayerListDialog.getInstance().createDeleteLayerAction());
        actions.add(new LayerListPopup.InfoAction(this));
        actions.add(new ForceClear());
        for (GenericInformation<?, ?, ?> type : enabledSources.keySet()) {
            actions.add(new ToggleSource(type));
        }
        return actions.toArray(new Action[0]);
    }

    private class ToggleSource extends AbstractAction {
        private static final long serialVersionUID = -3530922723120575358L;
        private final transient GenericInformation<?, ?, ?> type;

        public ToggleSource(GenericInformation<?, ?, ?> type) {
            this.type = type;
            if (Boolean.FALSE.equals(enabledSources.get(type))) {
                new ImageProvider("warning-small").getResource().attachImageIcon(this, true);
            } else {
                new ImageProvider("dialogs", "validator").getResource().attachImageIcon(this, true);
            }
            putValue(SHORT_DESCRIPTION, tr("Toggle source {0}", type.getName()));
            putValue(NAME, tr("Toggle source {0}", type.getName()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            enabledSources.put(type, !enabledSources.get(type));
            invalidate();
        }
    }

    private class ForceClear extends AbstractAction {
        private static final long serialVersionUID = -4472400258489788312L;

        public ForceClear() {
            new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Clear cached information for OpenQA."));
            putValue(NAME, tr("Clear"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OpenQACache.clear();
            dataSets.forEach(this::forceClear);
            OpenQALayerChangeListener.updateOpenQALayers();
        }

        private <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> void forceClear(
                DataSetPairs<I, N, D> entry) {
            D ds = entry.dataset();
            D temporaryDataSet = entry.genericInformation().createNewDataSet();
            for (N osmPrimitive : ds.allPrimitives()) {
                if (osmPrimitive.hasKey(STRING_ACTION_TAKEN)) {
                    ds.removePrimitive(osmPrimitive);
                    temporaryDataSet.addPrimitive(osmPrimitive);
                }
            }
            ds.clear();
            ds.mergeFrom(temporaryDataSet);
        }

    }

    /**
     * Inserts HTML line breaks ({@code <br>
     * } at the end of each sentence mark (period, interrogation mark, exclamation
     * mark, ideographic full stop).
     *
     * @param longText a long text that does not fit on a single line without
     *                 exceeding half of the map view
     * @return text with line breaks
     */
    static String insertLineBreaks(String longText) {
        return SENTENCE_MARKS_WESTERN.matcher(SENTENCE_MARKS_EASTERN.matcher(longText).replaceAll("$1<br>$2"))
                .replaceAll("$1<br>$2");
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get(OpenQA.OPENQA_IMAGE, ImageProvider.ImageSizes.SMALLICON);
    }

    @Override
    public String getToolTipText() {
        int size = 0;
        for (DataSetPairs<?, ?, ?> ds : dataSets) {
            size += ds.dataset().getNodes().size();
        }
        return trn("{0} {1} note", "{0} {1} notes", size, size, OpenQA.NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void mergeFrom(Layer from) {
        if (from instanceof ErrorLayer) {
            ErrorLayer efrom = (ErrorLayer) from;
            for (DataSetPairs<?, ?, ?> eEntry : efrom.dataSets) {
                boolean merged = false;
                for (DataSetPairs<?, ?, ?> entry : dataSets) {
                    if (entry.dataset().getClass().isInstance(eEntry.dataset())) {
                        entry.dataset().mergeFrom(entry.dataset().getClass().cast(eEntry.dataset()));
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    dataSets.add(eEntry);
                }
            }
        }
    }

    @Override
    public boolean isMergable(Layer other) {
        return (other instanceof ErrorLayer);
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        for (DataSetPairs<?, ?, ?> ds : dataSets) {
            for (OpenQANode<?> osm : ds.dataset().allPrimitives()) {
                v.visit(osm);
            }
        }

    }

    @Override
    public Object getInfoComponent() {
        int size = 0;
        for (DataSetPairs<?, ?, ?> ds : dataSets) {
            size += ds.dataset().allPrimitives().size();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(tr("Keep Right Layer")).append('\n').append(tr("Total notes")).append(' ').append(size);
        return sb;
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
    public void mouseDragged(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        final BBox location = new BBox(MainApplication.getMap().mapView.getLatLon(e.getX(), e.getY()));
        for (DataSetPairs<?, ?, ?> entry : this.dataSets) {
            highlightUpdated(location, entry);
        }
    }

    private static <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> void highlightUpdated(BBox location,
            DataSetPairs<I, N, D> entry) {
        // FIXME
        Collection<N> nodes = entry.dataset().searchNodes(location);

        for (N node : nodes) {
            entry.genericInformation().getNodeToolTip(node);
            if (!node.hasKey(STRING_ACTION_TAKEN)) {
                node.put(STRING_ACTION_TAKEN, "true");
            }
        }
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        if (e.getAddedLayer() instanceof OsmDataLayer) {
            DataSet ds = ((OsmDataLayer) e.getAddedLayer()).getDataSet();
            if (!this.listeningDataSets.contains(ds)) {
                ds.addDataSourceListener(this);
                this.listeningDataSets.add(ds);
            }
        }
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof OsmDataLayer) {
            DataSet ds = ((OsmDataLayer) e.getRemovedLayer()).getDataSet();
            if (this.listeningDataSets.contains(ds)) {
                ds.removeDataSourceListener(this);
                this.listeningDataSets.remove(ds);
            }
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    @Override
    public void dataSourceChange(DataSourceChangeEvent event) {
        OpenQALayerChangeListener.updateOpenQALayers();
    }
}
