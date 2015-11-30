package org.janelia.simview.klb.bdv;

import bdv.img.cache.VolatileGlobalCellCache;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;

import java.io.IOException;

public class KlbVolatileArrayLoaderUInt8 extends KlbVolatileArrayLoader< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray >
{
    private final UnsignedByteType type = new UnsignedByteType();
    private final VolatileUnsignedByteType volatileType = new VolatileUnsignedByteType();
    private VolatileByteArray theEmptyArray = new VolatileByteArray( 96 * 96 * 8, false );

    public KlbVolatileArrayLoaderUInt8( final KlbPartitionResolver resolver, final VolatileGlobalCellCache cache )
    {
        super( resolver, cache );
    }

    @Override
    public UnsignedByteType getType()
    {
        return type;
    }

    @Override
    public VolatileUnsignedByteType getVolatileType()
    {
        return volatileType;
    }

    @Override
    public int getBytesPerElement()
    {
        return 1;
    }

    @Override
    protected VolatileByteArray tryLoadArray(
            final int timePoint,
            final int viewSetup,
            final int level,
            final int[] dimensions,
            final long[] offset
    )
            throws InterruptedException
    {
        final byte[] buffer = new byte[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
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
            return new VolatileByteArray( buffer, true );
        } catch ( IOException ex ) {
            return new VolatileByteArray( buffer, true );
        }
    }

    @Override
    public VolatileByteArray emptyArray( final int[] dimensions )
    {
        int numEntities = 1;
        for ( int d : dimensions )
            numEntities *= d;
        if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
            theEmptyArray = new VolatileByteArray( numEntities, false );
        return theEmptyArray;
    }
}
