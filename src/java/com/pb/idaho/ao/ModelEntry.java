package com.pb.idaho.ao;

import com.pb.common.util.ResourceUtil;
import java.io.*;
import java.util.*;

/**
 * The {@code ModelEntry} ...
 *
 * @author crf
 *         Started 2/4/13 3:42 PM
 */
public enum ModelEntry {
    PT(ModelEntryParameterKeys.PROPERTY_FILE_PATH);

    private final Set<String> requiredParameterKeys;
    private final Set<String> optionalParameterKeys;

    private ModelEntry(List<String> optionalParameters, String ... requiredParameterKeys) {
        this.requiredParameterKeys = new LinkedHashSet<String>(Arrays.asList(requiredParameterKeys));
        this.optionalParameterKeys = new LinkedHashSet<String>(optionalParameters);
    }

    private ModelEntry(String ... requiredParameterKeys) {
    	this(new LinkedList<String>(),requiredParameterKeys);
    }

    public static class ModelEntryParameterKeys {
        public static final String PROPERTY_FILE_PATH = "property_file";
    }


    private void checkKeys(Map<String,String> parameters) {
        Set<String> missingKeys = new HashSet<String>();
        for (String requiredKey : requiredParameterKeys)
            if (!parameters.containsKey(requiredKey))
                missingKeys.add(requiredKey);
        if (missingKeys.size() > 0)
            throw new IllegalArgumentException("Missing required parameters for " + this + ": " + missingKeys.toString());
    }

    private ResourceBundle getResourceBundle(Map<String,String> parameters) {
        return ResourceUtil.getPropertyBundle(new File(parameters.get(ModelEntryParameterKeys.PROPERTY_FILE_PATH)));
    }

    private int getBaseYear(Map<String,String> parameters) {
        return ResourceUtil.getIntegerProperty(getResourceBundle(parameters),"base.year");
    }

    private String getRootDir(Map<String,String> parameters) {
        return ResourceUtil.getProperty(getResourceBundle(parameters),"root.dir");
    }

    private int getTYear(Map<String,String> parameters) {
        return ResourceUtil.getIntegerProperty(getResourceBundle(parameters),"t.year");
    }

    public void writeRunParamsToPropertiesFile(Map<String,String> parameters){
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        int baseYear = getBaseYear(parameters);
        int tYear = getTYear(parameters);
        String propertyFile = parameters.get(ModelEntryParameterKeys.PROPERTY_FILE_PATH);
        String scenarioName = resourceBundle.getString("scenario.name");

        File runParams = new File(resourceBundle.getString("pt.daf.run.params.file"));
        PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(runParams));
            writer.println("scenarioName=" + scenarioName);
            writer.println("baseYear=" + baseYear);
            writer.println("timeInterval=" + tYear);
            writer.println("pathToAppRb=" + propertyFile);
            writer.println("pathToGlobalRb=" + propertyFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open the RunParams file", e);
        }
        writer.close();
    }

    private void runPt(Map<String,String> parameters) {
        PT.checkKeys(parameters);
        ResourceBundle resourceBundle = getResourceBundle(parameters);
        writeRunParamsToPropertiesFile(parameters);
        new StartDafApplication("ptdaf",resourceBundle,getTYear(parameters)).run();
    }

    private static String usage() {
        StringBuilder builder = new StringBuilder("ModelEntry usage:\n");
        builder.append("java ... com.pb.idaho.ao.ModelEntry model key1=parameter1 ...\n");
        builder.append("  where models and keys (r = required) are:\n");
        builder.append("  Model        Parameters \n");
        builder.append("  -----        -----------\n");
        for (ModelEntry entry : ModelEntry.values()) {
            String entryName = entry.name();
            int gap = 13 - entryName.length();
            builder.append("  ").append(entryName);
            for (int i = 0; i < gap; i++)
                builder.append(" ");
            boolean first = true;
            for (String param : entry.requiredParameterKeys) {
                if (first)
                    first = false;
                else
                    builder.append("               ");
                builder.append(param).append(" (r)\n");
            }
            for (String param : entry.optionalParameterKeys) {
                if (first)
                    first = false;
                else
                    builder.append("               ");
                builder.append(param).append("\n");
            }
        }
        return builder.toString();
    }

    private static Map<String,String> parseParameters(String ... args) {
        Map<String,String> parameters = new HashMap<String,String>();
        //skip first one
        for (int i = 1; i < args.length; i++) {
            String[] split = args[i].split("=",2);
            parameters.put(split[0].trim(),split.length > 1 ? split[1].trim() : "");
        }
        return parameters;
    }

    public static void main(String ... args) {
        if (args.length < 1)
            throw new IllegalArgumentException(usage());
        ModelEntry model;
        try {
            model = ModelEntry.valueOf(args[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown model type: " + args[0] + "\n" + usage());
        }
        Map<String,String> parameters = parseParameters(args);
        PT.runPt(parameters);
    }
}
