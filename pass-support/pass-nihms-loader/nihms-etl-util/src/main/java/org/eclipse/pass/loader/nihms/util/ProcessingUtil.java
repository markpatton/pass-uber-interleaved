/*
 * Copyright 2023 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.pass.loader.nihms.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * General small utilities to tidy up data processing code.
 *
 * @author Karen Hanson
 */
public class ProcessingUtil {

    private ProcessingUtil () {
        //never called
    }

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Returns true if a string is empty or null
     *
     * @param str the string, may be {@code null}
     * @return true if a string is empty or null
     */
    public static boolean nullOrEmpty(String str) {
        return (str == null || str.isEmpty());
    }

    /**
     * Returns true if a collection has 0 rows or is null
     *
     * @param collection the collection, may be {@code null}
     * @return true if a collection has 0 rows or is null
     */
    public static boolean nullOrEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    /**
     * Formats a date to a ZonedDateTime according to pattern provided. If pattern is null, defaults to yyyy-MM-dd.
     * Returns null if no date passed in
     *
     * @param date    a date
     * @param pattern e.g. MM/dd/yyyy
     * @return the DateTime for the supplied {@code date}
     */
    public static ZonedDateTime formatDate(String date, String pattern) {
        if (nullOrEmpty(date)) {
            return null;
        }
        if (nullOrEmpty(pattern)) {
            pattern = DEFAULT_DATE_PATTERN;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDate ld = LocalDate.parse(date, formatter);
        ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("UTC"));
        return zdt;
    }

}
