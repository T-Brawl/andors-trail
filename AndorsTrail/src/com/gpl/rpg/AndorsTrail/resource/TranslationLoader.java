package com.gpl.rpg.AndorsTrail.resource;

import android.content.res.AssetManager;
import com.gpl.rpg.AndorsTrail.util.L;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public final class TranslationLoader {
    private final BinaryMoFileParser parser;

    public TranslationLoader(AssetManager mgr) {
        BinaryMoFileParser parser = null;
        try {
            InputStream is = mgr.open("messages.mo");
            parser = new BinaryMoFileParser(is);
        } catch (IOException e) {
            L.log("ERROR: Reading from translation asset failed: " + e.toString());
        }
        this.parser = parser;
    }

    private String tr(String s) {
        if (s == null) return null;
        if (s.length() <= 1) return s;
        if (parser == null) return s;
        try {
            //String t = parser.translate(s);
            //L.log(translations.size() + " : " + s + " -> " + t);
            //return t;
            return parser.translate(s);
        } catch (IOException e) {
            return s;
        }
    }

    public void close() {
        if (parser == null) return;
        try {
            parser.close();
        } catch (IOException e) { }
    }

    public String translateItemCategoryName(String s) { return tr(s); }
    public String translateActorConditionName(String s) { return tr(s); }
    public String translateItemTypeName(String s) { return tr(s); }
    public String translateMonsterTypeName(String s) { return tr(s); }
    public String translateQuestName(String s) { return tr(s); }
    public String translateQuestLogEntry(String s) { return tr(s); }
    public String translateConversationPhrase(String s) { return tr(s); }
    public String translateConversationReply(String s) { return tr(s); }

    //public static HashSet<String> translations = new HashSet<String>();

    private static final class BinaryMoFileParser {
        private final InputStream is;
        private final BufferedInputStream reader;
        private final int numStrings;
        private final int offsetOriginalStrings;
        private final int offsetTranslatedStrings;

        public BinaryMoFileParser(InputStream is) throws IOException {
            this.is = is;
            this.reader = new BufferedInputStream(is);
            this.reader.mark(9999999);
            int magic = readIntLE();
            if (magic != 0x950412de) throw new IOException("Invalid magic in MO file");
            this.reader.skip(4);
            numStrings = readIntLE();
            offsetOriginalStrings = readIntLE();
            offsetTranslatedStrings = readIntLE();
        }

        public void close() throws IOException {
            reader.close();
            is.close();
        }

        private final Charset charset = Charset.forName("utf-8");

        public String translate(String s) throws IOException {
            if (numStrings <= 0) return s;

            byte[] bytes = s.getBytes(charset);
            byte[] translation = find(bytes);
            if (translation == null || translation.length <= 0) return s;
            //translations.add(s);
            return new String(translation, charset);
        }

        private byte[] find(byte[] bytes) throws IOException {
            return find(bytes, 0, numStrings);
        }

        private byte[] find(byte[] bytes, int minIndex, int maxIndex) throws IOException {
            int middleIndex;
            while(minIndex != maxIndex) {
                if (minIndex+1 == maxIndex) middleIndex = minIndex;
                else if (minIndex+2 == maxIndex) middleIndex = minIndex;
                else middleIndex = (minIndex + maxIndex) / 2;

                int c = compare(bytes, middleIndex);
                if (c == 0) return getTranslatedStringAt(middleIndex);

                if (minIndex+1 == maxIndex) {
                    return null;
                } else if (c > 0) {
                    maxIndex = middleIndex;
                } else {
                    minIndex = middleIndex+1;
                }
            }
            return null;
        }

        private final byte[] buf = new byte[8];
        public final int readIntLE() throws IOException
        {
            reader.read(buf, 0, 4);
            return
                    (buf[3])      << 24 |
                    (buf[2]&0xff) << 16 |
                    (buf[1]&0xff) <<  8 |
                    (buf[0]&0xff);
        }

        private void seek(int pos) throws IOException {
            reader.reset();
            reader.skip(pos);
        }

        private byte[] getTranslatedStringAt(int idx) throws IOException {
            seek(offsetTranslatedStrings + idx*8);
            int length = readIntLE();
            int offset = readIntLE();
            seek(offset);
            byte[] result = new byte[length];
            reader.read(result, 0, length);
            return result;
        }

        private int compare(byte[] bytes, int idx) throws IOException {
            seek(offsetOriginalStrings + idx*8);
            int length = readIntLE();
            int offset = readIntLE();
            seek(offset);
            int maxLength = bytes.length;
            for(int i = 0; i < length; ++i) {
                if (i == maxLength) return 1;
                int b = reader.read();
                if (b == bytes[i]) continue;
                return (b < bytes[i]) ? -1 : 1;
            }
            if (length < maxLength) return -1;
            return 0;
        }
    }
}
