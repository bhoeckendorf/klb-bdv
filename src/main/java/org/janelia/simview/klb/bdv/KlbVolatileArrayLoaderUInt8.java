package org.janelia.simview.klb.bdv;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;

import java.io.IOException;

public class KlbVolatileArrayLoaderUInt8 extends KlbVolatileArrayLoader< UnsignedByteType, VolatileUnsignedByteType, VolatileByteArray >
{
    private final UnsignedByteType type = new UnsignedByteType();
    private final VolatileUnsignedByteType volatileType = new VolatileUnsignedByteType();
    private VolatileByteArray theEmptyArray = new VolatileByteArray( 96 * 96 * 8, false );

    public KlbVolatileArrayLoaderUInt8( final KlbPartitionResolver resolver )
    {
        super( resolver );
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
    public VolatileByteArray loadArray(
            final String filePath,
            final long[] xyzctMin,
            final long[] xyzctMax,
            final int numElements
    )
            throws InterruptedException
    {
        final byte[] buffer = new byte[ numElements ];
        try {
            klb.readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
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
