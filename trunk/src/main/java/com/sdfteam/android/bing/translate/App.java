package com.sdfteam.android.bing.translate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

/**
 * Hello world!
 *
 */
public class App {
    private static final Language SOURCE_LANGUAGE = Language.ENGLISH;
    private static final String API_KEY = Configuration.getString("configuration.api_key"); //$NON-NLS-1$
    private static final String PATH = Configuration.getString("configuration.values_path"); //$NON-NLS-1$
    private static final String FILENAME = Configuration.getString("configuration.filename"); //$NON-NLS-1$
    private static final String ENDLINE = Configuration.getString("configuration.endline"); //$NON-NLS-1$
    private static final String WORKING_DIR = Configuration.getString("configuration.work_dir"); //$NON-NLS-1$
    private static final Language[] TARGET_LANGUAGES = /*getLanguages(); /* */ {Language.HUNGARIAN, Language.GERMAN, Language.DUTCH, Language.DANISH, Language.FRENCH, Language.SWEDISH, Language.SPANISH}; //*/
    private static final Pattern STRINGS_PATTERN = Pattern.compile(
            "<string\\s+name=\\\"([a-zA-Z0-9_]*)\\\">([^<]*)</string>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE); //$NON-NLS-1$
    private static final String NL = "\r\n"; //$NON-NLS-1$
    private static final long WAIT_TIME = 5 * 60 * 1000;
    private static final int WAIT_AFTER_COUNT = 99;
    private static int currentCount = 0;

    public static void main(String[] args) throws Exception {
        Translate.setKey(API_KEY);
        String workingDirectory = WORKING_DIR;
        String file = readFile(workingDirectory + PATH + Configuration.getString("configuration.path_separator") + FILENAME); //$NON-NLS-1$
        Matcher matcher = STRINGS_PATTERN.matcher(file);
        List<DroidString> strings = retrieveStrings(matcher);
        Map<Language, List<DroidString>> translated = translateStrings(strings);
        System.out.println(translated);
        writeTranslations(workingDirectory, translated);
        System.out.println("Finished translating"); //$NON-NLS-1$
    }

    private static void writeTranslations(String workingDirectory, Map<Language, List<DroidString>> translated) throws IOException {
        Set<Language> langs = translated.keySet();
        for (Language language : langs) {
            List<DroidString> stringsList = translated.get(language);
            File languageFile = getLanguageFile(workingDirectory, language);
            Writer fileWriter = new FileWriter(languageFile);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writeHeader(writer);
            writeStrings(writer, stringsList);
            writeFooter(writer);
            writer.close();
        }
    }

    private static void writeStrings(BufferedWriter writer, List<DroidString> stringsList) throws IOException {
        for (DroidString string : stringsList) {
            writer.append("\t<string name=\"" + string.getKey() + "\">" + string.getValue() + "</string>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    private static void writeFooter(BufferedWriter writer) throws IOException {
        writer.append("</resources>"); //$NON-NLS-1$
    }

    private static void writeHeader(BufferedWriter writer) throws IOException {
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + NL); //$NON-NLS-1$
        writer.append("<!-- This file is generated with android bing translation tool -->" + NL); //$NON-NLS-1$
        writer.append("<resources>" + NL); //$NON-NLS-1$
    }

    private static File getLanguageFile(String workingDirectory, Language language) throws IOException {
        String path = createDir(workingDirectory, language);

        File file = new File(path + "/" + FILENAME); //$NON-NLS-1$
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        return file;
    }

    private static String createDir(String baseDir, Language language) {
        String path = baseDir + PATH + "_" + language.toString(); //$NON-NLS-1$
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdir();
        }
        return path;
    }

    private static Map<Language, List<DroidString>> translateStrings(List<DroidString> strings) throws Exception {
        Map<Language, List<DroidString>> translated = new HashMap<Language, List<DroidString>>();

        List<String> strignsToTranslate = new LinkedList<String>();
        for (DroidString droidString : strings) {
            strignsToTranslate.add(droidString.getValue());
        }
        
        for (Language language : TARGET_LANGUAGES) {
            waitOnDemand();
            String[] translatedStrings = Translate.execute(strignsToTranslate.toArray(new String[]{}), SOURCE_LANGUAGE, language);
            int index = 0;
            for (DroidString droidString : strings) {
                String translatedString = translatedStrings[index];//Translate.execute(droidString.getValue(), SOURCE_LANGUAGE, language);
                DroidString copy1 = droidString.copy();
                copy1.setValue(translatedString);
                DroidString copy = copy1;
                List<DroidString> translatedList = getListForLanguages(translated, language);
                translatedList.add(copy);
                System.out.println(droidString + " translated to language:" + language + ", result: " + copy); //$NON-NLS-1$ //$NON-NLS-2$*/
                index ++;
            }
        }
        return translated;
    }

    private static void waitOnDemand() {
        currentCount++;
        if (currentCount % WAIT_AFTER_COUNT == 0) {
            System.out.println("Sleeping " + WAIT_TIME + "ms."); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<DroidString> getListForLanguages(Map<Language, List<DroidString>> translated, Language language) {
        List<DroidString> translatedList = translated.get(language);
        if (translatedList == null) {
            translatedList = new LinkedList<DroidString>();
            translated.put(language, translatedList);
        }
        return translatedList;
    }

    private static List<DroidString> retrieveStrings(Matcher matcher) {
        List<DroidString> strings = new LinkedList<App.DroidString>();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            DroidString droidString = new DroidString(key, value);
            strings.add(droidString);
            System.out.println(droidString);
        }
        return strings;
    }

    /*private static Language[] getLanguages() {
        Language[] values = Language.values();
        List<Language> langs = new LinkedList<Language>();
        for (Language lang : values) {
            if (!SOURCE_LANGUAGE.equals(lang) && !Language.AUTO_DETECT.equals(lang)) {
                langs.add(lang);
            }
        }
        return langs.toArray(values);
    }*/

    private static String readFile(String string) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(string));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line + ENDLINE);
            line = reader.readLine();
        }
        return sb.toString();
    }

    public static class DroidString {
        private String key;
        private String value;

        public DroidString(String key, String value) {
            super();
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "DroidString [key=" + key + ", value=" + value + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        public DroidString copy() {
            DroidString droidString = new DroidString(key, value);
            return droidString;
        }
    }
}
