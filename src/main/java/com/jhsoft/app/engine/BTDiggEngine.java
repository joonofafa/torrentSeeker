package com.jhsoft.app.engine;

import com.jhsoft.app.ParserHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class BTDiggEngine extends ParserHandler
{
    private enum ClassName
    {
        ONE_RESULT("one_result"),
        TORRENT_EXCERPT("torrent_excerpt"),
        TORRENT_MAGNET("torrent_magnet"),
        FA_MAGNET("fa fa-magnet"),
        ;
        private final String m_strPrefix;

        ClassName(String strPrefix)
        {
            m_strPrefix = strPrefix;
        }

        public String getKeyword()
        {
            return m_strPrefix;
        }
    }

    private enum AttributeName
    {
        FA_FILE("fa fa-file"),
        ;

        private final String m_strPrefix;

        AttributeName(String strPrefix)
        {
            m_strPrefix = strPrefix;
        }

        public String getKeyword()
        {
            return m_strPrefix;
        }
    }

    public BTDiggEngine()
    {
        super("https://btdig.com/search?q=");
    }

    public ArrayList<MagnetInfo> searchTorrent(ArrayList<String> strParameter) throws Exception
    {
        StringBuilder sbKeyword = new StringBuilder();
        ArrayList<MagnetInfo> alMagInfo = new ArrayList<>();

        if (strParameter == null)
        {
            return null;
        }

        for (String strItem : strParameter)
        {
            sbKeyword.append(strItem.trim());
            sbKeyword.append("+");
        }

        String strUrl = super.getUrlString() + sbKeyword.toString();
        String strTemp;
        Document docInst = Jsoup.connect(strUrl).get();
        Elements elsOneResult;
        Elements elsTorrentExcerpt, elsMagnet, elsFaMagnet;
        boolean isFound;
        MagnetInfo miTemp = null;

        /*super.writeLog("* URL Called [%s]", strUrl);*/

        elsOneResult = docInst.getElementsByClass(ClassName.ONE_RESULT.getKeyword());
        /*writeLog("* elsOneResult.size [%d]", elsOneResult.size());*/
        if (elsOneResult.size() > 0)
        {
            for (Element elItem : elsOneResult)
            {
                /*super.writeLog("* Index [%d]", elsOneResult.indexOf(elItem));*/
                elsTorrentExcerpt = getElementOfContainingKeyword(ClassName.TORRENT_EXCERPT.getKeyword(), elItem);
                if (elsTorrentExcerpt.size() > 0)
                {
                    miTemp = new MagnetInfo();
                    for (Element elSubItem : elsTorrentExcerpt)
                    {
                        for (Element elSubSubItem : elSubItem.children())
                        {
                            isFound = false;
                            for (Attribute attrSubSubItem : elSubSubItem.attributes())
                            {
                                if (attrSubSubItem.getKey().toLowerCase().equals("class"))
                                {
                                    if (attrSubSubItem.getValue().toLowerCase().contains(AttributeName.FA_FILE.getKeyword()))
                                    {
                                        /*super.writeLog("    - Attribute [%s:%s]", attrSubSubItem.getKey(), attrSubSubItem.getValue());*/
                                        isFound = true;
                                        break;
                                    }
                                }
                            }

                            if (isFound)
                            {
                                /*super.writeLog("    - Filename [%s]", elSubSubItem.text());*/
                                miTemp.addFileName(elSubSubItem.text());
                            }
                        }
                    }
                }

                elsMagnet = getElementOfContainingKeyword(ClassName.TORRENT_MAGNET.getKeyword(), elItem);

                if ((elsTorrentExcerpt.size() > 0) && (elsMagnet.size() > 0))
                {
                    elsFaMagnet = getElementOfContainingKeyword(ClassName.FA_MAGNET.getKeyword(), elsMagnet.get(0));
                    /*writeLog("* elsFaMagnet.size [%d]", elsFaMagnet.size());*/
                    if (elsFaMagnet.size() > 0)
                    {
                        for (Element elSubItem : elsFaMagnet)
                        {
                            for (Element elFaSub : elSubItem.children())
                            {
                                /*writeLog("*    [%s] Attribute [%s]", elFaSub.text(), elFaSub.attributes().toString());*/
                                strTemp = getStringValueOfKeyInAttributes("href", elFaSub.attributes());
                                if (strTemp != null && miTemp != null)
                                {
                                    miTemp.setMagnetLink(strTemp);
                                    /*writeLog("* Magnet [%s]", strTemp);*/
                                    alMagInfo.add(miTemp);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return alMagInfo;
    }
}
