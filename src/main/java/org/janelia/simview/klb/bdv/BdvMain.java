package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.bdv.ui.ViewSetupsConfigDialog;

import java.awt.*;

public class BdvMain
{

    public static void main( final String[] args )
    {
        EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
                final ViewSetupsConfigDialog dialog = new ViewSetupsConfigDialog();
                dialog.setLocationRelativeTo( null );
                dialog.setVisible( true );
            }
        } );
    }
}
