package com.mud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServicePartitionTest {

    private Object service;
    private Method partitionMethod;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = AnalysisService.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        service = constructor.newInstance(null, null, null, null, null, null, null, null);

        partitionMethod = AnalysisService.class.getDeclaredMethod("partition", List.class, int.class);
        partitionMethod.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private <T> List<List<T>> partition(List<T> list, int size) throws Exception {
        return (List<List<T>>) partitionMethod.invoke(service, list, size);
    }

    @Test
    @DisplayName("균등 분할: 10개 → size=5 → [5,5]")
    void evenPartition() throws Exception {
        List<List<Integer>> result = partition(List.of(1,2,3,4,5,6,7,8,9,10), 5);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(5);
        assertThat(result.get(1)).hasSize(5);
    }

    @Test
    @DisplayName("불균등 분할: 10개 → size=3 → [3,3,3,1]")
    void unevenPartition() throws Exception {
        List<List<Integer>> result = partition(List.of(1,2,3,4,5,6,7,8,9,10), 3);
        assertThat(result).hasSize(4);
        assertThat(result.get(3)).hasSize(1);
    }

    @Test
    @DisplayName("빈 리스트 → 빈 결과")
    void emptyList() throws Exception {
        List<List<Integer>> result = partition(List.of(), 5);
        assertThat(result).isEmpty();
    }
}
