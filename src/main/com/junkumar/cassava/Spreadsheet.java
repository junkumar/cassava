package com.junkumar.cassava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

public class Spreadsheet {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(
                    "This program takes a single mandatory argument which is an input csv file without a header row.\n" +
                    "After evaluation, the output will be formatted as csv and printed to STDOUT.\n" +
                    "Usage: cassava csv-file-name");
            System.exit(1);
        }

        File inFile = new File(args[0]);
        if (!inFile.exists()) {
            System.err.println("File not found - " + args[0] + "\n");
            System.exit(1);
        }

        if (!inFile.isFile() || !inFile.canRead()) {
            System.err.println("File not readable - " + args[0] + "\n");
            System.exit(1);
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inFile));
            SpreadsheetContext spreadsheet = new SpreadsheetContext(reader);
            // Using java 8 methodRef to specify that we want non-strict behavior
            // which will allow null terminals to be printed -> getFormatted
            spreadsheet.printStream(new OutputStreamWriter(System.out), spreadsheet::getFormatted);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
}
