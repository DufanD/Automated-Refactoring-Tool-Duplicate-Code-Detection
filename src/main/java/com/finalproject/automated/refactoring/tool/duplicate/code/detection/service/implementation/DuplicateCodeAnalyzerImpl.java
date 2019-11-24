package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.DuplicateCodeAnalyzer;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DuplicateCodeAnalyzerImpl implements DuplicateCodeAnalyzer {

    private List<String> identifier = Arrays.asList("private", "default", "public", "protected");

    private List<String> integralType = Arrays.asList("byte", "short", "int", "long", "float", "double");

    private String charType = "char";

    private String booleanType = "boolean";

    @Override
    public float analysis(List<List<String>> listSplitedString1, List<List<String>> listSplitedString2) {
        int N_UNIQ;
        float TOTAL_UPI = 0;
        int ABSOLUTE_UNIQ = 1;

        listSplitedString1 = filterIdentifier(listSplitedString1);
        listSplitedString2 = filterIdentifier(listSplitedString2);

        for (int i = 0; i < listSplitedString1.size(); i++) {
            N_UNIQ = 0;
            List<String> splitedString1 = listSplitedString1.get(i);
            List<String> splitedString2 = listSplitedString2.get(i);

            if (splitedString1.size() == splitedString2.size()) {
                for (int j = 0; j < splitedString1.size(); j++) {
                    if (containsType(splitedString1.get(j)) && containsType(splitedString2.get(j))) {
                        if (checkValue(splitedString1.get(j + 1), splitedString2.get(j + 1))) {
                            j++;
                        } else {
                            N_UNIQ += 2;
                        }
                    } else if (!splitedString1.get(j).equals(splitedString2.get(j))) {
                        N_UNIQ++;
                    }
                }
                TOTAL_UPI += upiFormula(N_UNIQ, splitedString1.size());
            } else {
                TOTAL_UPI += ABSOLUTE_UNIQ;
            }
        }

        return upiFormula(TOTAL_UPI, listSplitedString1.size());
    }

    private float upiFormula(int nUniq, int nWord) {
        return (float) nUniq / (float) nWord;
    }

    private float upiFormula(float nUniq, int nWord) {
        return nUniq / (float) nWord;
    }

    private List<List<String>> filterIdentifier(List<List<String>> listSplitedString) {
        return listSplitedString.stream()
                .map(listString -> listString.stream()
                        .map(text -> {
                            String[] arrayString = Arrays.stream(text.split(" "))
                                    .filter(y -> !identifier.contains(y))
                                    .toArray(String[]::new);
                            return String.join(" ", arrayString);
                        }).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private boolean checkValue(String text1, String text2) {
        text1 = text1.replace(";", "");
        text2 = text2.replace(";", "");
        return Double.parseDouble(text1) == Double.parseDouble(text2);
    }

    private boolean containsType(String text) {
        return text.contains("=") && (containsIntegralType(text) || containsCharType(text) || containsBooleanType(text));
    }

    private boolean containsIntegralType(String text) {
        return integralType.stream().filter(type -> text.contains(type)).count() >= 1;
    }

    private boolean containsCharType(String text) {
        return text.contains(charType);
    }

    private boolean containsBooleanType(String text) {
        return text.contains(booleanType);
    }
}
