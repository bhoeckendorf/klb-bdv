package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

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

        for ( final String template : resolver.viewSetupTemplates ) {
            final Element templateElem = new Element( "ViewSetupTemplate" );
            templateElem.addContent( XmlHelpers.textElement( "template", template ) );
            resolverElem.addContent( templateElem );
        }
        if ( resolver.getLastTimePoint() - resolver.getFirstTimePoint() > 0 ) {
            KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
            tag.dimension = KlbMultiFileNameTag.Dimension.TIME;
            tag.tag = resolver.timeTag;
            tag.first = resolver.getFirstTimePoint();
            tag.last = resolver.getLastTimePoint();
            tag.stride = 1;
            resolverElem.addContent( nameTagToXml( tag ) );
        }
        if ( resolver.getMaxNumResolutionLevels() > 1 ) {
        	KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
            tag.dimension = KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL;
            tag.tag = resolver.resLvlTag;
            tag.first = 0;
            tag.last = resolver.getMaxNumResolutionLevels() - 1;
            tag.stride = 1;
            resolverElem.addContent( nameTagToXml( tag ) );
        }

        return resolverElem;
    }

    private KlbPartitionResolver resolverFromXml( final Element elem )
    {
        final String type = elem.getAttributeValue( "type" );
        if ( type.equals( KlbPartitionResolver.class.getName() ) || type.equals( KlbPartitionResolver.class.getName() + "Default" ) ) {
            final List< String > templates = new ArrayList< String >();

            for ( final Element e : elem.getChildren( "ViewSetupTemplate" ) ) {
                templates.add( XmlHelpers.getText( e, "template" ) );
            }

            final List< KlbMultiFileNameTag > tags = new ArrayList< KlbMultiFileNameTag >();
            for ( final Element e : elem.getChildren( "MultiFileNameTag" ) ) {
                tags.add( nameTagFromXml( e ) );
            }
            
            boolean hasTime = false, hasResolution = false;
            for (int i=0; i < tags.size(); ++i) {
            	if (tags.get(i).dimension.equals(KlbMultiFileNameTag.Dimension.TIME))
            		hasTime = true;
            	if (tags.get(i).dimension.equals(KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL))
            		hasResolution = true;
            }
            if ( ! hasTime ) {
                final KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
                tag.tag = "";
                tag.dimension = KlbMultiFileNameTag.Dimension.TIME;
                tag.first = 0;
                tag.last = 0;
                tag.stride = 1;
                tags.add( tag );
            }
            if ( ! hasResolution ) {
                final KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
                tag.tag = "";
                tag.dimension = KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL;
                tag.first = 0;
                tag.last = 0;
                tag.stride = 1;
                tags.add( tag );
            }
            Collections.sort(tags);
            
            String[] arr = new String[ templates.size() ];
            templates.toArray( arr );
            return new KlbPartitionResolver( arr, tags.get(0).tag, tags.get(0).first, tags.get(0).last, "RESLVL", tags.get(1).last+1 );
        }

        throw new RuntimeException( "Could not instantiate KlbPartitionResolver" );
    }

    private Element nameTagToXml( final KlbMultiFileNameTag tag )
    {
        final Element elem = new Element( "MultiFileNameTag" );
        elem.addContent( XmlHelpers.textElement( "dimension", tag.dimension.toString() ) );
        elem.addContent( XmlHelpers.textElement( "tag", tag.tag ) );
        elem.addContent( XmlHelpers.intElement( "lastIndex", tag.last ) );

        if ( tag.first != 0 ) {
            elem.addContent( XmlHelpers.intElement( "firstIndex", tag.first ) );
        }

        if ( tag.stride != 1 ) {
            elem.addContent( XmlHelpers.intElement( "indexStride", tag.stride ) );
        }

        return elem;
    }

    private KlbMultiFileNameTag nameTagFromXml( final Element elem )
    {
        final KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
        final String dim = XmlHelpers.getText( elem, "dimension" );
        if ( dim.equals( KlbMultiFileNameTag.Dimension.TIME.toString() ) ) {
            tag.dimension = KlbMultiFileNameTag.Dimension.TIME;
        }
        else if ( dim.equals( KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL.toString() ) ) {
            tag.dimension = KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL;
        }
        tag.tag = XmlHelpers.getText( elem, "tag" );
        tag.last = XmlHelpers.getInt( elem, "lastIndex" );

        tag.first = 0;
        try {
            tag.first = XmlHelpers.getInt( elem, "firstIndex" );
        } catch ( Exception ex ) {
        }

        tag.stride = 1;
        try {
            tag.stride = XmlHelpers.getInt( elem, "indexStride" );
        } catch ( Exception ex ) {
        }

        return tag;
    }
}
