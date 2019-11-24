package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.ClonePair;
import com.finalproject.automated.refactoring.tool.locs.detection.service.LocsDetection;
import com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.DuplicateCodeDetection;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.CloneCandidate;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.DuplicateCodeAnalyzer;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Dufan Quraish
 * @version 1.0.0
 * @since 19 August 2019
 */

@Service
public class DuplicateCodeDetectionImpl implements DuplicateCodeDetection {

    @Autowired
    private LocsDetection locsDetection;

    @Autowired
    private DuplicateCodeAnalyzer duplicateCodeAnalyzer;

    private ClonePair clonePair;

    private int maxRange = 0;

    private float thresholdUniqness = 0.3f;

    @Override
    public Map<String, List<ClonePair>> detect(@NonNull Map<String, List<MethodModel>> methodModels, @NonNull Long threshold) {
        Map<String, List<ClonePair>> clonePairs = new HashMap<>();

        methodModels.entrySet()
                .stream()
                .forEach(entry -> {
                    entry.getValue().parallelStream().map(this::detectLoc);
                    setMaxRange(entry.getValue());
                    clonePairs.put(entry.getKey(), findCandidates(entry.getValue(), threshold));
                })
        ;

        return clonePairs;
    }

    private void setMaxRange(List<MethodModel> methodModels) {
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

        if (threshold < maxRange) {
            List<ClonePair> merged = new ArrayList<>(findClonePair(cloneCandidates));
            merged.addAll(findCandidates(methodModels, threshold + 1));
            return merged;
        }
        else
            return findClonePair(cloneCandidates);
    }

    private List<ClonePair> findClonePair(List<CloneCandidate> cloneCandidates) {
        return  cloneCandidates.parallelStream()
                    .map(cloneCandidate -> comparingCandidates(cloneCandidate, cloneCandidates))
                    .filter(Objects::nonNull)
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
        List<List<String>> listSplitedString1 = transformToText(cloneCandidate1);
        List<List<String>> listSplitedString2 = transformToText(cloneCandidate2);
        return duplicateCodeAnalyzer.analysis(listSplitedString1, listSplitedString2) < thresholdUniqness;
    }

    private List<List<String>> transformToText(CloneCandidate cloneCandidate) {
        List<List<String>> listSplitedString = new ArrayList<>();

        for (StatementModel statementModel : cloneCandidate.getStatements()) {
            List<String> splitString = Arrays.asList(statementModel.getStatement().split("(?<=[=;({])"));
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
