package org.janelia.simview.klb.bdv.ui;

import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.Tile;
import net.miginfocom.swing.MigLayout;
import org.janelia.simview.klb.KLB;
import org.janelia.simview.klb.bdv.KlbPartitionResolver;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NameTagPanel extends JPanel implements ActionListener
{
    private final KLB klb = KLB.newInstance();
    private final NameTagTableModel model;
    private final JTextField filePathEdit = new JTextField( Global.getCurrentOrDefaultPath() );
    private final JButton filePathButton = new JButton( "..." );
    private final JCheckBox overrideSamplingCheckbox = new JCheckBox( "Manually specify pixel spacing (\u00B5m)" );
    private final JCheckBox checkTimePointsCheckBox = new JCheckBox( "Check for (and handle) missing files" );
    private final JTextField
            pixelSpacingXEdit = new JTextField( "1.000" ),
            pixelSpacingYEdit = new JTextField( "1.000" ),
            pixelSpacingZEdit = new JTextField( "1.000" );
    private final JLabel
            pixelSpacingXLabel = new JLabel( "x" ),
            pixelSpacingYLabel = new JLabel( "y" ),
            pixelSpacingZLabel = new JLabel( "z" );

    public NameTagPanel()
    {
        model = new NameTagTableModel();
        final JTable table = new JTable( model );
        filePathButton.addActionListener( this );
        overrideSamplingCheckbox.addActionListener( this );

        pixelSpacingXLabel.setEnabled( false );
        pixelSpacingYLabel.setEnabled( false );
        pixelSpacingZLabel.setEnabled( false );
        pixelSpacingXEdit.setEnabled( false );
        pixelSpacingYEdit.setEnabled( false );
        pixelSpacingZEdit.setEnabled( false );
        pixelSpacingYEdit.setEditable( false );
        pixelSpacingXEdit.setEditable( false );
        pixelSpacingZEdit.setEditable( false );

        setLayout( new MigLayout( "nogrid, fillx" ) );
        add( new JLabel( "Template file" ) );
        add( filePathEdit, "grow" );
        add( filePathButton, "wrap" );
        add( new JLabel( "Name tags that are blank or not found in template file path will be ignored." ), "gaptop para, wrap" );
        add( new JScrollPane( table ), "grow, wrap" );
        add( overrideSamplingCheckbox, "gaptop para" );
        add( new JLabel( "x" ), "gap unrelated" );
        add( pixelSpacingXEdit, "gap related, growx" );
        add( new JLabel( "y" ), "gap unrelated" );
        add( pixelSpacingYEdit, "gap related, growx" );
        add( new JLabel( "z" ), "gap unrelated" );
        add( pixelSpacingZEdit, "gap related, growx, wrap" );
        add( checkTimePointsCheckBox, "gaptop para" );
    }

    private void updatePixelSpacing()
    {
        if ( overrideSamplingCheckbox.isSelected() ) {
            return;
        }
        final String filePath = filePathEdit.getText().trim();
        try {
            final KLB.Header header = klb.readHeader( filePath );
            pixelSpacingXEdit.setText( String.format( "%.3f", header.pixelSpacing[ 0 ] ) );
            pixelSpacingYEdit.setText( String.format( "%.3f", header.pixelSpacing[ 1 ] ) );
            pixelSpacingZEdit.setText( String.format( "%.3f", header.pixelSpacing[ 2 ] ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public KlbPartitionResolver getResolver()
    {
        final KlbPartitionResolver resolver = new KlbPartitionResolver();

        final int angleRow = 0;
        final int tileRow = 1;
        final int channelRow = 2;
        final int illuminationRow = 3;
        final int timeRow = 4;

        for ( int angleId = model.getFirstIndex( angleRow ); angleId <= model.getLastIndex( angleRow ); angleId += model.getIndexStride( angleRow ) )
        {
            resolver.addAngle( new Angle( angleId ) );
        }
        for ( int tileId = model.getFirstIndex( tileRow ); tileId <= model.getLastIndex( tileRow ); tileId += model.getIndexStride( tileRow ) )
        {
            resolver.addTile( new Tile( tileId ) );
        }
        for ( int channelId = model.getFirstIndex( channelRow ); channelId <= model.getLastIndex( channelRow ); channelId += model.getIndexStride( channelRow ) )
        {
            resolver.addChannel( new Channel( channelId ) );
        }
        for ( int illuminationId = model.getFirstIndex( illuminationRow ); illuminationId <= model.getLastIndex( illuminationRow ); illuminationId += model.getIndexStride( illuminationRow ) )
        {
            resolver.addIllumination( new Illumination( illuminationId ) );
        }

        final String templateFilePath = filePathEdit.getText().trim();
        final String angleTag = model.getValueAt( angleRow, 1 ).toString();
        final String tileTag = model.getValueAt( tileRow, 1).toString();
        final String channelTag = model.getValueAt( channelRow, 1 ).toString();
        final String illuminationTag = model.getValueAt( illuminationRow, 1 ).toString();
        final String timeTag = model.getValueAt( timeRow, 1 ).toString();

        final String[] angleMatchAndFormat = angleTag.isEmpty() ? null : resolver.getTagMatchAndFormat( templateFilePath, angleTag );
        final String[] tileMatchAndFormat = tileTag.isEmpty() ? null : resolver.getTagMatchAndFormat( templateFilePath, tileTag );
        final String[] channelMatchAndFormat = channelTag.isEmpty() ? null : resolver.getTagMatchAndFormat( templateFilePath, channelTag );
        final String[] illuminationMatchAndFormat = illuminationTag.isEmpty() ? null : resolver.getTagMatchAndFormat( templateFilePath, illuminationTag );
        final String[] timeMatchAndFormat = timeTag.isEmpty() ? null : resolver.getTagMatchAndFormat( templateFilePath, timeTag );

        for ( final Angle angle : ( List< Angle > ) resolver.getAngles() ) {
            String filePath = templateFilePath;
            if ( angleMatchAndFormat != null ) {
                filePath = filePath.replaceAll( angleMatchAndFormat[ 0 ], String.format( angleMatchAndFormat[ 1 ], angle.getId() ) );
            }
            for ( final Tile tile : ( List< Tile > ) resolver.getTiles() ) {
                if ( tileMatchAndFormat != null ) {
                    final String newField = String.format( tileMatchAndFormat[ 1 ], tile.getId() );
                    filePath = filePath.replaceAll( tileMatchAndFormat[ 0 ], newField );
                    tileMatchAndFormat[ 0 ] = newField;
                }
                for (final Channel channel : (List<Channel>) resolver.getChannels()) {
                    if (channelMatchAndFormat != null) {
                        final String newField = String.format(channelMatchAndFormat[1], channel.getId());
                        filePath = filePath.replaceAll(channelMatchAndFormat[0], newField);
                        channelMatchAndFormat[0] = newField;
                    }
                    for (final Illumination illumination : (List<Illumination>) resolver.getIlluminations()) {
                        if (illuminationMatchAndFormat != null) {
                            final String newField = String.format(illuminationMatchAndFormat[1], illumination.getId());
                            filePath = filePath.replaceAll(illuminationMatchAndFormat[0], newField);
                            illuminationMatchAndFormat[0] = newField;
                        }
                        KlbPartitionResolver.KlbViewSetupConfig setup = null;
                        if (timeMatchAndFormat != null) {
                            setup = resolver.addViewSetup(filePath, timeTag);
                        } else {
                            setup = resolver.addViewSetup(filePath);
                        }
                        if (setup == null) {
                            continue;
                        }
                        setup.setName("" + setup.getId());
                        setup.setAngleId(angle.getId());
                        setup.setTileId( tile.getId() );
                        setup.setChannelId(channel.getId());
                        setup.setIlluminationId(illumination.getId());
                    }
                }
            }
        }

        final List< Integer > timePoints = new ArrayList< Integer >();
        for ( int t = Integer.parseInt( model.getValueAt( 4, 2 ).toString() ); t <= Integer.parseInt( model.getValueAt( 4, 3 ).toString() ); t += Integer.parseInt( model.getValueAt( 4, 4 ).toString() ) )
        {
            timePoints.add( t );
        }
        for ( final KlbPartitionResolver.KlbViewSetupConfig config : ( List< KlbPartitionResolver.KlbViewSetupConfig > ) resolver.getViewSetupConfigs() ) {
            if ( checkTimePointsCheckBox.isSelected() ) {
                final List< Integer > tps = new ArrayList< Integer >();
                for ( final int t : timePoints ) {
                    final String fp = config.getFilePath( t );
                    if ( new File( fp ).exists() ) {
                        tps.add( t );
                    }
                }
                config.setTimePoints( tps );
            } else {
                config.setTimePoints( timePoints );
            }
        }
        if ( overrideSamplingCheckbox.isSelected() ) {
            final double[] pixelSpacing = new double[ 3 ];
            pixelSpacing[ 0 ] = Double.parseDouble( pixelSpacingXEdit.getText() );
            pixelSpacing[ 1 ] = Double.parseDouble( pixelSpacingYEdit.getText() );
            pixelSpacing[ 2 ] = Double.parseDouble( pixelSpacingZEdit.getText() );
            for ( final KlbPartitionResolver.KlbViewSetupConfig config : ( List< KlbPartitionResolver.KlbViewSetupConfig > ) resolver.getViewSetupConfigs() ) {
                config.setPixelSpacing( pixelSpacing );
            }
        }
        return resolver;
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        final Object source = e.getSource();
        if ( source == filePathButton ) {
            final JFileChooser chooser = new JFileChooser( Global.getCurrentOrDefaultPath( filePathEdit.getText().trim() ) );
            chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
            if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                final String filePath = chooser.getSelectedFile().getAbsolutePath();
                filePathEdit.setText( filePath );
                Global.updateCurrentPath( filePath );
                model.updateTemplate();
                updatePixelSpacing();
            }
        } else if ( source == overrideSamplingCheckbox ) {
            if ( overrideSamplingCheckbox.isSelected() ) {
                pixelSpacingXLabel.setEnabled( true );
                pixelSpacingYLabel.setEnabled( true );
                pixelSpacingZLabel.setEnabled( true );
                pixelSpacingXEdit.setEnabled( true );
                pixelSpacingYEdit.setEnabled( true );
                pixelSpacingZEdit.setEnabled( true );
                pixelSpacingYEdit.setEditable( true );
                pixelSpacingXEdit.setEditable( true );
                pixelSpacingZEdit.setEditable( true );
            } else {
                updatePixelSpacing();
                pixelSpacingXLabel.setEnabled( false );
                pixelSpacingYLabel.setEnabled( false );
                pixelSpacingZLabel.setEnabled( false );
                pixelSpacingXEdit.setEnabled( false );
                pixelSpacingYEdit.setEnabled( false );
                pixelSpacingZEdit.setEnabled( false );
                pixelSpacingYEdit.setEditable( false );
                pixelSpacingXEdit.setEditable( false );
                pixelSpacingZEdit.setEditable( false );
            }
        }
    }


    private class NameTagTableModel extends DefaultTableModel
    {
        // This instance exists solely to gain access to its 'getTagMatchAndFormat' function
        private final KlbPartitionResolver resolver = new KlbPartitionResolver();
        private final String[] columnHeaders = {
                "Dimension", "File Path Tag", "First", "Last", "Stride" };
        private final Object[][] data = {
                { "Angle", "Tile", "Color channel", "Illumination", "Time" },
                { "CM", "SPM", "CHN", "", "TM" },
                { "", "", "", "", "" },
                { "", "", "", "", "" },
                { "", "", "", "", "" }
        };

        public void updateTemplate()
        {
            for ( int row = 0; row < getRowCount(); ++row ) {
                updateTemplate( row );
            }
        }

        private void updateTemplate( final int row )
        {
            final String tagValue = getValueAt( row, 1 ).toString();
            if ( tagValue.isEmpty() ) {
                for ( int col = 2; col < getColumnCount(); ++col ) {
                    setValueAt( "n/a", row, col );
                }
                return;
            }
            final String template = filePathEdit.getText().trim();
            final String[] matchAndFormat = resolver.getTagMatchAndFormat( template, tagValue );
            if ( matchAndFormat == null ) {
                for ( int col = 2; col < getColumnCount(); ++col ) {
                    setValueAt( "n/a", row, col );
                }
                return;
            }
            final int last = Integer.parseInt( matchAndFormat[ 0 ].substring( tagValue.length() ) );
            if ( getValueAt( row, 2 ).toString().isEmpty() || getValueAt( row, 2 ).toString().equals( "n/a" ) ) {
                setValueAt( 0, row, 2 );
            }
            setValueAt( last, row, 3 );
            if ( getValueAt( row, 4 ).toString().isEmpty() || getValueAt( row, 4 ).toString().equals( "n/a" ) ) {
                setValueAt( 1, row, 4 );
            }
        }

        @Override
        public int getColumnCount()
        {
            return 5; //columnHeaders.length;
        }

        @Override
        public int getRowCount()
        {
            return 5; //data[0].length;
        }

        @Override
        public String getColumnName( final int column )
        {
            return columnHeaders[ column ];
        }

        @Override
        public void setValueAt( final Object value, final int row, final int column )
        {
            data[ column ][ row ] = value;
            fireTableCellUpdated( row, column );
            if ( column == 1 ) {
                updateTemplate( row );
            } else if ( column == 2 || column == 3 ) {
                try {
                    final int first = getValueAt( row, column ) instanceof Integer ? ( Integer ) getValueAt( row, column ) : Integer.parseInt( getValueAt( row, column ).toString() );
                    final int last = getValueAt( row, column ) instanceof Integer ? ( Integer ) getValueAt( row, column ) : Integer.parseInt( getValueAt( row, column ).toString() );
                    if ( first > last ) {
                        setValueAt( last, 3, row );
                    }
                } catch ( NumberFormatException e ) {
                }
            }
        }

        @Override
        public Object getValueAt( final int row, final int column )
        {
            return data[ column ][ row ];
        }

        @Override
        public boolean isCellEditable( final int row, final int column )
        {
            return column != 0 && !getValueAt( row, column ).equals( "n/a" );
        }

        private int getIndex( final int row, final int column )
        {
            final Object value = getValueAt( row, column );
            if ( value instanceof Integer ) {
                return ( Integer ) value;
            }
            try {
                return Integer.parseInt( value.toString() );
            } catch ( NumberFormatException e ) {
                return column == 4 ? 1 : 0;
            }
        }

        public int getFirstIndex( final int row )
        {
            return getIndex( row, 2 );
        }

        public int getLastIndex( final int row )
        {
            return getIndex( row, 3 );
        }

        public int getIndexStride( final int row )
        {
            return getIndex( row, 4 );
        }
    }
}

