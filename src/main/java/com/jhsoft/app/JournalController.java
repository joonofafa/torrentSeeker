package com.jhsoft.app;

public class JournalController
{
    public enum LogLevel
    {
        LOG_NORMAL(     "N"),
        LOG_CATION(     "C"),
        LOG_ERROR(      "E"),
        LOG_EXCEPTION(  "X"),
        LOG_SERIOUS(    "S");

        private final String m_strIdentification;

        LogLevel(String strIdentification)
        {
            m_strIdentification = strIdentification;
        }

        public String getIdentification()
        {
            return m_strIdentification;
        }
    }

    private final String m_strClassName;

    public JournalController()
    {
        m_strClassName = this.getClass().getName();
    }

    public JournalController(String strClassName)
    {
        m_strClassName = strClassName;
    }

    public void writeLog(String strMessage)
    {
        writeLog(LogLevel.LOG_NORMAL, strMessage);
    }

    public void writeLog(String strFormat, Object... objArgs)
    {
        writeLog(LogLevel.LOG_NORMAL, strFormat, objArgs);
    }

    public void writeLog(Exception e)
    {
        writeLog(LogLevel.LOG_EXCEPTION, e.toString());
        for (StackTraceElement steTemp : e.getStackTrace())
        {
            writeLog(LogLevel.LOG_EXCEPTION, steTemp.toString());
        }
    }

    public void writeLog(LogLevel emLevel, String strFormat, Object... objArgs)
    {
        writeLog(emLevel, String.format(strFormat, objArgs));
    }

    public void writeLog(LogLevel emLevel, String strMessage)
    {
        System.out.println(String.format("[%06d:%s:%s] %s", Thread.currentThread().getId(), m_strClassName, emLevel.getIdentification(), strMessage));
    }

    private static final int HELLO_COLUMN_SIZE = 16;

    public void writeBuffer(String strName, byte[] baBuffer)
    {
        writeBuffer(strName, baBuffer, baBuffer.length);
    }

    public void writeBuffer(String strName, byte[] baBuffer, int iSize)
    {
        writeBuffer(strName, baBuffer, 0, iSize);
    }

    public void writeBuffer(String strName, byte[] baBuffer, int iOffset, int iSize)
    {
        try
        {
            if (baBuffer == null)
            {
                writeLog("baBuffer[] is NULL!!");
                return;
            }

            if (baBuffer.length > 0)
            {
                StringBuilder sbHex = new StringBuilder();
                int iPadlength = iSize + ((iSize % HELLO_COLUMN_SIZE) == 0 ? 0 : HELLO_COLUMN_SIZE - (iSize % HELLO_COLUMN_SIZE));
                int iColCnt = 0;

                writeLog("[" + strName + "] Length = " + iSize);
                writeLog("=============================================================================");
                writeLog(" Offset   00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F");
                writeLog("-----------------------------------------------------------------------------");
                for (int i = iOffset; i < iOffset + iPadlength; i++)
                {
                    if (i < (iSize + iOffset))
                    {
                        sbHex.append(String.format("%02X", baBuffer[i]));
                    }
                    else
                    {
                        sbHex.append("  ");
                    }

                    sbHex.append(" ");
                    if ((++iColCnt % HELLO_COLUMN_SIZE) == 0)
                    {
                        sbHex.append(": ");
                        for (int iHex = 0; iHex < HELLO_COLUMN_SIZE; iHex++)
                        {
                            if (i - (HELLO_COLUMN_SIZE - 1) + iHex < iSize)
                            {
                                if ((baBuffer[i - (HELLO_COLUMN_SIZE - 1) + iHex] < 0x20) || (baBuffer[i - (HELLO_COLUMN_SIZE - 1) + iHex] > 0x7F))
                                {
                                    sbHex.append("^");
                                }
                                else
                                {
                                    sbHex.append((char)baBuffer[i - (HELLO_COLUMN_SIZE - 1) + iHex]);
                                }
                            }
                        }

                        writeLog(String.format("%08X", i - (HELLO_COLUMN_SIZE - 1)) + "  " + sbHex.toString());
                        sbHex.delete(0, sbHex.length());
                    }
                }
                writeLog("=============================================================================");
            }
        }
        catch (Exception e)
        {
            writeLog(e);
        }
    }
}
