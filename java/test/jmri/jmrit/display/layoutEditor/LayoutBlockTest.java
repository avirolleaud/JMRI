package jmri.jmrit.display.layoutEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

import jmri.Block;
import jmri.InstanceManager;
import jmri.Memory;
import jmri.SensorManager;
import jmri.jmrix.internal.InternalSensorManager;
import jmri.jmrix.internal.InternalSystemConnectionMemo;
import jmri.util.JUnitUtil;
import jmri.util.junit.annotations.DisabledIfHeadless;

import org.jdom2.JDOMException;

import org.junit.jupiter.api.*;
import org.netbeans.jemmy.QueueTool;

/**
 * Test simple functioning of LayoutBlock
 *
 * @author Paul Bender Copyright (C) 2016
 */
public class LayoutBlockTest {

    private LayoutBlock layoutBlock = null;

    @Test
    public void testCtor() {
        assertNotNull( layoutBlock, "exists");
        assertEquals("Test Block", layoutBlock.getUserName());
    }

    @Test
    public void testBlockRename() {
        // initialize the layout block and the related automatic block
        layoutBlock.initializeLayoutBlock();

        // Get the referenced block and change its user name
        Block block = jmri.InstanceManager.getDefault(jmri.BlockManager.class).getByUserName("Test Block");
        assertNotNull(block);
        block.setUserName("New Test Block");

        // Verify that the block user name change propagated to the layout block
        assertEquals("New Test Block", layoutBlock.getUserName());
    }

    @Test
    public void testBlockSensor() {
        // initialize the layout block and the related automatic block
        layoutBlock.initializeLayoutBlock();

        // Create an occupancy sensor
        SensorManager sm = new InternalSensorManager(InstanceManager.getDefault(InternalSystemConnectionMemo.class));
        sm.provideSensor("IS123");

        // Get the referenced block and set its occupancy sensor
        Block block = jmri.InstanceManager.getDefault(jmri.BlockManager.class).getByUserName("Test Block");
        assertNotNull(block);
        block.setSensor("IS123");

        // Verify that the block sensor change propagated to the layout block
        assertEquals("IS123", layoutBlock.getOccupancySensorName());
    }

    @Test
    public void testSetMemoryFromStringBlockValue() {
        // initialize the layout block and the related automatic block
        layoutBlock.initializeLayoutBlock();

        // get a memory and associate it with the layout block.
        Memory mem = jmri.InstanceManager.getDefault(jmri.MemoryManager.class).provideMemory("IM1");

        layoutBlock.setMemory(mem,"IM1");

        // verify the memory is associated
        assertEquals( mem, layoutBlock.getMemory(), "memory saved");

        // Get the referenced block
        Block block = jmri.InstanceManager.getDefault(jmri.BlockManager.class).getByUserName("Test Block");
        Assertions.assertNotNull(block);

        // change the value of the block.
        block.setValue("hello world");

        // and verify the value is in the memory
        assertEquals( block.getValue(), mem.getValue(), "memory content same as block value");

    }

    @Test
    public void testSetMemoryFromRosterEntryBlockValue() throws IOException, JDOMException {
        // initialize the layout block and the related automatic block
        layoutBlock.initializeLayoutBlock();

        // get a memory and associate it with the layout block.
        Memory mem = jmri.InstanceManager.getDefault(jmri.MemoryManager.class).provideMemory("IM1");

        layoutBlock.setMemory(mem,"IM1");

        // verify the memory is associated
        assertEquals( mem, layoutBlock.getMemory(), "memory saved");

        // Get the referenced block
        Block block = jmri.InstanceManager.getDefault(jmri.BlockManager.class).getByUserName("Test Block");
        Assertions.assertNotNull(block);

        // add a roster entry as the block value
        jmri.jmrit.roster.RosterEntry re = jmri.jmrit.roster.RosterEntry.fromFile(new java.io.File("java/test/jmri/jmrit/roster/ACL1012-Schema.xml"));

        // change the value of the block.
        block.setValue(re);

        // and verify the value is in the memory
        assertEquals( block.getValue(), mem.getValue(), "memory content same as block value");
    }

    @Test
    public void testSetMemoryFromIdTagBlockValue() {
        // initialize the layout block and the related automatic block
        layoutBlock.initializeLayoutBlock();

        // get a memory and associate it with the layout block.
        Memory mem = jmri.InstanceManager.getDefault(jmri.MemoryManager.class).provideMemory("IM1");

        layoutBlock.setMemory(mem,"IM1");

        // verify the memory is associated
        assertEquals( mem, layoutBlock.getMemory(), "memory saved");

        // Get the referenced block
        Block block = jmri.InstanceManager.getDefault(jmri.BlockManager.class).getByUserName("Test Block");
        Assertions.assertNotNull(block);

        jmri.IdTag tag = new jmri.implementation.DefaultIdTag("1234");

        // change the value of the block.
        block.setValue(tag);

        // and verify the value is in the memory
        assertEquals( block.getValue(), mem.getValue(), "memory content same as block value");
    }



    @Test
    @DisabledIfHeadless
    public void testValueChangeDoesNotRedrawPanel() {
        layoutBlock.initializeLayoutBlock();
        Block block = InstanceManager.getDefault(jmri.BlockManager.class).getByUserName("Test Block");
        assertNotNull(block);

        LayoutEditor panel = new LayoutEditor();
        try {
            panel.setAllEditable(false); // a panel in use, not being edited
            layoutBlock.addLayoutEditor(panel);
            List<String> redrawEvents = new ArrayList<>();
            layoutBlock.addPropertyChangeListener( evt -> {
                if (LayoutBlock.PROPERTY_REDRAW.equals(evt.getPropertyName())) {
                    redrawEvents.add(evt.getPropertyName());
                }
            });

            new QueueTool().waitEmpty(); // let the repaints of the setup settle first
            PanelRepaintRecorder recorder = new PanelRepaintRecorder(panel.getTargetPanel());
            RepaintManager.setCurrentManager(recorder);
            try {
                // the value of a block is displayed by icons which repaint themselves
                block.setValue("1234");
                new QueueTool().waitEmpty();
                assertEquals( List.of(), recorder.dirtyRegions,
                    "a value change does not repaint the panel");
                assertEquals( 1, redrawEvents.size(),
                    "a value change still notifies listeners of the redraw property");

                // occupancy changes the colour the track is drawn with, so it still does
                block.setState(Block.OCCUPIED);
                new QueueTool().waitEmpty();
                assertTrue( recorder.dirtyRegions.size() > 0,
                    "an occupancy change repaints the panel");

                // a panel being edited draws a rectangle around each block contents icon,
                // sized from the value, so it is still redrawn
                panel.setAllEditable(true);
                recorder.dirtyRegions.clear();
                block.setValue("5678");
                new QueueTool().waitEmpty();
                assertTrue( recorder.dirtyRegions.size() > 0,
                    "a value change repaints a panel being edited");
            } finally {
                RepaintManager.setCurrentManager(null);
            }
        } finally {
            JUnitUtil.dispose(panel);
        }
    }

    /**
     * Records the areas a panel is asked to repaint.
     */
    private static class PanelRepaintRecorder extends RepaintManager {

        private final JComponent watched;
        private final List<Rectangle> dirtyRegions = new ArrayList<>();

        PanelRepaintRecorder(JComponent watched) {
            this.watched = watched;
        }

        @Override
        public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
            if (c == watched) {
                dirtyRegions.add(new Rectangle(x, y, w, h));
            }
            super.addDirtyRegion(c, x, y, w, h);
        }
    }

    // from here down is testing infrastructure
    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        JUnitUtil.resetInstanceManager();
        JUnitUtil.initInternalSensorManager();
        // Create layout block and the related automatic block
        layoutBlock = new LayoutBlock("ILB999", "Test Block");
    }

    @AfterEach
    public void tearDown() {
        layoutBlock.dispose();
        layoutBlock = null;
        JUnitUtil.deregisterBlockManagerShutdownTask();
        JUnitUtil.tearDown();
    }
    // private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LayoutBlockTest.class);
}
