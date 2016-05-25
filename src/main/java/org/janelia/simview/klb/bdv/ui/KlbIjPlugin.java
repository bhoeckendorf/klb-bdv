package org.janelia.simview.klb.bdv.ui;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin( type = Command.class, menuPath = "Plugins>BigDataViewer>Open KLB" )
public class KlbIjPlugin implements Command
{

    @Override
    public void run()
    {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        final ViewSetupsConfigDialog dialog = new ViewSetupsConfigDialog();
        dialog.setLocationRelativeTo( null );
        dialog.setVisible( true );
    }
}
