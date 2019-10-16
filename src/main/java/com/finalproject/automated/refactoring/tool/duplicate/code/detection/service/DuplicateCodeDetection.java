package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service;

import com.finalproject.automated.refactoring.tool.duplicate.code.detection.model.ClonePair;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import lombok.NonNull;

import java.util.List;

/**
 * @author Dufan Quraish
 * @version 1.0.0
 * @since 19 August 2019
 */

public interface DuplicateCodeDetection {

    List<ClonePair> detect(@NonNull MethodModel methodModel, @NonNull Long threshold);

    List<ClonePair> detect(@NonNull List<MethodModel> methodModels, @NonNull Long threshold);
}
