package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.Tile;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.janelia.simview.klb.KLB;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines a data set consisting of one or multiple KLB files.
 * <p>
 * Interfaces with Fiji's Big Data Viewer and SPIM plugins through
 * KlbSpimDataAdapter, which generates a SpimData2 instance that can
 * be consumed by Fiji and/or saved to XML.
 * <p>
 * Returns metadata that is apparent from the file system, such as the
 * number of resolution levels.
 * <p>
 * Retrieves basic image-related metadata (image dimensions, block
 * dimensions, pixel spacing).
 * <p>
 * Uses a user-defined path name tag pattern.
 */
public class KlbPartitionResolver< T extends RealType< T > & NativeType< T > >
{
    private final KLB klb = KLB.newInstance();
    private final List< KlbViewSetupConfig > viewSetupConfigs = new ArrayList< KlbViewSetupConfig >();
    private final Map< Integer, Angle > angles = new HashMap< Integer, Angle >();
    private final Map< Integer, Tile > tiles = new HashMap< Integer, Tile >();
    private final Map< Integer, Channel > channels = new HashMap< Integer, Channel >();
    private final Map< Integer, Illumination > illuminations = new HashMap< Integer, Illumination >();

    /**
     * Add a multi file ViewSetup to the data set, each file is a time point
     *
     * @param timeSeriesTemplateFile absolute file system path to representative file of this ViewSetup
     * @param timeTag                tag in the file system path that denotes the time index of the file
     *                               e.g. "t" for "t001"
     * @return the newly added ViewSetup or null if its configuration failed for any reason,
     * e.g. template file does not exist or does not contain the value of timeTag in its file system path
     */
    public KlbViewSetupConfig addViewSetup( final String timeSeriesTemplateFile, final String timeTag )
    {
        final KlbViewSetupConfig setup = new KlbViewSetupConfig();
        if ( setup.setTimeSeriesTemplateFile( timeSeriesTemplateFile, timeTag ) ) {
            viewSetupConfigs.add( setup );
            return setup;
        }
        return null;
    }

    /**
     * Add a single file, single time point ViewSetup to the data set
     *
     * @param singleFile absolute file system path to file of this ViewSetup
     * @return the newly added ViewSetup or null if its configuration failed for any reason,
     * e.g. file does not exist
     */
    public KlbViewSetupConfig addViewSetup( final String singleFile )
    {
        final KlbViewSetupConfig setup = new KlbViewSetupConfig();
        if ( setup.setSingleFile( singleFile ) ) {
            viewSetupConfigs.add( setup );
            return setup;
        }
        return null;
    }

    public void removeViewSetup( final KlbViewSetupConfig config )
    {
        viewSetupConfigs.remove( config );
    }

    public void removeViewSetup( final int viewSetupId )
    {
        viewSetupConfigs.remove( viewSetupId );
    }

    public List< KlbViewSetupConfig > getViewSetupConfigs()
    {
        return viewSetupConfigs;
    }

    public int getNumViewSetups()
    {
        return viewSetupConfigs.size();
    }

    public KlbViewSetupConfig getViewSetupConfig( final int viewSetupId )
    {
        return viewSetupConfigs.get( viewSetupId );
    }

    public List< Angle > getAngles()
    {
        final List< Angle > list = new ArrayList< Angle >( angles.values() );
        Collections.sort( list );
        return list;
    }

    public Angle getAngle( final int angleId )
    {
        return angles.get( angleId );
    }

    /**
     * Add an Angle to the data set.
     * <p>
     * This is only needed when creating a new data set definition to be serialized to XML.
     *
     * @param angle the Angle
     */
    public void addAngle( final Angle angle )
    {
        angles.put( angle.getId(), angle );
    }

    public List< Channel > getChannels()
    {
        final List< Channel > list = new ArrayList< Channel >( channels.values() );
        Collections.sort( list );
        return list;
    }

    public List< Tile > getTiles()
    {
        final List< Tile > list = new ArrayList< Tile >( tiles.values() );
        Collections.sort( list );
        return list;
    }

    public Tile getTile( final int tileId )
    {
        return tiles.get( tileId );
    }

    public void addTile( final Tile tile )
    {
        tiles.put( tile.getId(), tile );
    }

    public Channel getChannel( final int channelId )
    {
        return channels.get( channelId );
    }

    /**
     * Add a color Channel to the data set.
     * <p>
     * This is only needed when creating a new data set definition to be serialized to XML.
     *
     * @param channel the color Channel
     */
    public void addChannel( final Channel channel )
    {
        channels.put( channel.getId(), channel );
    }

    public List< Illumination > getIlluminations()
    {
        final List< Illumination > list = new ArrayList< Illumination >( illuminations.values() );
        Collections.sort( list );
        return list;
    }

    public Illumination getIllumination( final int illuminationId )
    {
        return illuminations.get( illuminationId );
    }

    /**
     * Add an Illumination mode to the data set.
     * This is only needed when creating a new data set definition to be serialized to XML.
     *
     * @param illumination the Illumination mode
     */
    public void addIllumination( final Illumination illumination )
    {
        illuminations.put( illumination.getId(), illumination );
    }

    /**
     * Returns the number of available resolution levels for the
     * given ViewSetup.
     * Should be 1 (not 0) if only full resolution, original images
     * are available.
     *
     * @param viewSetup ViewSetup (channel) index
     * @return number of resolution levels
     */
    public int getNumResolutionLevels( final int viewSetup )
    {
        return viewSetupConfigs.get( viewSetup ).getNumResolutionLevels();
    }

    /**
     * Writes the image dimensions (x,y,z) of the defined ViewSetup and level into out.
     * Does not read it from file but uses a cached value.
     *
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     */
    public void getImageSize( final int viewSetup, final int level, final long[] out )
    {
        viewSetupConfigs.get( viewSetup ).getImageSize( level, out );
    }

    /**
     * Reads the header of the image defined by ViewSetup index, time point and level,
     * and writes the image dimensions (x,y,z) into out.
     * Returns false in case of failure (e.g. file not found).
     *
     * @param timePoint time point
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     * @return whether or not successful
     */
    public boolean getImageSize( final int timePoint, final int viewSetup, final int level, final long[] out )
    {
        return viewSetupConfigs.get( viewSetup ).getImageSize( timePoint, level, out );
    }

    /**
     * Writes the block dimensions (x,y,z) of the defined ViewSetup and level into out.
     * Does not read it from file but uses a cached value.
     *
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     */
    public void getBlockSize( final int viewSetup, final int level, final int[] out )
    {
        viewSetupConfigs.get( viewSetup ).getBlockSize( level, out );
    }

    /**
     * Reads the header of the image defined by ViewSetup index, time point and level,
     * and writes the block dimensions (x,y,z) into out.
     * Returns false in case of failure (e.g. file not found).
     *
     * @param timePoint time point
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     * @return whether or not successful
     */
    public boolean getBlockSize( final int timePoint, final int viewSetup, final int level, final int[] out )
    {
        return viewSetupConfigs.get( viewSetup ).getBlockSize( timePoint, level, out );
    }

    /**
     * Writes the pixel spacing (x,y,z) of the defined ViewSetup and level into out.
     * Does not read it from file but uses a cached value.
     *
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     */
    public void getPixelSpacing( final int viewSetup, final int level, final double[] out )
    {
        viewSetupConfigs.get( viewSetup ).getPixelSpacing( level, out );
    }

    /**
     * Reads the header of the image defined by ViewSetup index, time point and level,
     * and writes the pixel spacing (x,y,z, in microns) into out.
     * Returns false in case of failure (e.g. file not found).
     *
     * @param timePoint time point
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     * @return whether or not successful
     */
    public boolean getPixelSpacing( final int timePoint, final int viewSetup, final int level, final double[] out )
    {
        return viewSetupConfigs.get( viewSetup ).getPixelSpacing( timePoint, level, out );
    }

    public String getFilePath( final int timePoint, final int viewSetup, final int level )
    {
        return viewSetupConfigs.get( viewSetup ).getFilePath( timePoint, level );
    }

    public ImgPlus< T > getImage( final int timePoint, final int viewSetup, final int level )
            throws IOException
    {
        return viewSetupConfigs.get( viewSetup ).getImage( timePoint, level );
    }

    /**
     * Returns {"t045", "t%03d"} for input arguments "/path/to/dataset1t045channel7.klb", "t".
     * Is used to get the file path pattern of multi file ViewSetups and get appropriate paths to read files as needed.
     *
     * @param template
     * @param tag
     * @return
     */
    public String[] getTagMatchAndFormat( final String template, final String tag )
    {
        final Pattern pattern = Pattern.compile( String.format( "%s\\d+", tag ) );
        final Matcher matcher = pattern.matcher( template );
        if ( matcher.find() ) {
            final String tagMatch = template.substring( matcher.start(), matcher.end() );
            final String tagFormat = String.format( "%s%s%dd", tag, "%0", tagMatch.length() - tag.length() );
            return new String[]{ tagMatch, tagFormat };
        } else {
            return null;
        }
    }

    public List< Integer > getTimePoints( final int first, final int last )
    {
        return getTimePoints( first, last, 1 );
    }

    public List< Integer > getTimePoints( final int first, final int last, final int stride )
    {
        final List< Integer > timePoints = new ArrayList< Integer >();
        for ( int t = first; t <= last; t += stride ) {
            timePoints.add( t );
        }
        return timePoints;
    }

    /**
     * Turns a string to configure time points in the format "first[-last[:stride]]" into a List of Integers
     *
     * @param timePointString
     * @return
     */
    public List< Integer > getTimePoints( final String timePointString )
    {
        final List< Integer > timePoints = new ArrayList< Integer >();
        final Set< Integer > timePointSet = new HashSet< Integer >();
        final String[] timePeriods = timePointString.replaceAll( "\\s", "" ).split( "," );
        for ( final String timePeriod : timePeriods ) {
            final String[] extremaAndStride = timePeriod.split( ":" );
            if ( extremaAndStride.length < 1 || extremaAndStride.length > 2 ) {
                return timePoints;
            }
            final int stride = extremaAndStride.length == 2 ? Integer.parseInt( extremaAndStride[ 2 ] ) : 1;

            final String[] minAndMax = extremaAndStride[ 0 ].split( "-" );
            if ( minAndMax.length < 1 || minAndMax.length > 2 ) {
                return timePoints;
            }
            final int min = Integer.parseInt( minAndMax[ 0 ] );
            final int max = minAndMax.length == 2 ? Integer.parseInt( minAndMax[ 1 ] ) : min;

            for ( int t = min; t <= max; t += stride ) {
                timePointSet.add( t );
            }
        }
        timePoints.addAll( timePointSet );
        Collections.sort( timePoints );
        return timePoints;
    }


    /**
     * Stores the configuration of a single ViewSetup, instantiated via KlbPartitionResolver.addViewSetup(...)
     */
    public class KlbViewSetupConfig
    {
        private String filePathTemplate = "", indexTag = "";
        private final List< KLB.Header > headers = new ArrayList< KLB.Header >();
        private final double[] pixelSpacing = { 1, 1, 1 };

        private int angleId = -1, tileId = -1, channelId = -1, illuminationId = -1;
        private String name = "";
        private List< Integer > timePoints = null;

        private String tagMatch = null;
        private String tagFormat = null;

        private KlbViewSetupConfig()
        {
        }

        /**
         * A multi file ViewSetup to the data set, each file is a time point
         *
         * @param filePath absolute file system path to representative file of this ViewSetup
         * @param timeTag  tag in the file system path that denotes the time index of the file
         *                 e.g. "t" for "t001"
         * @return the newly added ViewSetup or null if its configuration failed for any reason,
         * e.g. template file does not exist or does not contain the value of timeTag in its file system path
         */
        public boolean setTimeSeriesTemplateFile( final String filePath, final String timeTag )
        {
            final int t = getTagMatchFormatAndIndex( filePath, timeTag );
            if ( tagMatch == null ) {
                return false;
            }

            KLB.Header header = null;
            try {
                header = klb.readHeader( filePath );
            } catch ( IOException ex ) {
                return false;
            }

            headers.clear();
            headers.add( header );

            filePathTemplate = filePath;
            this.indexTag = timeTag;
            setPixelSpacing( header.pixelSpacing );

            int level = 0;
            while ( true ) {
                try {
                    headers.add( klb.readHeader( getFilePath( t, ++level ) ) );
                } catch ( IOException ex ) {
                    break;
                }
            }
            return true;
        }

        /**
         * A single file, single time point ViewSetup to the data set
         *
         * @param filePath absolute file system path to file of this ViewSetup
         * @return the newly added ViewSetup or null if its configuration failed for any reason,
         * e.g. file does not exist
         */
        public boolean setSingleFile( final String filePath )
        {
            KLB.Header header = null;
            try {
                header = klb.readHeader( filePath );
            } catch ( IOException ex ) {
                return false;
            }

            headers.clear();
            headers.add( header );

            filePathTemplate = filePath;
            indexTag = tagMatch = tagFormat = null;
            timePoints = null;
            setPixelSpacing( header.pixelSpacing );

            final int t = 0;
            int level = 0;
            while ( true ) {
                try {
                    headers.add( klb.readHeader( getFilePath( t, ++level ) ) );
                } catch ( IOException ex ) {
                    break;
                }
            }
            return true;
        }

        private int getTagMatchFormatAndIndex( final String template, final String tag )
        {
            final String[] matchAndFormat = getTagMatchAndFormat( template, tag );
            if ( matchAndFormat == null ) {
                tagMatch = tagFormat = null;
                return -1;
            }
            tagMatch = matchAndFormat[ 0 ];
            tagFormat = matchAndFormat[ 1 ];
            return Integer.parseInt( tagMatch.substring( tag.length() ) );
        }

        /**
         * Set the time points for with this ViewSetup has data.
         * This is only needed when creating a new data set definition to be serialized to XML.
         *
         * @param tps
         */
        public void setTimePoints( final List< Integer > tps )
        {
            timePoints = tps;
        }

        public List< Integer > getTimePoints()
        {
            return timePoints;
        }

        public void setPixelSpacing( final double[] pixelSpacing )
        {
            System.arraycopy( pixelSpacing, 0, this.pixelSpacing, 0, this.pixelSpacing.length );
        }

        /**
         * Manually specify pixel spacing, if the true values are not stored in the KLB header
         *
         * @param pixelSpacing
         */
        public void setPixelSpacing( final float[] pixelSpacing )
        {
            for ( int i = 0; i < this.pixelSpacing.length; ++i ) {
                this.pixelSpacing[ i ] = pixelSpacing[ i ];
            }
        }

        public int getNumResolutionLevels()
        {
            return headers.size();
        }

        public String getFilePathTemplate()
        {
            return filePathTemplate;
        }

        public String getTimeTag()
        {
            return indexTag;
        }

        public String getFilePath( final int timePoint )
        {
            return getFilePath( filePathTemplate, timePoint, 0 );
        }

        public String getFilePath( final int timePoint, final int level )
        {
            return getFilePath( filePathTemplate, timePoint, level );
        }

        private String getFilePath( final String template, final int timePoint, final int level )
        {
            if ( tagMatch == null ) {
                return template;
            }
            String fn = template.replaceAll( tagMatch, String.format( tagFormat, timePoint ) );
            if ( level > 0 ) {
                fn = fn.replace( ".klb", String.format( ".RESLVL%d.klb", level ) ); // ToDo: replace last only
            }
            return fn;
        }

        public int getId()
        {
            return viewSetupConfigs.indexOf( this );
        }

        public String getName()
        {
            if ( name.isEmpty() )
                return String.valueOf( getId() );
            return name;
        }

        public void setName( final String name )
        {
            this.name = name;
        }

        public int getAngleId()
        {
            return angleId;
        }

        public void setAngleId( final int id )
        {
            angleId = id;
        }

        public Angle getAngle()
        {
            return angles.get( getAngleId() );
        }

        public int getTileId() {
            return tileId;
        }

        public void setTileId( final int id ) {
            tileId = id;
        }

        public Tile getTile() {
            return tiles.get( getTileId() );
        }

        public int getChannelId()
        {
            return channelId;
        }

        public void setChannelId( final int id )
        {
            channelId = id;
        }

        public Channel getChannel()
        {
            return channels.get( getChannelId() );
        }

        public int getIlluminationId()
        {
            return illuminationId;
        }

        public void setIlluminationId( final int id )
        {
            illuminationId = id;
        }

        public Illumination getIllumination()
        {
            return illuminations.get( getIlluminationId() );
        }

        public T getDataType()
        {
            return ( T ) headers.get( 0 ).dataType;
        }

        public void getImageSize( final int level, final long[] out )
        {
            System.arraycopy( headers.get( level ).imageSize, 0, out, 0, out.length );
        }

        public boolean getImageSize( final int timePoint, final int level, final long[] out )
        {
            try {
                final KLB.Header header = klb.readHeader( getFilePath( timePoint, level ) );
                System.arraycopy( header.imageSize, 0, out, 0, out.length );
                return true;
            } catch ( IOException ex ) {
                return false;
            }
        }

        public void getPixelSpacing( final int level, final double[] out )
        {
            if ( pixelSpacing != null && level == 0 ) {
                System.arraycopy( pixelSpacing, 0, out, 0, out.length );
            } else {
                // ToDo: This assumes that the true values are stored in the KLB header of all resolution levels >0
                final float[] pixelSpacing = headers.get( level ).pixelSpacing;
                for ( int i = 0; i < out.length; ++i ) {
                    out[ i ] = ( double ) pixelSpacing[ i ];
                }
            }
        }

        public boolean getPixelSpacing( final int timePoint, final int level, final double[] out )
        {
            try {
                final KLB.Header header = klb.readHeader( getFilePath( timePoint, level ) );
                final float[] pixelSpacing = header.pixelSpacing;
                for ( int i = 0; i < out.length; ++i ) {
                    out[ i ] = ( double ) pixelSpacing[ i ];
                }
                return true;
            } catch ( IOException ex ) {
                return false;
            }
        }

        public void getBlockSize( final int level, final int[] out )
        {
            final long[] blockSize = headers.get( level ).blockSize;
            for ( int i = 0; i < out.length; ++i ) {
                out[ i ] = ( int ) blockSize[ i ]; // Math.toIntExact( blockSize[i] ); requires Java 1.7
            }
        }

        public boolean getBlockSize( final int timePoint, final int level, final int[] out )
        {
            try {
                final KLB.Header header = klb.readHeader( getFilePath( timePoint, level ) );
                final long[] blockSize = header.blockSize;
                for ( int i = 0; i < out.length; ++i ) {
                    out[ i ] = ( int ) blockSize[ i ]; // Math.toIntExact( blockSize[i] ); requires Java 1.7
                }
                return true;
            } catch ( IOException ex ) {
                return false;
            }
        }

        public ImgPlus< T > getImage( final int timePoint, final int level )
                throws IOException
        {
            return klb.readFull( getFilePath( timePoint, level ) );
        }
    }
}
