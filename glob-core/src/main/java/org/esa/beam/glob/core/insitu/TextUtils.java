package org.esa.beam.glob.core.insitu;

public class TextUtils {

    public static int indexOf(String[] textValues, String[] possibleValues) {
        for (String possibleValue : possibleValues) {
            for (int index = 0; index < textValues.length; index++) {
                if (possibleValue.equalsIgnoreCase(textValues[index])) {
                    return index;
                }
            }
        }
        return -1;
    }
}
