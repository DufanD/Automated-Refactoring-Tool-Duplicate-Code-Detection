package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service;

import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.CloneCandidate;

import java.util.List;

/**
 * @author Dufan Quraish
 * @version 1.0.0
 * @since 23 October 2019
 */

public interface DuplicateCodeAnalyzer {

    float analysis(List<List<String>> listSplitedString1, List<List<String>> listSplitedString2);

    float analysis(CloneCandidate cloneCandidate1, CloneCandidate cloneCandidate2);
}
