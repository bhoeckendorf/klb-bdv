package org.janelia.simview.klb.bdv.ui;

class Global
{
    private static String currentPath = "";

    protected static String getCurrentOrDefaultPath( final String suggestion )
    {
        if ( suggestion == null || suggestion.trim().isEmpty() )
            return getCurrentOrDefaultPath();
        return suggestion;
    }

    protected static String getCurrentOrDefaultPath()
    {
        if ( currentPath.isEmpty() )
            return System.getProperty( "user.home" );
        return currentPath;
    }

    protected static void updateCurrentPath( final String path )
    {
        currentPath = path;
    }
}
