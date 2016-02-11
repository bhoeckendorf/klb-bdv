package org.janelia.simview.klb.bdv.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SingleFilePathPanel extends JPanel implements ActionListener
{
    private static String currentPath = "";
    private final JButton button = new JButton( "..." );
    private final int selectionMode;
    private final JTextField textField = new JTextField( getCurrentOrDefaultPath() );

    // ToDo: use events rather than references
    private final NameTagPanel nameTagPanel;
    private final SpecifySamplingPanel samplingPanel;

    protected static String getCurrentOrDefaultPath( final String suggestion )
    {
        if ( suggestion == null || suggestion.trim().isEmpty() )
            return getCurrentOrDefaultPath();
        return suggestion;
    }

    protected static String getCurrentOrDefaultPath()
    {
        if ( currentPath.isEmpty() )
            return System.getProperty( "user.home" );
        return currentPath;
    }

    protected static void updateCurrentPath( final String path )
    {
        currentPath = path;
    }

    public SingleFilePathPanel( final String label, final int fileChooserSelectionMode, final NameTagPanel nameTagPanel, final SpecifySamplingPanel samplingPanel )
    {
        this.nameTagPanel = nameTagPanel;
        this.samplingPanel = samplingPanel;

        selectionMode = fileChooserSelectionMode;
        textField.setEditable( false );

        button.addActionListener( this );

        setLayout( new MigLayout( "", "[][grow][]", "[]" ) );
        add( new JLabel( label ), "cell 0 0" );
        add( textField, "cell 1 0, grow" );
        add( button, "cell 2 0" );
    }

    public String getFilePath()
    {
        return textField.getText();
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        if ( e.getSource() == button ) {
            final JFileChooser chooser = new JFileChooser( getCurrentOrDefaultPath( textField.getText() ) );
            chooser.setFileSelectionMode( selectionMode );
            if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                final String filePath = chooser.getSelectedFile().getAbsolutePath();
                textField.setText( filePath );
                updateCurrentPath( filePath );

                nameTagPanel.updateTemplate( filePath );
                if ( !samplingPanel.isSamplingSpecified() ) {
                    samplingPanel.updateSampling( filePath );
                }
            }
        }
    }
}
