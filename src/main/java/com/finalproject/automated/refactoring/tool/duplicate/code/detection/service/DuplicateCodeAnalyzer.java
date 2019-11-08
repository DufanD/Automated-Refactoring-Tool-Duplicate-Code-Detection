package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service;

import java.util.List;

/**
 * @author Dufan Quraish
 * @version 1.0.0
 * @since 23 October 2019
 */

public interface DuplicateCodeAnalyzer {

    float analysis(List<List<String>> listSplitedString1, List<List<String>> listSplitedString2);
}
