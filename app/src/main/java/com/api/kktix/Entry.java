package com.api.kktix;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "entry",strict = false)
public class Entry{
    @Element(name = "id")
    String id;

    @Element(name = "published")
    String published;

    @Element(name = "updated")
    String updated;

    @Path("link")
    @Attribute(name = "href")
    String url;

    @Element(name = "title")
    String title;

    @Element(name = "summary",type = String.class)
    String summary;

    @Element(name = "content")
    String content;

    @Path("author")
    @Element(name = "name")
    String authorName;

    @Path("author")
    @Element(name = "uri")
    String authorUri;


    public String getContent() {
        return content;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorUri() {
        return authorUri;
    }


    public String getId() {
        return id;
    }

    public String getPublished() {
        return published;
    }

    public String getUpdated() {
        return updated;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getUrl(){
        return url;
    }
}


