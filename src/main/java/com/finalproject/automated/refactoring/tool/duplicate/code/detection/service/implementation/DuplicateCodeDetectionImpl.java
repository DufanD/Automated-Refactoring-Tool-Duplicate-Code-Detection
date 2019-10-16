package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.ClonePair;
import com.finalproject.automated.refactoring.tool.locs.detection.service.LocsDetection;
import com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.DuplicateCodeDetection;
import com.finalproject.automated.refactoring.tool.model.CodeSmellName;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.CloneCandidate;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Dufan Quraish
 * @version 1.0.0
 * @since 19 August 2019
 */

@Service
public class DuplicateCodeDetectionImpl implements DuplicateCodeDetection {

    @Autowired
    private LocsDetection locsDetection;

    private ClonePair clonePair;

    private int maxRange = 0;

    private float thresholdUniqness = 0.3f;

    @Override
    public List<ClonePair> detect(@NonNull MethodModel methodModel, @NonNull Long threshold) {
        return detect(Collections.singletonList(methodModel), threshold);
    }

    @Override
    public List<ClonePair> detect(@NonNull List<MethodModel> methodModels, @NonNull Long threshold) {
        methodModels.parallelStream().map(this::detectLoc);
        findMaxRange(methodModels);
        return Stream.of(findCandidates(methodModels, threshold))
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
    }

    private void findMaxRange(List<MethodModel> methodModels) {
        for (MethodModel methodModel : methodModels) {
            if (maxRange < methodModel.getStatements().size())
                maxRange = methodModel.getStatements().size();
        }
    }

    private List<ClonePair> findCandidates(List<MethodModel> methodModels, Long threshold) {
        int INITIAL_INDEX = 0;
        List<CloneCandidate> cloneCandidates = new ArrayList<>();

        for (MethodModel methodModel : methodModels) {
            int CURRENT_INDEX = INITIAL_INDEX;
            int RANGE = threshold.intValue();
            int NEXT_INDEX = CURRENT_INDEX + RANGE;

            while (NEXT_INDEX <= methodModel.getStatements().size()) {
                CloneCandidate cloneCandidate = CloneCandidate.builder()
                        .methodModel(methodModel)
                        .statements(methodModel.getStatements().subList(CURRENT_INDEX, NEXT_INDEX))
                        .build();
                cloneCandidates.add(cloneCandidate);
                CURRENT_INDEX++;
                NEXT_INDEX = CURRENT_INDEX + RANGE;
            }
        }

        if (threshold < maxRange)
            return findCandidates(methodModels, threshold++);
        else
            return findClonePair(cloneCandidates);
    }

    private List<ClonePair> findClonePair(List<CloneCandidate> cloneCandidates) {
        return  cloneCandidates.parallelStream()
                    .map(cloneCandidate -> comparingCandidates(cloneCandidate, cloneCandidates))
                    .collect(Collectors.toList());
    }

    private ClonePair comparingCandidates(CloneCandidate cloneCandidate, List<CloneCandidate> cloneCandidates) {
        int NEXT_COMPARED = cloneCandidates.indexOf(cloneCandidate) + 1;
        ClonePair clonePair = ClonePair.builder().build();

        for (int i = NEXT_COMPARED; i < cloneCandidates.size(); i++) {
            if (isDuplicate(cloneCandidate, cloneCandidates.get(i))) {
                if (clonePair.getCloneCandidates().isEmpty()) {
                    clonePair.getCloneCandidates().add(cloneCandidate);
                }
                clonePair.getCloneCandidates().add(cloneCandidates.get(i));
            }
        }

        if (clonePair.getCloneCandidates().isEmpty())
            return null;
        else
            return clonePair;
    }

    private Boolean isDuplicate(CloneCandidate cloneCandidate1, CloneCandidate cloneCandidate2) {
        return scoreUPI(cloneCandidate1, cloneCandidate2) < thresholdUniqness;
    }

    private float scoreUPI(CloneCandidate cloneCandidate1, CloneCandidate cloneCandidate2) {
        List<List<String>> listSplitedString1 = transformToText(cloneCandidate1);
        List<List<String>> listSplitedString2 = transformToText(cloneCandidate2);
        int N_UNIQ, TOTAL_UPI = 0;
        int ABSOLUTE_UNIQ = 1;

        for (int i = 0; i < listSplitedString1.size(); i++) {
            N_UNIQ = TOTAL_UPI = 0;
            List<String> splitedString1 = listSplitedString1.get(i);
            List<String> splitedString2 = listSplitedString2.get(i);

            if (splitedString1.size() == splitedString2.size()) {
                for (int j = 0; j < splitedString1.size(); j++) {
                    if (!analyzeDuplicateTypeTwo(splitedString1.get(j), splitedString2.get(j))) {
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

    private Boolean analyzeDuplicateTypeTwo(String firstWord, String secondWord) {
        // analyze comparing candidate

        return false;
    }

    private Boolean analyzeIdentifier(String firstWord, String secondWord) {

        return false;
    }

    private Boolean analyzeType(String firstWord, String secondWord) {
        return false;
    }

    private Boolean analyzeLiteral(String firstWord, String secondWord) {
        return false;
    }

    private float upiFormula(int nUniq, int nWord) {
        return nUniq / nWord;
    }

    private List<List<String>> transformToText(CloneCandidate cloneCandidate) {
        List<List<String>> listSplitedString = new ArrayList<>();

        for (StatementModel statementModel : cloneCandidate.getStatements()) {
            List<String> splitString = Arrays.asList(statementModel.getStatement().split("[=;({]"));
            listSplitedString.add(splitString);
        }
        return listSplitedString;
    }

    private MethodModel detectLoc(MethodModel methodModel) {
        Long loc = locsDetection.llocDetection(methodModel.getBody());
        methodModel.setLoc(loc);

        return methodModel;
    }
}
