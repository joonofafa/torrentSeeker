package com.jhsoft.app.engine;

import com.jhsoft.app.ParserHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class BTDiggEngine extends ParserHandler {
    private enum ClassName {
        ONE_RESULT("one_result"),
        TORRENT_EXCERPT("torrent_excerpt"),
        TORRENT_MAGNET("torrent_magnet"),
        FA_MAGNET("fa fa-magnet"),
        ;
        private final String _prefix;

        ClassName(String prefix) {
            _prefix = prefix;
        }

        public String getKeyword() {
            return _prefix;
        }
    }

    private enum AttributeName {
        FA_FILE("fa fa-file"),
        ;

        private final String _prefix;

        AttributeName(String prefix) {
            _prefix = prefix;
        }

        public String getKeyword() {
            return _prefix;
        }
    }

    public BTDiggEngine() {
        super("https://btdig.com/search?q=");
    }

    public ArrayList<MagnetInfo> searchTorrent(ArrayList<String> strParameter) throws Exception {
        StringBuilder keyword = new StringBuilder();
        ArrayList<MagnetInfo> magInfoList = new ArrayList<>();

        if (strParameter == null) {
            return null;
        }

        for (String strItem : strParameter) {
            keyword.append(strItem.trim());
            keyword.append("+");
        }

        String url = super.getUrlString() + keyword.toString() + "&order=2";
        String temp;
        Document document = Jsoup.connect(url).get();
        Elements elsResult;
        Elements elsTorrentExcerpt, elsMagnet, elsFaMagnet;
        boolean isFound;
        MagnetInfo miTemp = null;

        super.writeLog("* URL Called [%s]", url);

        elsResult = document.getElementsByClass(ClassName.ONE_RESULT.getKeyword());
        /*writeLog("* elsResult.size [%d]", elsResult.size());*/
        if (elsResult.size() > 0) {
            for (Element elItem : elsResult) {
                /*super.writeLog("* Index [%d]", elsResult.indexOf(elItem));*/
                elsTorrentExcerpt = getElementOfContainingKeyword(ClassName.TORRENT_EXCERPT.getKeyword(), elItem);
                if (elsTorrentExcerpt.size() > 0) {
                    miTemp = new MagnetInfo();
                    for (Element elSubItem : elsTorrentExcerpt) {
                        for (Element elSubSubItem : elSubItem.children()) {
                            isFound = false;
                            for (Attribute attrSubSubItem : elSubSubItem.attributes()) {
                                if (attrSubSubItem.getKey().toLowerCase().equals("class")) {
                                    if (attrSubSubItem.getValue().toLowerCase().contains(AttributeName.FA_FILE.getKeyword())) {
                                        /*super.writeLog("    - Attribute [%s:%s]", attrSubSubItem.getKey(), attrSubSubItem.getValue());*/
                                        isFound = true;
                                        break;
                                    }
                                }
                            }

                            if (isFound) {
                                /*super.writeLog("    - Filename [%s]", elSubSubItem.text());*/
                                miTemp.addFileName(elSubSubItem.text());
                            }
                        }
                    }
                }

                elsMagnet = getElementOfContainingKeyword(ClassName.TORRENT_MAGNET.getKeyword(), elItem);
                if ((elsTorrentExcerpt.size() > 0) && (elsMagnet.size() > 0)) {
                    elsFaMagnet = getElementOfContainingKeyword(ClassName.FA_MAGNET.getKeyword(), elsMagnet.get(0));
                    /*writeLog("* elsFaMagnet.size [%d]", elsFaMagnet.size());*/
                    if (elsFaMagnet.size() > 0) {
                        for (Element elSubItem : elsFaMagnet) {
                            for (Element elFaSub : elSubItem.children()) {
                                /*writeLog("*    [%s] Attribute [%s]", elFaSub.text(), elFaSub.attributes().toString());*/
                                temp = getStringValueOfKeyInAttributes("href", elFaSub.attributes());
                                if (temp != null && miTemp != null) {
                                    miTemp.setMagnetLink(temp);
                                    /*writeLog("* Magnet [%s]", temp);*/
                                    magInfoList.add(miTemp);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return magInfoList;
    }
}
