package org.aksw.simba.bengal.paraphrasing;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

public class BingParaphraseService implements ParaphraseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BingParaphraseService.class);

    private static final String BING_PROPERTIES_FILE = "bing.properties";
    private static final String BING_CLIENT_ID_KEY = "org.aksw.simba.bengal.paraphrasing.BingParaphraser.clientId";
    private static final String BING_CLIENT_SECRET_KEY = "org.aksw.simba.bengal.paraphrasing.BingParaphraser.clientSecret";

    public static BingParaphraseService create() {
        Properties properties;
        try {
            properties = PropertiesLoaderUtils.loadAllProperties(BING_PROPERTIES_FILE);
        } catch (IOException e) {
            LOGGER.error("Couldn't load properties file " + BING_PROPERTIES_FILE + ". Returning null.", e);
            return null;
        }
        BingParaphraseService service = null;
        if (properties.contains(BING_CLIENT_ID_KEY) && properties.contains(BING_CLIENT_SECRET_KEY)) {
            Translate.setClientId(properties.getProperty(BING_CLIENT_ID_KEY));
            Translate.setClientSecret(properties.getProperty(BING_CLIENT_SECRET_KEY));
            service = new BingParaphraseService();
        } else {
            LOGGER.error("Couldn't load needed properties ({}, {}) from properties file {}. Returning null.",
                    BING_CLIENT_ID_KEY, BING_CLIENT_SECRET_KEY, BING_PROPERTIES_FILE);
        }
        return service;
    }

    protected BingParaphraseService() {
    }

    @Override
    public String paraphrase(String originalText) {
        try {
            String translatedText = Translate.execute(originalText, Language.ENGLISH, Language.ITALIAN);
            return Translate.execute(translatedText, Language.ITALIAN, Language.ENGLISH);
        } catch (Exception e) {
            LOGGER.error("Exception from translation API. Returning null.", e);
            return null;
        }
    }
}
