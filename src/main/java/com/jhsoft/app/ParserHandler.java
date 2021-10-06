package com.jhsoft.app;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public abstract class ParserHandler
{
    private final JournalController m_jcInstance;
    private final String m_strUrlPrefix;

    public static class MagnetInfo
    {
        private final ArrayList<String> m_alFileList;
        private String m_strMagnetLink;

        public MagnetInfo()
        {
            m_strMagnetLink = null;
            m_alFileList = new ArrayList<>();
        }

        public String getMagnetLink()
        {
            return m_strMagnetLink;
        }

        public void setMagnetLink(String strMagnetLink)
        {
            m_strMagnetLink = strMagnetLink;
        }

        public void addFileName(String strFilename)
        {
            m_alFileList.add(strFilename);
        }

        public ArrayList<String> getFilenames()
        {
            return m_alFileList;
        }
    }

    public ParserHandler(String strUrl)
    {
        m_jcInstance = new JournalController(getClass().getName());
        m_strUrlPrefix = strUrl;
    }

    protected String getUrlString()
    {
        return m_strUrlPrefix;
    }

    protected void writeLog(Exception e)
    {
        m_jcInstance.writeLog(e);
    }

    protected void writeLog(String strFormat, Object ...objects)
    {
        writeLog(String.format(strFormat, objects));
    }

    protected void writeLog(String strText)
    {
        m_jcInstance.writeLog(strText);
    }

    protected String getStringValueOfKeyInAttributes(String strKeyword, Attributes attrsParam)
    {
        if (attrsParam != null)
        {
            for (Attribute attrItem : attrsParam)
            {
                if (attrItem.getKey().trim().equals(strKeyword.trim()))
                {
                    return attrItem.getValue();
                }
            }
        }

        return null;
    }

    protected Elements getElementOfContainingKeyword(String strKeyword, Element elsParam)
    {
        Elements elsResult = null;

        if (elsParam != null)
        {
            elsResult = elsParam.getElementsByClass(strKeyword);
            if (elsResult.size() > 0)
            {
                return elsResult;
            }
        }

        return elsResult;
    }

    public abstract ArrayList<MagnetInfo> searchTorrent(ArrayList<String> strParameter) throws Exception;
}
