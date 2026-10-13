package com.aquagrow.mcp.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** The part of Spring Data's {@code Page} JSON the client needs: the rows and whether this is the last page. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiPage<T>(List<T> content, boolean last, int number) {
}
