package com.aquagrow.resourcemonitor.rules;

import com.aquagrow.resourcemonitor.model.Classification;

import java.util.List;

public record AnomalyResult(Classification level, List<String> reasons) {
}
