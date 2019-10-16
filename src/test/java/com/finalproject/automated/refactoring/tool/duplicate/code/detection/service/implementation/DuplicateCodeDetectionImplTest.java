package com.finalproject.automated.refactoring.tool.duplicate.code.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.locs.detection.service.LocsDetection;
import com.finalproject.automated.refactoring.tool.model.CodeSmellName;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Dufan Quraish
 * @version 1.0.0
 * @since 19 August 2019
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class DuplicateCodeDetectionImplTest {

    @Autowired
    private DuplicateCodeDetectionImpl duplicateCodeDetection;

    @MockBean
    private LocsDetection locsDetection;

    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;
    private static final Integer LONG_METHOD_COUNT = 1;
    private static final Integer NORMAL_METHOD_COUNT = 1;
    private static final Integer EMPTY_COUNT = 0;
    private static final Integer ONCE_INVOCATION = 1;

    private static final Long THRESHOLD = 10L;
    private static final Long FIRST_INDEX_LOC = 2L;
    private static final Long SECOND_INDEX_LOC = 14L;

    private List<MethodModel> methodModels;

    @Before
    public void setUp() {
        methodModels = createMethodModels();

        when(locsDetection.llocDetection(eq(methodModels.get(FIRST_INDEX).getBody())))
                .thenReturn(FIRST_INDEX_LOC);
        when(locsDetection.llocDetection(eq(methodModels.get(SECOND_INDEX).getBody())))
                .thenReturn(SECOND_INDEX_LOC);
    }

    @Test
    public void detect_singleMethod_success() {
        duplicateCodeDetection.detect(methodModels.get(SECOND_INDEX), THRESHOLD);

        assertEquals(LONG_METHOD_COUNT.intValue(), methodModels.get(SECOND_INDEX).getCodeSmells().size());
        assertEquals(CodeSmellName.LONG_METHOD, methodModels.get(SECOND_INDEX).getCodeSmells().get(FIRST_INDEX));

        verify(locsDetection, times(ONCE_INVOCATION))
                .llocDetection(eq(methodModels.get(SECOND_INDEX).getBody()));
        verifyNoMoreInteractions(locsDetection);
    }

    @Test
    public void detect_singleMethod_success_notLongMethod() {
        duplicateCodeDetection.detect(methodModels.get(FIRST_INDEX), THRESHOLD);
        assertTrue(methodModels.get(FIRST_INDEX).getCodeSmells().isEmpty());

        verify(locsDetection, times(ONCE_INVOCATION))
                .llocDetection(eq(methodModels.get(FIRST_INDEX).getBody()));
        verifyNoMoreInteractions(locsDetection);
    }

    @Test
    public void detect_multiMethods_success() {
        duplicateCodeDetection.detect(methodModels, THRESHOLD);

        assertEquals(LONG_METHOD_COUNT.longValue(), getLongMethodsCount().longValue());
        assertEquals(NORMAL_METHOD_COUNT.longValue(), getNormalMethodsCount().longValue());

        verify(locsDetection, times(ONCE_INVOCATION))
                .llocDetection(eq(methodModels.get(FIRST_INDEX).getBody()));
        verify(locsDetection, times(ONCE_INVOCATION))
                .llocDetection(eq(methodModels.get(SECOND_INDEX).getBody()));
        verifyNoMoreInteractions(locsDetection);
    }

    @Test
    public void detect_multiMethods_success_notLongMethod() {
        methodModels.remove(SECOND_INDEX.intValue());
        duplicateCodeDetection.detect(methodModels, THRESHOLD);

        assertEquals(EMPTY_COUNT.longValue(), getLongMethodsCount().longValue());
        assertEquals(NORMAL_METHOD_COUNT.longValue(), getNormalMethodsCount().longValue());

        verify(locsDetection, times(ONCE_INVOCATION))
                .llocDetection(eq(methodModels.get(FIRST_INDEX).getBody()));
        verifyNoMoreInteractions(locsDetection);
    }

    @Test(expected = NullPointerException.class)
    public void detect_singleMethod_failed_emptyMethod() {
        MethodModel methodModel = null;
        duplicateCodeDetection.detect(methodModel, THRESHOLD);
    }

    @Test(expected = NullPointerException.class)
    public void detect_singleMethod_failed_emptyThreshold() {
        duplicateCodeDetection.detect(methodModels.get(SECOND_INDEX), null);
    }

    @Test(expected = NullPointerException.class)
    public void detect_multiMethods_failed_emptyMethods() {
        methodModels = null;
        duplicateCodeDetection.detect(methodModels, THRESHOLD);
    }

    @Test(expected = NullPointerException.class)
    public void detect_multiMethods_failed_emptyThreshold() {
        duplicateCodeDetection.detect(methodModels, null);
    }

    private List<MethodModel> createMethodModels() {
        List<MethodModel> methodModels = new ArrayList<>();

        methodModels.add(MethodModel.builder()
                .keywords(Collections.singletonList("public"))
                .name("EmailHelp")
                .parameters(Arrays.asList(
                        PropertyModel.builder()
                                .type("String")
                                .name("emailSubject")
                                .build(),
                        PropertyModel.builder()
                                .type("String")
                                .name("emailContent")
                                .build()))
                .exceptions(Arrays.asList("Exception", "IOException"))
                .body("\n" +
                        "       mEmailSubject = emailSubject;\n" +
                        "       mEmailContent = emailContent;\n" +
                        "\n")
                .build());

        methodModels.add(MethodModel.builder()
                .keywords(Collections.singletonList("public"))
                .returnType("MyResponse<Integer>")
                .name("addGiftInfoCategory")
                .parameters(Collections.singletonList(
                        PropertyModel.builder()
                                .type("GiftInfoCategory")
                                .name("giftInfoCategory")
                                .build()))
                .body("\n" +
                        "        String message;\n" +
                        "        int response;\n" +
                        "\n" +
                        "        try {\n" +
                        "            giftInfoCategory = mGiftInfoCategoryService.addGiftInfoCategory(giftInfoCategory);\n" +
                        "\n" +
                        "            boolean isSuccess = giftInfoCategory != null;\n" +
                        "            message = isSuccess ? \"Gift info category add success\" : \"Gift info category add failed\";\n" +
                        "            response = isSuccess ? 1 : 0;\n" +
                        "        } catch (DataIntegrityViolationException e) {\n" +
                        "            message = \"Gift info category add failed - Gift info category already exists\";\n" +
                        "            response = 0;\n" +
                        "        } catch (Exception e) {\n" +
                        "            message = \"Gift info category add failed - Internal Server Error\";\n" +
                        "            response = 0;\n" +
                        "        }\n" +
                        "\n" +
                        "        return new MyResponse<>(message, response);\n" +
                        "\n")
                .build());

        return methodModels;
    }

    private Long getLongMethodsCount() {
        return methodModels.stream()
                .filter(this::isLongMethod)
                .count();
    }

    private Long getNormalMethodsCount() {
        return methodModels.stream()
                .filter(methodModel -> !isLongMethod(methodModel))
                .count();
    }

    private Boolean isLongMethod(MethodModel methodModel) {
        return methodModel.getCodeSmells()
                .contains(CodeSmellName.LONG_METHOD);
    }
}