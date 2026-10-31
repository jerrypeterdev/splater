package com.partydamage;

import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

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
}
