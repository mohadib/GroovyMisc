#!/usr/bin/env groovy
// https://en.wikipedia.org/wiki/Wikipedia:Getting_to_Philosophy
@Grab(group='org.ccil.cowan.tagsoup', module='tagsoup', version='1.2' )

history = new HashSet()
slurper = new XmlSlurper( new org.ccil.cowan.tagsoup.Parser() )

// more reliable than a regex :s
cleanParens = { doc ->
    def reader = new StringReader( doc )
    int depth = 0
    def inquote = false
    def c
    def open = '(' as char
    def close = ')' as char
    def clean = new StringBuilder()
    while( (c = reader.read()) != -1)
    {
        c = c as char
        if( !inquote && c == open) depth++
        else if(!inquote && c == close && depth) depth--
        else if(!inquote && c == close && !depth) clean.append(c) // avoid hits on '1) item one <b> 2) item two'
        else if( c == '"' as char && !depth)
        {
            clean.append(c)
            inquote = !inquote
        }
        else if( !depth ) clean.append(c)
    }
    clean.toString()
}

doIt = { pageName  ->
    def raw = new URL("https://en.wikipedia.org/wiki/${pageName}").text
    def html = slurper.parseText( cleanParens(raw) )
    def content = html."**".find{ it.@id == "mw-content-text" }

    def links = content."**".findAll{
        it.name() == 'a' &&
        it.@href?.text()?.length() != 0 &&
        it.@href?.text()?.indexOf('#') == -1 &&
        it.@class.size() == 0 &&
        it.@href.text().indexOf("action=edit") == -1
    }

    // see if we can find a link with a 'p' parent, or return the first (this weeds out a lot of junk)
    // links on disambiguation pages are sometimes not in a 'p'
    // check for parent->parent == mw-content-text to avoid sidebar links..
    def link = links.find{
        it.parent().name() == 'p' &&
        it.parent().parent().@id == "mw-content-text" &&
        !history.contains( it.@href?.text() )
    } ?: links.find{ !history.contains( it.@href?.text() ) }

    def linkToken = link.@href?.text()?.split("/")?.getAt(-1)
    history.add( link.@href?.text() )

    if( linkToken != "Philosophy" )
    {
        println linkToken
        doIt( linkToken )
    }
}

doIt(args[0] )
println "\nTotal links to get to Philosophy: ${history.size()}"
