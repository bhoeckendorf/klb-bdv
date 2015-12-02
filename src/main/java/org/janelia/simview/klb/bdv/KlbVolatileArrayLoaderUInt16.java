package org.janelia.simview.klb.bdv;

import bdv.img.cache.VolatileGlobalCellCache;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

import java.io.IOException;

public class KlbVolatileArrayLoaderUInt16 extends KlbVolatileArrayLoader< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray >
{
    private final UnsignedShortType type = new UnsignedShortType();
    private final VolatileUnsignedShortType volatileType = new VolatileUnsignedShortType();
    private VolatileShortArray theEmptyArray = new VolatileShortArray( 96 * 96 * 8, false );

    public KlbVolatileArrayLoaderUInt16( final KlbPartitionResolver resolver, final VolatileGlobalCellCache cache )
    {
        super( resolver, cache );
    }

    @Override
    public UnsignedShortType getType()
    {
        return type;
    }

    @Override
    public VolatileUnsignedShortType getVolatileType()
    {
        return volatileType;
    }

    @Override
    public int getBytesPerElement()
    {
        return 2;
    }

    @Override
    public VolatileShortArray loadArray(
            final int timePoint,
            final int viewSetup,
            final int level,
            final int[] dimensions,
            final long[] offset
    )
            throws InterruptedException
    {
        final short[] buffer = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
        try {
            klb.readROIinPlace(
                    resolver.getFilePath( timePoint, viewSetup, level ),
                    new long[]{ offset[ 0 ], offset[ 1 ], offset[ 2 ], 0, 0 },
                    new long[]{
                            offset[ 0 ] + dimensions[ 0 ] - 1,
                            offset[ 1 ] + dimensions[ 1 ] - 1,
                            offset[ 2 ] + dimensions[ 2 ] - 1,
                            0, 0 },
                    buffer );
            return new VolatileShortArray( buffer, true );
        } catch ( IOException ex ) {
            return new VolatileShortArray( buffer, true );
        }
    }

    @Override
    public VolatileShortArray emptyArray( final int[] dimensions )
    {
        int numEntities = 1;
        for ( int d : dimensions )
            numEntities *= d;
        if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
            theEmptyArray = new VolatileShortArray( numEntities, false );
        return theEmptyArray;
    }
}
