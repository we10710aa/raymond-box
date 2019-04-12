package com.api.kktix;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "feed",strict = false)
public class KktixRss {

    @Element(name = "id")
    String id;

    @Element(name = "title")
    String title;

    @Element(name = "updated")
    String updated;

    @Element(name = "icon")
    String icon;

    @Element(name = "logo")
    String logo;

    @ElementList(name = "entry",inline =true)
    List<Entry> entryList;


    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUpdated() {
        return updated;
    }

    public String getIcon() {
        return icon;
    }

    public String getLogo() {
        return logo;
    }

    public List<Entry> getEntryList() {
        return entryList;
    }


}

