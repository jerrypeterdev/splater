package com.partydamage;

import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import java.awt.Component;
import java.awt.Container;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PartyDamagePanelTest
{
    @Test public void panelCanBeConstructedBeforePluginFieldsAreInjected() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            PartyDamagePlugin plugin = new PartyDamagePlugin();
            PartyDamageConfig config = new PartyDamageConfig() { };
            assertNotNull(new PartyDamagePanel(plugin, config));
        });
    }

    @Test public void settingsCanBeExpandedAndCollapsed() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            PartyDamagePanel panel = new PartyDamagePanel(new PartyDamagePlugin(), new PartyDamageConfig() { });
            JButton toggle = find(panel, JButton.class, "SETTINGS  \u25b6");
            JCheckBox option = find(panel, JCheckBox.class, "Show damage dealt overlay");
            assertNotNull(toggle);
            assertNotNull(option);
            assertFalse(option.getParent().isVisible());
            toggle.doClick();
            assertTrue(option.getParent().isVisible());
            toggle.doClick();
            assertFalse(option.getParent().isVisible());
        });
    }

    private static <T extends javax.swing.AbstractButton> T find(Container root, Class<T> type, String label)
    {
        for (Component component : root.getComponents())
        {
            if (type.isInstance(component) && label.equals(type.cast(component).getText())) return type.cast(component);
            if (component instanceof Container)
            {
                T found = find((Container) component, type, label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
