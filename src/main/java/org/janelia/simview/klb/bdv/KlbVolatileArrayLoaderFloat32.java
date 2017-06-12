package org.janelia.simview.klb.bdv;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;

import java.io.IOException;

public class KlbVolatileArrayLoaderFloat32 extends KlbVolatileArrayLoader< FloatType, VolatileFloatType, VolatileFloatArray >
{
    private final FloatType type = new FloatType();
    private final VolatileFloatType volatileType = new VolatileFloatType();
    private VolatileFloatArray theEmptyArray = new VolatileFloatArray( 96 * 96 * 8, false );

    public KlbVolatileArrayLoaderFloat32( final KlbPartitionResolver resolver )
    {
        super( resolver );
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
            final String filePath,
            final long[] xyzctMin,
            final long[] xyzctMax,
            final int numElements
    )
            throws InterruptedException
    {
        final float[] buffer = new float[ numElements ];
        try {
            klb.readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return new VolatileFloatArray( buffer, true );
        } catch ( IOException ex ) {
            return new VolatileFloatArray( buffer, true );
        }
    }
}
