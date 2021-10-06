package com.jhsoft.app;

import com.jhsoft.app.engine.BTDiggEngine;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.jhsoft.app.ParserHandler.*;

public class ParserMain
{
    public static void main(String[] args)
    {
        if (args == null || args.length == 0)
        {
            System.out.println(
                    "You need to input the parameters to find the magnets. \n" +
                    "  Parameter Usage(Example:Single file)   > ubuntu desktop iso \n" +
                    "  Parameter Usage(Example:Multiple file) > game of thrones s1 e[1-10] \n"
            );
            return;
        }

        JournalController jcInst = new JournalController("MainClass");
        ArrayList<MagnetInfo> alResult;
        ArrayList<MagnetInfo> alContainList;
        Clipboard cbInst = Toolkit.getDefaultToolkit().getSystemClipboard();

        MagnetInfo miChoose;
        boolean isContainedAll;
        StringBuilder sbTemp = new StringBuilder();

        try
        {
            BTDiggEngine pmInst = new BTDiggEngine();
            ArrayList<String> alParam = new ArrayList<>();
            ArrayList<String> alArgs = new ArrayList<>();
            String strPrefix = "";
            String[] saNumbers;
            ArrayList<Integer> alNumbers = new ArrayList<>();
            int iNumberLength = 0;

            for (String strItem : args)
            {
                if (strItem.contains("[") && strItem.endsWith("]"))
                {
                    strPrefix = strItem.substring(0, strItem.indexOf("["));
                    saNumbers = strItem.substring(strItem.indexOf("[") + 1, strItem.indexOf("]")).split("-");
                    for (String strNo : saNumbers)
                    {
                        alNumbers.add(Integer.parseInt(strNo.trim()));
                    }
                }
                else
                {
                    alArgs.add(strItem);
                }
            }

            int iStart = alNumbers.isEmpty() ? 1 : Collections.min(alNumbers);
            int iEnd = alNumbers.isEmpty() ? 1 : Collections.max(alNumbers);

            if (iEnd > 1)
            {
                iNumberLength = String.valueOf(iEnd).length();
            }

            jcInst.writeLog("iNumberLength = " + iNumberLength);
            for (int iIndex = iStart; iIndex <= iEnd; iIndex++)
            {
                alParam.clear();
                alParam.addAll(alArgs);
                if (iNumberLength > 1)
                {
                    alParam.add(strPrefix + String.format("%0" + iNumberLength + "d", iIndex));
                }
                else if (iNumberLength == 1)
                {
                    alParam.add(strPrefix + iIndex);
                }

                jcInst.writeLog("Actual Parameter ->" + Arrays.toString(alParam.toArray()));

                alResult = pmInst.searchTorrent(alParam);
                if (alResult.size() > 0)
                {
                    alContainList = new ArrayList<>();
                    for (MagnetInfo miItem : alResult)
                    {
                        for (String strItem : miItem.getFilenames())
                        {
                            isContainedAll = true;
                            for (String strKeyword : alParam)
                            {
                                if (!strItem.trim().toLowerCase().contains(strKeyword.trim().toLowerCase()))
                                {
                                    isContainedAll = false;
                                    break;
                                }
                            }

                            if (isContainedAll)
                            {
                                alContainList.add(miItem);
                                break;
                            }
                        }
                    }

                    if (alContainList.size() > 0)
                    {
                        miChoose = null;
                        for (MagnetInfo miItem : alContainList)
                        {
                            if (miChoose == null)
                            {
                                miChoose = miItem;
                            }
                            else
                            {
                                if (miChoose.getFilenames().size() > miItem.getFilenames().size())
                                {
                                    miChoose = miItem;
                                }
                            }
                        }

                        if (miChoose != null)
                        {
                            if (miChoose.getMagnetLink() != null)
                            {
                                jcInst.writeLog("* Magnet containing [%s] file is added at Transmission-Engine. (File count:%d)", miChoose.getFilenames(), miChoose.getFilenames().size());
                                sbTemp.append(miChoose.getMagnetLink()).append("\n");
                            }
                        }
                    }
                    else
                    {
                        jcInst.writeLog("* Magnet containing [%s] file is nothing.", alParam.toString());
                    }

                    Thread.sleep(1000 + (long)(Math.random() * 1500));
                }
            }
        }
        catch (Exception e)
        {
            jcInst.writeLog(e);
        }

        if (sbTemp.toString().length() > 0)
        {
            cbInst.setContents(new StringSelection(sbTemp.toString()), null);
        }
    }
}
