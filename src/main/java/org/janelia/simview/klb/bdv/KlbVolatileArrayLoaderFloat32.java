package org.janelia.simview.klb.bdv;

import bdv.img.cache.VolatileGlobalCellCache;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;

import java.io.IOException;

public class KlbVolatileArrayLoaderFloat32 extends KlbVolatileArrayLoader< FloatType, VolatileFloatType, VolatileFloatArray >
{
    private final FloatType type = new FloatType();
    private final VolatileFloatType volatileType = new VolatileFloatType();
    private VolatileFloatArray theEmptyArray = new VolatileFloatArray( 96 * 96 * 8, false );

    public KlbVolatileArrayLoaderFloat32( final KlbPartitionResolver resolver, final VolatileGlobalCellCache cache )
    {
        super( resolver, cache );
    }

    @Override
    public FloatType getType()
    {
        return type;
    }

    @Override
    public VolatileFloatType getVolatileType()
    {
        return volatileType;
    }

    @Override
    public int getBytesPerElement()
    {
        return 4;
    }

    @Override
    public VolatileFloatArray loadArray(
            final int timePoint,
            final int viewSetup,
            final int level,
            final int[] dimensions,
            final long[] offset
    )
            throws InterruptedException
    {
        final float[] buffer = new float[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
        try {
            klb.readROIinPlace(
                    (( KlbPartitionResolverDefault ) resolver).getFilePath( timePoint, viewSetup, level ),
                    new long[]{ offset[ 0 ], offset[ 1 ], offset[ 2 ], 0, 0 },
                    new long[]{
                            offset[ 0 ] + dimensions[ 0 ] - 1,
                            offset[ 1 ] + dimensions[ 1 ] - 1,
                            offset[ 2 ] + dimensions[ 2 ] - 1,
                            0, 0 },
                    buffer );
            return new VolatileFloatArray( buffer, true );
        } catch ( IOException ex ) {
            return new VolatileFloatArray( buffer, true );
        }
    }

    @Override
    public VolatileFloatArray emptyArray( final int[] dimensions )
    {
        int numEntities = 1;
        for ( int d : dimensions )
            numEntities *= d;
        if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
            theEmptyArray = new VolatileFloatArray( numEntities, false );
        return theEmptyArray;
    }
}
