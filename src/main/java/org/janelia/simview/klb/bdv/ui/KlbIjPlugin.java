package org.janelia.simview.klb.bdv.ui;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.*;

@Plugin( type = Command.class, menuPath = "Plugins>BigDataViewer>Open KLB" )
public class KlbIjPlugin implements Command
{
    @Parameter
    private UIService uiService;

    @Override
    public void run()
    {
        /*try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }*/

        final ViewSetupsConfigDialog dialog = new ViewSetupsConfigDialog(uiService);
        dialog.setLocationRelativeTo( null );
        dialog.setVisible( true );
    }
}
