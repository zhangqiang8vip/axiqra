package com.axiqra.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeneratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorBootstrap.class);

    private GeneratorBootstrap() {
    }

    public static void main(String[] args) {
        LOGGER.info("Axiqra generator scaffold ready.");
    }
}
