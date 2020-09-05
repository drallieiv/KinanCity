package com.kinancity.mail.config;

import com.kinancity.mail.mailchanger.password.EmailMatcher;
import com.kinancity.mail.mailchanger.password.matcher.ExactEmailMatcher;
import com.kinancity.mail.mailchanger.password.matcher.MatchingPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class CsvPasswordReader {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CSV_COMMENT_PREFIX = "#";

    private static final String CSV_SPLITTER = ";";

    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    public List<MatchingPair> load(String accountFileName) {
        logger.info("Will load a list of accout to create from a csv file");

        File accountFile = new File(accountFileName);
        if (!accountFile.exists() || !accountFile.canRead()) {
            logger.error("Cannot open file {}. Abort", accountFileName);
        }

        try {
            return loadFile(accountFile);
        } catch (FileNotFoundException e) {
            logger.error("Cannot open file {}. Abort", accountFileName);
        }
        return new ArrayList<>();
    }



    /**
     * Load CSV file and build a list of Exact matchers
     * @param accountFile
     * @return
     * @throws FileNotFoundException
     */
    private List<MatchingPair> loadFile(File accountFile) throws FileNotFoundException {
        List<MatchingPair> rules = new ArrayList<>();
        try (Scanner scanner = new Scanner(accountFile)) {
            // Read first line that must be header
            String firstline = scanner.nextLine();
            List<String> headers = null;
            if (firstline != null && firstline.startsWith(CSV_COMMENT_PREFIX)) {
                headers = Arrays.asList(firstline.replace(CSV_COMMENT_PREFIX, "").split(CSV_SPLITTER));
                if (!headers.containsAll(Arrays.asList(PASSWORD, EMAIL))) {
                    logger.error("CSV file header is missing either password or email fields.");
                }
            } else {
                logger.error("CSV file is missing header line.");
                return rules;
            }

            // Read all other lines
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                rules.add(buildMatcherFromCsv(line, headers));
            }
        }
        return rules;
    }

    /**
     * Create a matching pair from csv
     * @param line
     * @param headers
     * @return MatchingPair
     */
    public MatchingPair buildMatcherFromCsv(String line, List<String> headers) {
        // Parse csv into data map
        Map<String, String> fieldMap = new HashMap<>();
        List<String> fields = Arrays.asList(line.split(CSV_SPLITTER));
        for (int i = 0; i < Math.min(fields.size(), headers.size()); i++) {
            fieldMap.put(headers.get(i), fields.get(i));
        }
        return buildMatcherFromDataFromMap(fieldMap);
    }

    /**
     * Create an MatchingPair given a set of fields
     *
     * @param fieldMap
     * @return MatchingPair
     */
    public MatchingPair buildMatcherFromDataFromMap(Map<String, String> fieldMap) {
        String email = fieldMap.get(EMAIL);
        String password = fieldMap.get(PASSWORD);
        EmailMatcher matcher = new ExactEmailMatcher(email);
        return new MatchingPair(matcher, password);
    }
}
