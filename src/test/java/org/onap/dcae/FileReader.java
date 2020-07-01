package org.onap.dcae;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class FileReader {

    private static final Logger log = LoggerFactory.getLogger(FileReader.class);

    private FileReader() {
    }

    public static String readFileAsString(String fileName) {
        String fileContent = "";
        try {
            fileContent = new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            log.error("Error while reading file.", e);
        }
        return fileContent;
    }
}