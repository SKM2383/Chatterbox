package model.net.utilities;

import java.util.List;

public class ChatterExtractor {

    public static String extract(String[] arrayToExtract, int startIndex, int endIndex, String delimeter){
        StringBuilder sb = new StringBuilder();

        // Append all elements from startIndex to end Index into the StringBuilder
        for(int i = startIndex; i < endIndex; i++){
            sb.append(arrayToExtract[i] + delimeter);
        }

        return sb.toString();
    }

    public static String extract(List<String> listOfStrings, int start, int end, String delimeter){
        StringBuilder sb = new StringBuilder();

        for(int i = start; i < end; i++){
            sb.append(listOfStrings.get(i) + delimeter);
        }

        return sb.toString();
    }
}
