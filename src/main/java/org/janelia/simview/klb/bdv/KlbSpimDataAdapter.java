package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import spim.fiji.datasetmanager.MultiViewDatasetDefinition;
import spim.fiji.spimdata.SpimData2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Interfaces a KlbPartitionResolver instance with Fiji's SpimData
 * dataset layout description. Can be consumed directly or serialized
 * to XML.
 */
public class KlbSpimDataAdapter implements MultiViewDatasetDefinition
{

    private final KlbPartitionResolver resolver;

    public KlbSpimDataAdapter( final KlbPartitionResolver resolver )
    {
        this.resolver = resolver;
    }

    @Override
    public String getTitle()
    {
        return "KLB Dataset";
    }

    @Override
    public String getExtendedDescription()
    {
        return "KLB Dataset";
    }

    @Override
    public SpimData2 createDataset()
    {
        // ViewSetups
        final long[] imageSize = new long[ 3 ];
        final double[] pixelSpacing = new double[ 3 ];
        int firstTimePoint = Integer.MAX_VALUE, lastTimePoint = Integer.MIN_VALUE;
        final HashMap< Integer, ViewSetup > viewSetups = new HashMap< Integer, ViewSetup >();
        for ( int viewSetupId = 0; viewSetupId < resolver.getNumViewSetups(); ++viewSetupId ) {
            resolver.getImageSize( viewSetupId, 0, imageSize );
            resolver.getPixelSpacing( viewSetupId, 0, pixelSpacing );
            final KlbPartitionResolver.KlbViewSetupConfig setupConfig = resolver.getViewSetupConfig( viewSetupId );
            final List< Integer > timePoints = setupConfig.getTimePoints();
            if ( timePoints != null && !timePoints.isEmpty() ) {
                Collections.sort( timePoints );
                firstTimePoint = Math.min( timePoints.get( 0 ), firstTimePoint );
                lastTimePoint = Math.max( timePoints.get( timePoints.size() - 1 ), lastTimePoint );
            }
            viewSetups.put( viewSetupId, new ViewSetup(
                    viewSetupId, setupConfig.getName(),
                    new FinalDimensions( imageSize ),
                    new FinalVoxelDimensions( "\u00B5m", pixelSpacing ),
                    setupConfig.getChannel(),
                    setupConfig.getAngle(),
                    setupConfig.getIllumination() )
            );
        }
        if ( firstTimePoint > lastTimePoint ) {
            // no ViewSetup has configured time points
            firstTimePoint = lastTimePoint = 0;
        }

        // time points
        final HashMap< Integer, TimePoint > timePointMap = new HashMap< Integer, TimePoint >();
        for ( int t = firstTimePoint; t <= lastTimePoint; ++t ) {
            timePointMap.put( t, new TimePoint( t ) );
        }
        final TimePoints timePoints = new TimePoints( timePointMap );

        // missing views
        final List< ViewId > missingViewIds = new ArrayList< ViewId >();
        for ( int viewSetupId = 0; viewSetupId < resolver.getNumViewSetups(); ++viewSetupId ) {
            final KlbPartitionResolver.KlbViewSetupConfig setup = resolver.getViewSetupConfig( viewSetupId );
            if ( setup.getTimePoints() == null || setup.getTimePoints().isEmpty() ) {
                for ( int t = firstTimePoint + 1; t <= lastTimePoint; ++t ) {
                    missingViewIds.add( new ViewId( t, viewSetupId ) );
                }
            } else {
                final List ts = setup.getTimePoints();
                for ( int t = firstTimePoint; t <= lastTimePoint; ++t ) {
                    if ( !ts.contains( t ) ) {
                        missingViewIds.add( new ViewId( t, viewSetupId ) );
                    }
                }
            }
        }
        final MissingViews missingViews = missingViewIds.isEmpty() ? null : new MissingViews( missingViewIds );

        // combine all the above into a SequenceDescription, and a KlbImgLoader
        final SequenceDescription seq = new SequenceDescription(
                timePoints,
                viewSetups,
                null,
                missingViews );
        final KlbImgLoader loader = new KlbImgLoader( resolver, seq );
        seq.setImgLoader( loader );

        // transforms/registrations
        final HashMap< ViewId, ViewRegistration > registrations = new HashMap< ViewId, ViewRegistration >();
        for ( final ViewSetup viewSetup : seq.getViewSetupsOrdered() ) {
            final int viewSetupId = viewSetup.getId();
            resolver.getPixelSpacing( viewSetupId, 0, pixelSpacing );
            final double min = Math.min( Math.min( pixelSpacing[ 0 ], pixelSpacing[ 1 ] ), pixelSpacing[ 2 ] );
            for ( int d = 0; d < pixelSpacing.length; ++d ) {
                pixelSpacing[ d ] /= min;
            }
            final AffineTransform3D trafo = new AffineTransform3D();
            trafo.set(
                    pixelSpacing[ 0 ], 0, 0, 0,
                    0, pixelSpacing[ 1 ], 0, 0,
                    0, 0, pixelSpacing[ 2 ], 0
            );
            for ( final TimePoint timePoint : seq.getTimePoints().getTimePointsOrdered() ) {
                final int timePointId = timePoint.getId();
                final ViewId viewId = new ViewId( timePointId, viewSetupId );
                if ( !missingViewIds.contains( viewId ) ) {
                    registrations.put( viewId, new ViewRegistration( timePointId, viewSetupId, trafo ) );
                }
            }
        }

        // combine all the above into a SpimData2
        return new SpimData2( new File( System.getProperty( "user.home" ) ), seq, new ViewRegistrations( registrations ), null, null );
    }

    @Override
    public MultiViewDatasetDefinition newInstance()
    {
        return new KlbSpimDataAdapter( null );
    }

    /**
     * Writes the dataset definition to XML.
     *
     * @param filePath file system path to write to
     */
    public void writeXML( final String filePath ) throws SpimDataException
    {
        final SpimData2 data = createDataset();
        data.setBasePath( new File( filePath ).getParentFile() );
        new XmlIoSpimData().save( data, filePath );
    }
}
