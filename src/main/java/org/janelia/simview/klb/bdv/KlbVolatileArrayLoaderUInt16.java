package org.janelia.simview.klb.bdv;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

import java.io.IOException;

public class KlbVolatileArrayLoaderUInt16 extends KlbVolatileArrayLoader< UnsignedShortType, VolatileUnsignedShortType, VolatileShortArray >
{
    private final UnsignedShortType type = new UnsignedShortType();
    private final VolatileUnsignedShortType volatileType = new VolatileUnsignedShortType();
    private VolatileShortArray theEmptyArray = new VolatileShortArray( 96 * 96 * 8, false );

    public KlbVolatileArrayLoaderUInt16( final KlbPartitionResolver resolver )
    {
        super( resolver );
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
            final String filePath,
            final long[] xyzctMin,
            final long[] xyzctMax,
            final int numElements
    )
            throws InterruptedException
    {
        final short[] buffer = new short[ numElements ];
        try {
            klb.readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return new VolatileShortArray( buffer, true );
        } catch ( IOException ex ) {
            return new VolatileShortArray( buffer, true );
        }
    }
}
