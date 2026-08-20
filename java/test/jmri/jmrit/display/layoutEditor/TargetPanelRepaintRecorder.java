package jmri.jmrit.display.layoutEditor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

/**
 * Records the areas a Layout Editor drawing panel is asked to repaint, for
 * testing use.
 * <p>
 * Icons on a panel should dirty only the area they cover, by calling
 * {@link jmri.jmrit.display.Editor#repaintTargetPanel(Rectangle)}, rather than
 * the whole drawing area with {@link LayoutEditor#redrawPanel()}. Both end up
 * in {@link RepaintManager#addDirtyRegion(JComponent, int, int, int, int)} for
 * the target panel, which is what this class watches.
 * <p>
 * Install it with {@link #install(LayoutEditor)} and remove it with
 * {@link #remove()} once the test is done, as it replaces the current
 * RepaintManager for the whole application context.
 *
 * @author Adrien Virolleaud Copyright (C) 2026
 */
public class TargetPanelRepaintRecorder extends RepaintManager {

    private final JComponent targetPanel;
    private final List<Rectangle> dirtyRegions = new ArrayList<>();

    private TargetPanelRepaintRecorder(JComponent targetPanel) {
        this.targetPanel = targetPanel;
    }

    /**
     * Start recording the repaints requested on the drawing panel of an editor.
     *
     * @param editor the editor to watch
     * @return the recorder, to be removed by {@link #remove()}
     */
    public static TargetPanelRepaintRecorder install(LayoutEditor editor) {
        TargetPanelRepaintRecorder recorder =
                new TargetPanelRepaintRecorder(editor.getTargetPanel());
        RepaintManager.setCurrentManager(recorder);
        return recorder;
    }

    /**
     * Restore the default RepaintManager.
     */
    public void remove() {
        RepaintManager.setCurrentManager(null);
    }

    /**
     * Forget what was recorded so far.
     */
    public void reset() {
        dirtyRegions.clear();
    }

    /**
     * Get the areas the drawing panel was asked to repaint.
     *
     * @return the recorded areas, in the order they were requested
     */
    public List<Rectangle> getDirtyRegions() {
        return new ArrayList<>(dirtyRegions);
    }

    /**
     * Check whether the drawing panel was asked to repaint an area which covers
     * the given one without covering the whole panel.
     * <p>
     * Other repaints may well be recorded alongside it, so this asks whether a
     * bounded one is present, not whether it is the only one.
     *
     * @param area the area which has to be covered
     * @return true if such an area was repainted
     */
    public boolean hasBoundedRegionCovering(Rectangle area) {
        Rectangle panel = getPanelBounds();
        for (Rectangle region : dirtyRegions) {
            if (region.contains(area) && region.width < panel.width && region.height < panel.height) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the whole area of the watched drawing panel, what a full repaint
     * would dirty.
     *
     * @return the panel bounds, in its own coordinates
     */
    public Rectangle getPanelBounds() {
        return new Rectangle(0, 0, targetPanel.getWidth(), targetPanel.getHeight());
    }

    @Override
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        if (c == targetPanel) {
            dirtyRegions.add(new Rectangle(x, y, w, h));
        }
        super.addDirtyRegion(c, x, y, w, h);
    }

}
