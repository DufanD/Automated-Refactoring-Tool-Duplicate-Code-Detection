package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.ClonePair;
import com.finalproject.automated.refactoring.tool.locs.detection.service.LocsDetection;
import com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.DuplicateCodeDetection;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
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

    int INITIAL_INDEX = 0;

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
                    clonePairs.put(entry.getKey(), findCandidates(entry, threshold));
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

    private List<ClonePair> findCandidates(Map.Entry<String, List<MethodModel>> entry, Long threshold) {
        List<MethodModel> methodModels = entry.getValue();
        List<CloneCandidate> cloneCandidates = new ArrayList<>();
        CloneCandidate cloneCandidate;

        for (MethodModel methodModel : methodModels) {
            int CURRENT_INDEX = INITIAL_INDEX;
            int RANGE = threshold.intValue();
            int NEXT_INDEX = CURRENT_INDEX + RANGE;

            while (NEXT_INDEX <= methodModel.getStatements().size()) {
                cloneCandidate = buildNewCandidate(methodModel, entry.getKey(), CURRENT_INDEX, NEXT_INDEX);
                cloneCandidates.add(cloneCandidate);
                CURRENT_INDEX++;
                NEXT_INDEX = CURRENT_INDEX + RANGE;
            }
            cloneCandidates.addAll(findCandidateFromBlocksOuter(methodModel, entry.getKey(), threshold));
        }

        if (threshold < maxRange) {
            List<ClonePair> merged = new ArrayList<>(findClonePair(cloneCandidates));
            merged.addAll(findCandidates(entry, threshold + 1));
            return merged;
        }
        else
            return findClonePair(cloneCandidates);
    }

    private List<ClonePair> findCandidatesBetweenClass(Map.Entry<String, List<MethodModel>> entry, Long threshold,
                                                       Map<String, List<MethodModel>> files) {
        List<CloneCandidate> cloneCandidates = new ArrayList<>();
        CloneCandidate cloneCandidate;
        int fileFlag = 0;

        for (String key : files.keySet()) {
            if (key.equals(entry.getKey()) || fileFlag == 1) {
                List<MethodModel> methodModels = entry.getValue();

                for (MethodModel methodModel : methodModels) {
                    int CURRENT_INDEX = INITIAL_INDEX;
                    int RANGE = threshold.intValue();
                    int NEXT_INDEX = CURRENT_INDEX + RANGE;

                    while (NEXT_INDEX <= methodModel.getStatements().size()) {
                        cloneCandidate = buildNewCandidate(methodModel, key, CURRENT_INDEX, NEXT_INDEX);
                        cloneCandidates.add(cloneCandidate);
                        CURRENT_INDEX++;
                        NEXT_INDEX = CURRENT_INDEX + RANGE;
                    }
                    cloneCandidates.addAll(findCandidateFromBlocksOuter(methodModel, key, threshold));
                }

                fileFlag = 1;
            }
        }

        if (threshold < maxRange) {
            List<ClonePair> merged = new ArrayList<>(findClonePair(cloneCandidates));
            merged.addAll(findCandidatesBetweenClass(entry, threshold + 1, files));
            return merged;
        }
        else
            return findClonePair(cloneCandidates);
    }

    private CloneCandidate buildNewCandidate(MethodModel methodModel, String path, int CURRENT_INDEX, int NEXT_INDEX) {
        return CloneCandidate.builder()
                .methodModel(methodModel)
                .statements(methodModel.getStatements().subList(CURRENT_INDEX, NEXT_INDEX))
                .path(path)
                .flag(0)
                .build();
    }

    private List<ClonePair> filterClonePairMemberCount(List<ClonePair> clonePairs) {
        return clonePairs.stream()
                .filter(clonePair -> clonePair.getCloneCandidates().size() > 2)
                .collect(Collectors.toList());
    }

    private List<CloneCandidate> findCandidateFromBlocksOuter(MethodModel methodModel, String path, Long threshold) {
        List<CloneCandidate> mergedCandidate = new ArrayList<>();
        for (StatementModel statementModel : methodModel.getStatements()) {
            if (statementModel instanceof BlockModel) {
                mergedCandidate.addAll(setCandidateFromBlocks((BlockModel) statementModel, methodModel, path,
                        threshold));
            }
        }
        return mergedCandidate;
    }

    private List<CloneCandidate> findCandidateFromBlocksInner(BlockModel blockModel, MethodModel methodModel,
                                                              String path, Long threshold) {
        List<CloneCandidate> mergedCandidate = new ArrayList<>();
        for (StatementModel statementModel : blockModel.getStatements()) {
            if (statementModel instanceof BlockModel) {
                mergedCandidate.addAll(setCandidateFromBlocks((BlockModel) statementModel, methodModel, path,
                        threshold));
            }
        }
        return mergedCandidate;
    }

    private List<CloneCandidate> setCandidateFromBlocks(BlockModel blockModel, MethodModel methodModel,
                                                        String path, Long threshold) {
        List<CloneCandidate> cloneCandidates = new ArrayList<>();
        CloneCandidate cloneCandidate;
        int CURRENT_INDEX = INITIAL_INDEX;
        int RANGE = threshold.intValue();
        int NEXT_INDEX = CURRENT_INDEX + RANGE;

        while (NEXT_INDEX <= blockModel.getStatements().size()) {
            cloneCandidate = buildNewCandidate(methodModel, path, CURRENT_INDEX, NEXT_INDEX);
            cloneCandidates.add(cloneCandidate);
            CURRENT_INDEX++;
            NEXT_INDEX = CURRENT_INDEX + RANGE;
        }
        cloneCandidates.addAll(findCandidateFromBlocksInner(blockModel, methodModel, path, threshold));

        return cloneCandidates;
    }

    private List<ClonePair> findClonePair(List<CloneCandidate> cloneCandidates) {
        return  cloneCandidates.stream()
                    .map(cloneCandidate -> comparingCandidates(cloneCandidate, cloneCandidates))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }

    private ClonePair comparingCandidates(CloneCandidate cloneCandidate, List<CloneCandidate> cloneCandidates) {
        int NEXT_COMPARED = cloneCandidates.indexOf(cloneCandidate) + 1;
        ClonePair clonePair = ClonePair.builder().build();

        if (cloneCandidate.getFlag() == 1) {
            return null;
        }

        for (int i = NEXT_COMPARED; i < cloneCandidates.size(); i++) {
            if (cloneCandidates.get(i).getFlag() == 1) {
                continue;
            } else if (isDuplicate(cloneCandidate, cloneCandidates.get(i))) {
                if (clonePair.getCloneCandidates().isEmpty()) {
                    clonePair.getCloneCandidates().add(cloneCandidate);
                    cloneCandidate.setFlag(1);
                }
                clonePair.getCloneCandidates().add(cloneCandidates.get(i));
                cloneCandidates.get(i).setFlag(1);
            }
        }

        if (clonePair.getCloneCandidates().isEmpty())
            return null;
        else
            return clonePair;
    }

    private Boolean isDuplicate(CloneCandidate cloneCandidate1, CloneCandidate cloneCandidate2) {
        return duplicateCodeAnalyzer.analysis(cloneCandidate1, cloneCandidate2) < thresholdUniqness;
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
