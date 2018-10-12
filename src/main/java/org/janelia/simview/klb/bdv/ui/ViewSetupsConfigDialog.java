package org.janelia.simview.klb.bdv.ui;

import bdv.BigDataViewer;
import bdv.export.ProgressWriterConsole;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;
import net.miginfocom.swing.MigLayout;
import org.janelia.simview.klb.bdv.KlbPartitionResolver;
import org.janelia.simview.klb.bdv.KlbSpimDataAdapter;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

import spim.fiji.spimdata.SpimData2;

public class ViewSetupsConfigDialog extends JDialog implements ActionListener
{
    private final UIService uiService;
    private final NameTagPanel nameTagPanel;
    private final JTabbedPane tabPane = new JTabbedPane();
    private final JButton viewButton = new JButton( "View in Big Data Viewer" );
    private final JButton saveAndOpenXmlButton = new JButton( "Save XML and open in Big Data Viewer" );
    private final JButton saveXmlButton = new JButton( "Save XML" );
    private final JButton cancelButton = new JButton( "Cancel" );

    public ViewSetupsConfigDialog(final UIService uiService)
    {
        this.uiService = uiService;
        KlbPartitionResolver resolver = new KlbPartitionResolver();
        nameTagPanel = new NameTagPanel(this.uiService);

        tabPane.add( "Basic", nameTagPanel );

        viewButton.addActionListener( this );
        saveAndOpenXmlButton.addActionListener( this );
        saveXmlButton.addActionListener( this );
        cancelButton.addActionListener( this );

        getContentPane().setLayout( new MigLayout( "nogrid, fillx" ) );
        add( tabPane, "span, grow, wrap" );
        add( viewButton, "gaptop para" );
        add( saveAndOpenXmlButton );
        add( saveXmlButton );
        add( cancelButton );
        pack();

        // call onCancel() when cross is clicked
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
        addWindowListener( new WindowAdapter()
        {
            public void windowClosing( final WindowEvent e )
            {
                onCancel();
            }
        } );

        // call onCancel() on ESCAPE
        (( JPanel ) getContentPane()).registerKeyboardAction( new ActionListener()
        {
            public void actionPerformed( final ActionEvent e )
            {
                onCancel();
            }
        }, KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
    }

    private void onCancel()
    {
        dispose();
    }

    public KlbPartitionResolver getResolver()
    {
        if ( tabPane.getSelectedIndex() == 0 ) {
            return nameTagPanel.getResolver();
        }
        return null;
    }

    private String saveXML()
    {
        final File file = uiService.chooseFile(null, FileWidget.SAVE_STYLE);
        if (file == null)
            return null;

        String filePath = file.getAbsolutePath();
        if ( !filePath.endsWith( ".xml" ) ) {
            filePath += ".xml";
        }

        final KlbPartitionResolver resolver = getResolver();
        final KlbSpimDataAdapter spimDataAdapter = new KlbSpimDataAdapter( resolver );

        try {
            spimDataAdapter.writeXML( filePath );
            return filePath;
        } catch ( SpimDataException ex ) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog( this,
                    "Failed to save XML.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE );
        }

        return null;
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        final Object source = e.getSource();
        if ( source == cancelButton ) {
            onCancel();
        } else if ( source == saveXmlButton ) {
            final String filePath = saveXML();
            if ( filePath == null ) {

            } else {
                dispose();
            }
        } else if ( source == saveAndOpenXmlButton ) {
            final String filePath = saveXML();
            if ( filePath != null ) {
                try {
                    BigDataViewer.open( filePath, filePath, new ProgressWriterConsole(), ViewerOptions.options() );
                } catch ( SpimDataException ex ) {
                    ex.printStackTrace();
                }
            }
            dispose();
        } else if ( source == viewButton ) {
            final KlbPartitionResolver resolver = getResolver();
            final KlbSpimDataAdapter spimDataAdapter = new KlbSpimDataAdapter( resolver );
            final SpimData2 spimData = spimDataAdapter.createDataset();
            BigDataViewer.open( spimData, "", new ProgressWriterConsole(), ViewerOptions.options() );
            dispose();
        }
    }
}
