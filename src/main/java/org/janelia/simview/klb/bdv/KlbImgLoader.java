package org.janelia.simview.klb.bdv;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.*;
import mpicbg.spim.data.generic.sequence.*;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.volatiles.array.AbstractVolatileArray;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.janelia.simview.klb.KLB;
import spim.Threads;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KlbImgLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
    private final KlbPartitionResolver resolver;
    private final AbstractSequenceDescription< BasicViewSetup, BasicViewDescription< BasicViewSetup >, KlbImgLoader > seq;
    private final VolatileGlobalCellCache cache;
    private final HashMap< Integer, KlbSetupImgLoader > setupImgLoaders = new HashMap< Integer, KlbSetupImgLoader >();

    public KlbImgLoader( final KlbPartitionResolver resolver, final AbstractSequenceDescription< ?, ?, ? > seq )
    {
        this.resolver = resolver;
        this.seq = ( AbstractSequenceDescription< BasicViewSetup, BasicViewDescription< BasicViewSetup >, KlbImgLoader > ) seq;
        cache = new VolatileGlobalCellCache(
                seq.getTimePoints().size(),
                resolver.getNumViewSetups(),
                resolver.getMaxNumResolutionLevels(),
                Threads.numThreads()
        );
        for ( final BasicViewSetup viewSetup : seq.getViewSetupsOrdered() ) {
            final int id = viewSetup.getId();
            final Type type = this.resolver.getViewSetupImageType( id );
            System.out.println(type.getClass().getName());
            if ( type instanceof UnsignedByteType )
                setupImgLoaders.put( id, new KlbSetupImgLoader( id, new KlbVolatileArrayLoaderUInt8( this.resolver, cache ) ) );
            else if ( type instanceof UnsignedShortType )
                setupImgLoaders.put( id, new KlbSetupImgLoader( id, new KlbVolatileArrayLoaderUInt16( this.resolver, cache ) ) );
            else if ( type instanceof FloatType )
                setupImgLoaders.put( id, new KlbSetupImgLoader( id, new KlbVolatileArrayLoaderFloat32( this.resolver, cache ) ) );
            else
                throw new UnsupportedOperationException( "Unknown or unsupported type" );
        }
    }

    public KlbPartitionResolver getResolver()
    {
        return resolver;
    }

    @Override
    public KlbSetupImgLoader getSetupImgLoader( final int viewSetupId )
    {
        return setupImgLoaders.get( viewSetupId );
    }

    @Override
    public Cache getCache()
    {
        return cache;
    }


    public class KlbSetupImgLoader< T extends RealType< T > & NativeType< T >, V extends Volatile< T > & NativeType< V >, A extends AbstractVolatileArray< A > > implements ViewerSetupImgLoader< T, V >, MultiResolutionSetupImgLoader< T >
    {
        private final int viewSetupId;
        private final long[] imageSize = new long[ 3 ];
        private final int[] blockSize = new int[ 3 ];
        private final KlbVolatileArrayLoader< T, V, A > arrayLoader;
        private double[][] mipMapResolutions;
        private AffineTransform3D[] mipMapTransforms;

        // cached constructors to create linked types
        private Constructor< T > typeConstructor;
        private Constructor< V > volatileTypeConstructor;

        /**
         * This instance of KLB is used to load images completely and directly. It will use all available threads to
         * read the image, whereas the KLB instances in the BigDataViewer use 1 thread each.
         */
        private KLB klb;

        public KlbSetupImgLoader( final int viewSetupId, final KlbVolatileArrayLoader< T, V, A > arrayLoader )
        {
            this.viewSetupId = viewSetupId;
            this.arrayLoader = arrayLoader;
        }

        @Override
        public Dimensions getImageSize( final int timePointId, final int level )
        {
            return seq.getViewSetups().get( viewSetupId ).getSize();
        }

        @Override
        public Dimensions getImageSize( final int timePointId )
        {
            return getImageSize( timePointId, 0 );
        }

        @Override
        public VoxelDimensions getVoxelSize( final int timePointId )
        {
            return seq.getViewSetups().get( viewSetupId ).getVoxelSize();
        }

        @Override
        public RandomAccessibleInterval< T > getImage( final int timePointId, final int level, final ImgLoaderHint... hints )
        {
            if ( Arrays.asList( hints ).contains( ImgLoaderHints.LOAD_COMPLETELY ) ) {
                if ( klb == null ) {
                    klb = KLB.newInstance();
                    klb.setNumThreads( Threads.numThreads() );
                }
                try {
                    return klb.readFull( resolver.getFilePath( timePointId, viewSetupId, level ) );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
            final CachedCellImg< T, A > img = prepareCachedImage( timePointId, level, LoadingStrategy.BLOCKING );
            if ( typeConstructor == null ) {
                for ( final Constructor< ? > c : getImageType().getClass().getConstructors() ) {
                    typeConstructor = ( Constructor< T > ) c;
                    try {
                        final T linkedType = typeConstructor.newInstance( img );
                        img.setLinkedType( linkedType );
                        return img;
                    } catch ( Exception ex ) {
                        typeConstructor = null;
                    }
                }
            }
            try {
                final T linkedType = typeConstructor.newInstance( img );
                img.setLinkedType( linkedType );
                return img;
            } catch ( Exception ex ) {
                throw new IllegalArgumentException( "Could not instantiate linked type " + getImageType().getClass().getName() );
            }
        }

        @Override
        public RandomAccessibleInterval< T > getImage( final int timePointId, final ImgLoaderHint... hints )
        {
            return getImage( timePointId, 0, hints );
        }

        // adapted from bdv.img.hdf5.Hdf5ImageLoader by Tobias Pietzsch et al.
        @Override
        public RandomAccessibleInterval< FloatType > getFloatImage( final int timePointId, final int level, final boolean normalize, final ImgLoaderHint... hints )
        {
            final RandomAccessibleInterval< T > inImg = getImage( timePointId, level, hints );

            // Todo: just return inImg if it is already FloatType

            // create float img
            final FloatType f = new FloatType();
            final ImgFactory< FloatType > imgFactory;
            if ( Intervals.numElements( inImg ) <= Integer.MAX_VALUE ) {
                imgFactory = new ArrayImgFactory< FloatType >();
            } else {
                getImageSize( timePointId, level ).dimensions( imageSize );
                getBlockSize( timePointId, level );
                imgFactory = new CellImgFactory< FloatType >( blockSize );
            }
            final Img< FloatType > floatImg = imgFactory.create( inImg, f );

            // set up executor service
            final int numProcessors = Runtime.getRuntime().availableProcessors();
            final ExecutorService taskExecutor = Executors.newFixedThreadPool( numProcessors );
            final ArrayList< Callable< Void > > tasks = new ArrayList< Callable< Void > >();

            // set up all tasks
            final int numPortions = Threads.numThreads();
            final long threadChunkSize = floatImg.size() / numPortions;
            final long threadChunkMod = floatImg.size() % numPortions;

            for ( int portionID = 0; portionID < numPortions; ++portionID ) {
                // move to the starting position of the current thread
                final long startPosition = portionID * threadChunkSize;

                // the last thread may has to run longer if the number of pixels cannot be divided by the number of threads
                final long loopSize = (portionID == numPortions - 1) ? threadChunkSize + threadChunkMod : threadChunkSize;

                if ( Views.iterable( inImg ).iterationOrder().equals( floatImg.iterationOrder() ) ) {
                    tasks.add( new Callable< Void >()
                    {
                        @Override
                        public Void call() throws Exception
                        {
                            final Cursor< T > in = Views.iterable( inImg ).cursor();
                            final Cursor< FloatType > out = floatImg.cursor();

                            in.jumpFwd( startPosition );
                            out.jumpFwd( startPosition );

                            for ( long j = 0; j < loopSize; ++j )
                                out.next().set( in.next().getRealFloat() );

                            return null;
                        }
                    } );
                } else {
                    tasks.add( new Callable< Void >()
                    {
                        @Override
                        public Void call() throws Exception
                        {
                            final Cursor< T > in = Views.iterable( inImg ).localizingCursor();
                            final RandomAccess< FloatType > out = floatImg.randomAccess();

                            in.jumpFwd( startPosition );

                            for ( long j = 0; j < loopSize; ++j ) {
                                final T vin = in.next();
                                out.setPosition( in );
                                out.get().set( vin.getRealFloat() );
                            }

                            return null;
                        }
                    } );
                }
            }

            try {
                // invokeAll() returns when all tasks are complete
                taskExecutor.invokeAll( tasks );
                taskExecutor.shutdown();
            } catch ( final InterruptedException e ) {
                return null;
            }

            if ( normalize )
                // normalize the image to 0...1
                normalize( floatImg );

            return floatImg;
        }

        @Override
        public RandomAccessibleInterval< FloatType > getFloatImage( final int timePointId, final boolean normalize, final ImgLoaderHint... hints )
        {
            return getFloatImage( timePointId, 0, normalize, hints );
        }

        @Override
        public RandomAccessibleInterval< V > getVolatileImage( final int timePointId, final int level, final ImgLoaderHint... hints )
        {
            final CachedCellImg< V, A > img = prepareCachedImage( timePointId, level, LoadingStrategy.VOLATILE );
            if ( volatileTypeConstructor == null ) {
                for ( final Constructor< ? > c : getVolatileImageType().getClass().getConstructors() ) {
                    volatileTypeConstructor = ( Constructor< V > ) c;
                    try {
                        final V linkedType = volatileTypeConstructor.newInstance( img );
                        img.setLinkedType( linkedType );
                        return img;
                    } catch ( Exception ex ) {
                        volatileTypeConstructor = null;
                    }
                }
            }
            try {
                final V linkedType = volatileTypeConstructor.newInstance( img );
                img.setLinkedType( linkedType );
                return img;
            } catch ( Exception ex ) {
                throw new IllegalArgumentException( "Could not instantiate linked type " + getVolatileImageType().getClass().getName() );
            }
        }

        private < T extends NativeType< T > > CachedCellImg< T, A > prepareCachedImage( final int timePointId, final int level, final LoadingStrategy loadingStrategy )
        {
            getImageSize( timePointId, level ).dimensions( imageSize );
            getBlockSize( timePointId, level );
            final int priority = resolver.getNumResolutionLevels( viewSetupId ) - 1 - level;
            final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
            final VolatileImgCells.CellCache< A > c = cache.new VolatileCellCache( timePointId, viewSetupId, level, cacheHints, arrayLoader );
            final VolatileImgCells< A > cells = new VolatileImgCells< A >( c, new Fraction(), imageSize, blockSize );
            return new CachedCellImg< T, A >( cells );
        }

        // copied from bdv.img.hdf5.Hdf5ImageLoader by Tobias Pietzsch et al.
        private void normalize( final IterableInterval< FloatType > img )
        {
            float currentMax = img.firstElement().get();
            float currentMin = currentMax;
            for ( final FloatType t : img ) {
                final float f = t.get();
                if ( f > currentMax )
                    currentMax = f;
                else if ( f < currentMin )
                    currentMin = f;
            }

            final float scale = ( float ) (1.0 / (currentMax - currentMin));
            for ( final FloatType t : img )
                t.set( (t.get() - currentMin) * scale );
        }

        @Override
        public int numMipmapLevels()
        {
            return resolver.getNumResolutionLevels( viewSetupId );
        }

        @Override
        public double[][] getMipmapResolutions()
        {
            if ( mipMapResolutions == null ) {
                mipMapResolutions = new double[ resolver.getNumResolutionLevels( viewSetupId ) ][ 3 ];
                for ( int level = 0; level < mipMapResolutions.length; ++level ) {
                    resolver.getSampling( resolver.getFirstTimePoint(), viewSetupId, level, mipMapResolutions[ level ] );
                }
            }
            return mipMapResolutions;
        }

        @Override
        public AffineTransform3D[] getMipmapTransforms()
        {
            if ( mipMapTransforms == null ) {
                mipMapTransforms = new AffineTransform3D[ resolver.getNumResolutionLevels( viewSetupId ) ];
                for ( int level = 0; level < mipMapTransforms.length; ++level ) {
                    mipMapTransforms[ level ] = new AffineTransform3D();
                }
            }
            return mipMapTransforms;
        }

        @Override
        public T getImageType()
        {
            return arrayLoader.getType();
        }

        @Override
        public V getVolatileImageType()
        {
            return arrayLoader.getVolatileType();
        }

        private void getBlockSize( final int timePointId, final int level )
        {
            if ( blockSize[ 0 ] == 0 ) {
                if ( !resolver.getBlockDimensions( timePointId, viewSetupId, level, blockSize ) ) {
                    final Map< Integer, TimePoint > timePoints = seq.getTimePoints().getTimePoints();
                    for ( final Integer t : timePoints.keySet() ) {
                        if ( resolver.getBlockDimensions( timePoints.get( t ).getId(), viewSetupId, level, blockSize ) ) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
