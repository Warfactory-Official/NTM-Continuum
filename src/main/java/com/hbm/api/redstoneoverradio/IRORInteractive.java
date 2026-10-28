package com.hbm.api.redstoneoverradio;

import java.util.Locale;

public interface IRORInteractive extends IRORInfo {
    String NAME_SEPARATOR = "!";
    String PARAM_SEPARATOR = ":";

    String EX_NULL = "Exception: Null Command";
    String EX_NAME = "Exception: Multiple Name Separators";
    String EX_FORMAT = "Exception: Parameter in Invalid Format";

    String runRORFunction(String name, String[] params);

    static String getCommand(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts[0].isEmpty()) throw new RORFunctionException(EX_NULL);
        return parts[0].toLowerCase(Locale.US);
    }

    static String[] getParams(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts.length == 1) return new String[0];
        return parts[1].split(PARAM_SEPARATOR);
    }

    static int parseInt(String value, int min, int max) {
        int result;
        try {
            result = Integer.parseInt(value);
        } catch (Exception first) {
            try {
                result = (int) Math.round(Double.parseDouble(value));
            } catch (Exception second) {
                throw new RORFunctionException(EX_FORMAT);
            }
        }
        if (result < min || result > max) throw new RORFunctionException(EX_FORMAT);
        return result;
    }

    static long parseLong(String value, long min, long max) {
        long result;
        try {
            result = Long.parseLong(value);
        } catch (Exception first) {
            try {
                result = Math.round(Double.parseDouble(value));
            } catch (Exception second) {
                throw new RORFunctionException(EX_FORMAT);
            }
        }
        if (result < min || result > max) throw new RORFunctionException(EX_FORMAT);
        return result;
    }

    static boolean parseBool(String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "1":
            case "true":
            case "on":
                return true;
            case "0":
            case "false":
            case "off":
                return false;
            default:
                throw new RORFunctionException(EX_FORMAT);
        }
    }
}
