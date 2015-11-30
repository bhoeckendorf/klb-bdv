package org.janelia.simview.klb.bdv;

import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import net.imglib2.Volatile;
import net.imglib2.img.basictypeaccess.volatiles.array.AbstractVolatileArray;
import org.janelia.simview.klb.KLB;

public abstract class KlbVolatileArrayLoader< T, V extends Volatile< T >, A extends AbstractVolatileArray< A > > implements CacheArrayLoader< A >
{
    protected final KLB klb = KLB.newInstance();
    protected final KlbPartitionResolver resolver;
    protected final VolatileGlobalCellCache cache;

    public KlbVolatileArrayLoader( final KlbPartitionResolver resolver, final VolatileGlobalCellCache cache )
    {
        this.resolver = resolver;
        this.cache = cache;
        klb.setNumThreads( 1 );
    }

    public abstract T getType();

    public abstract V getVolatileType();

    protected abstract A tryLoadArray(
            final int timePoint,
            final int viewSetup,
            final int level,
            final int[] dimensions,
            final long[] offset
    )
            throws InterruptedException;


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
        try {
            return tryLoadArray( timePoint, viewSetup, level, dimensions, offset );
        } catch ( final OutOfMemoryError e ) {
            cache.clearCache();
            System.gc();
            return tryLoadArray( timePoint, viewSetup, level, dimensions, offset );
        }
    }
}