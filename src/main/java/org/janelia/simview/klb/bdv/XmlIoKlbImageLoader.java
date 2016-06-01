package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;
import java.util.List;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

@ImgLoaderIo( format = "klb", type = KlbImgLoader.class )
public class XmlIoKlbImageLoader implements XmlIoBasicImgLoader< KlbImgLoader >
{

    @Override
    public Element toXml( final KlbImgLoader imgLoader, final File basePath )
    {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "klb" );
        elem.addContent( resolverToXml( imgLoader.getResolver() ) );
        return elem;
    }

    @Override
    public KlbImgLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
    {
        final KlbPartitionResolver resolver = resolverFromXml( elem.getChild( "Resolver" ) );
        return new KlbImgLoader( resolver, sequenceDescription );
    }

    private Element resolverToXml( final KlbPartitionResolver resolver )
    {
        final Element resolverElem = new Element( "Resolver" );
        final String type = resolver.getClass().getName();
        resolverElem.setAttribute( "type", type );
        final List< KlbPartitionResolver.KlbViewSetupConfig > setups = resolver.getViewSetupConfigs();
        for ( final KlbPartitionResolver.KlbViewSetupConfig setup : setups ) {
            final Element templateElem = new Element( "ViewSetupTemplate" );
            templateElem.addContent( XmlHelpers.textElement( "template", setup.getFilePathTemplate() ) );
            if ( setup.getTimePoints() != null && !setup.getTimePoints().isEmpty() ) {
                templateElem.addContent( XmlHelpers.textElement( "timeTag", setup.getTimeTag() ) );
            }
            resolverElem.addContent( templateElem );
        }
        return resolverElem;
    }

    private KlbPartitionResolver resolverFromXml( final Element elem )
    {
        final String type = elem.getAttributeValue( "type" );
        if ( type.equals( KlbPartitionResolver.class.getName() ) || type.equals( KlbPartitionResolver.class.getName() + "Default" ) ) {
            // For legacy support:
            // Handle resolver type "KlbPartitionResolverDefault" (code line above),
            // look for a "MultiFileNameTag" element with dimension "TIME" (for loop below),
            // to override implicit default values (variables below).
            String timeTag = "TM";
            for ( final Element e : elem.getChildren( "MultiFileNameTag" ) ) {
                final String dimension = XmlHelpers.getText( e, "dimension" );
                if ( dimension.equals( "TIME" ) ) {
                    timeTag = XmlHelpers.getText( e, "tag" );
                    break;
                }
            }

            final KlbPartitionResolver resolver = new KlbPartitionResolver();
            for ( final Element e : elem.getChildren( "ViewSetupTemplate" ) ) {
                final String template = XmlHelpers.getText( e, "template" );
                final String tag = XmlHelpers.getText( e, "timeTag" );
                KlbPartitionResolver.KlbViewSetupConfig config = null;
                if ( tag == null ) {
                    if ( timeTag != null ) {
                        config = resolver.addViewSetup( template, timeTag );
                    } else {
                        config = resolver.addViewSetup( template );
                    }
                } else {
                    config = resolver.addViewSetup( template, tag );
                }
                if ( config == null ) {
                    throw new RuntimeException( String.format( "Could not initialize ViewSetup %d because template file is missing: %s", resolver.getNumViewSetups(), template ) );
                }
            }
            return resolver;
        }

        throw new RuntimeException( "Could not instantiate KlbPartitionResolver" );
    }
}