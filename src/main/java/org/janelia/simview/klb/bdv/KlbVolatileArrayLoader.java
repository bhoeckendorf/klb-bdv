package org.janelia.simview.klb.bdv;

import bdv.img.cache.CacheArrayLoader;
import net.imglib2.Volatile;
import net.imglib2.img.basictypeaccess.volatiles.array.AbstractVolatileArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.janelia.simview.klb.KLB;

public abstract class KlbVolatileArrayLoader< T extends RealType< T > & NativeType< T >, V extends Volatile< T > & NativeType< V >, A extends AbstractVolatileArray< A > > implements CacheArrayLoader< A >
{
    protected final KLB klb = KLB.newInstance();
    private final KlbPartitionResolver resolver;
    private int
            currentTimePoint = Integer.MIN_VALUE,
            currentLevel = Integer.MIN_VALUE;
    private String currentFilePath;

    public KlbVolatileArrayLoader( final KlbPartitionResolver resolver )
    {
        this.resolver = resolver;
        klb.setNumThreads( 1 );
    }

    public abstract T getType();

    public abstract V getVolatileType();

    @Override
    public A loadArray(
            final int timePoint,
            final int viewSetup,
            final int level,
            final int[] dimensions,
            final long[] offset
    )
            throws InterruptedException
    {
        // Cache current file path
        // viewSetup is always the same index, because instances of this class are not shared between SetupImgLoaders,
        // so we only need to worry about time and level
        if ( level != currentLevel || timePoint != currentTimePoint ) {
            currentFilePath = resolver.getFilePath( timePoint, viewSetup, level );
            currentLevel = level;
            currentTimePoint = timePoint;
        }
        return loadArray( currentFilePath,
                new long[]{ offset[ 0 ], offset[ 1 ], offset[ 2 ], 0, 0 },
                new long[]{
                        offset[ 0 ] + dimensions[ 0 ] - 1,
                        offset[ 1 ] + dimensions[ 1 ] - 1,
                        offset[ 2 ] + dimensions[ 2 ] - 1,
                        0, 0 },
                dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] );
    }

    public abstract A loadArray( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numElements )
            throws InterruptedException;
}
