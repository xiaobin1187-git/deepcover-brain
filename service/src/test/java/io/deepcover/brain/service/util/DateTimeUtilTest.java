/*
 * Copyright 2024-2026 DeepCover
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
package io.deepcover.brain.service.util;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void getYMDByDate_noArgs_returnsTodayDateString() {
        String result = DateTimeUtil.getYMDByDate();

        String expected = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        assertEquals(expected, result);
    }

    @Test
    void getYMDByDate_withZero_returnsTodayDateString() {
        String result = DateTimeUtil.getYMDByDate(0);

        String expected = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        assertEquals(expected, result);
    }

    @Test
    void getYMDByDate_withNegativeOne_returnsYesterdayDateString() {
        String result = DateTimeUtil.getYMDByDate(-1);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String expected = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        assertEquals(expected, result);
    }

    @Test
    void getYMDByDate_withPositiveOne_returnsTomorrowDateString() {
        String result = DateTimeUtil.getYMDByDate(1);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String expected = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        assertEquals(expected, result);
    }

    @Test
    void getYMDByDate_resultFormat_isYyyyMmDd() {
        String result = DateTimeUtil.getYMDByDate(0);

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"),
                "Date format should be yyyy-MM-dd but was: " + result);
    }

    @Test
    void getDays_sameDay_returnsSingleDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();

        List<String> days = DateTimeUtil.getDays(today, today);

        assertEquals(1, days.size());
        assertEquals(sdf.format(today), days.get(0));
    }

    @Test
    void getDays_twoConsecutiveDays_returnsTwoDays() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DATE, 1);

        List<String> days = DateTimeUtil.getDays(start.getTime(), end.getTime());

        assertEquals(2, days.size());
    }

    @Test
    void getDays_threeDayRange_returnsThreeDays() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DATE, 2);

        List<String> days = DateTimeUtil.getDays(start.getTime(), end.getTime());

        assertEquals(3, days.size());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(sdf.format(start.getTime()), days.get(0));

        Calendar day2 = Calendar.getInstance();
        day2.add(Calendar.DATE, 1);
        assertEquals(sdf.format(day2.getTime()), days.get(1));

        assertEquals(sdf.format(end.getTime()), days.get(2));
    }

    @Test
    void getDays_includesBothStartAndEnd() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar startCal = Calendar.getInstance();
        startCal.set(2025, Calendar.JANUARY, 1);
        Calendar endCal = Calendar.getInstance();
        endCal.set(2025, Calendar.JANUARY, 3);

        List<String> days = DateTimeUtil.getDays(startCal.getTime(), endCal.getTime());

        assertEquals(3, days.size());
        assertEquals("2025-01-01", days.get(0));
        assertEquals("2025-01-02", days.get(1));
        assertEquals("2025-01-03", days.get(2));
    }

    @Test
    void getDays_timePortionIsIgnored() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 10);
        start.set(Calendar.MINUTE, 30);
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 18);
        end.set(Calendar.MINUTE, 45);

        List<String> days = DateTimeUtil.getDays(start.getTime(), end.getTime());

        assertEquals(1, days.size());
    }

    @Test
    void getThisDayStart_withZero_returnsTodayAtMidnight() {
        Date result = DateTimeUtil.getThisDayStart(0);

        assertNotNull(result);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, resultCal.get(Calendar.MINUTE));
        assertEquals(0, resultCal.get(Calendar.SECOND));
        assertEquals(0, resultCal.get(Calendar.MILLISECOND));

        Calendar expectedCal = Calendar.getInstance();
        assertEquals(expectedCal.get(Calendar.YEAR), resultCal.get(Calendar.YEAR));
        assertEquals(expectedCal.get(Calendar.MONTH), resultCal.get(Calendar.MONTH));
        assertEquals(expectedCal.get(Calendar.DAY_OF_MONTH), resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void getThisDayStart_withNegative_returnsPastDateAtMidnight() {
        Date result = DateTimeUtil.getThisDayStart(-5);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, resultCal.get(Calendar.MINUTE));
        assertEquals(0, resultCal.get(Calendar.SECOND));
        assertEquals(0, resultCal.get(Calendar.MILLISECOND));

        Calendar expectedCal = Calendar.getInstance();
        expectedCal.add(Calendar.DATE, -5);
        assertEquals(expectedCal.get(Calendar.YEAR), resultCal.get(Calendar.YEAR));
        assertEquals(expectedCal.get(Calendar.MONTH), resultCal.get(Calendar.MONTH));
        assertEquals(expectedCal.get(Calendar.DAY_OF_MONTH), resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void getThisDayStart_withPositive_returnsFutureDateAtMidnight() {
        Date result = DateTimeUtil.getThisDayStart(7);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, resultCal.get(Calendar.MINUTE));
        assertEquals(0, resultCal.get(Calendar.SECOND));
        assertEquals(0, resultCal.get(Calendar.MILLISECOND));

        Calendar expectedCal = Calendar.getInstance();
        expectedCal.add(Calendar.DATE, 7);
        assertEquals(expectedCal.get(Calendar.YEAR), resultCal.get(Calendar.YEAR));
        assertEquals(expectedCal.get(Calendar.MONTH), resultCal.get(Calendar.MONTH));
        assertEquals(expectedCal.get(Calendar.DAY_OF_MONTH), resultCal.get(Calendar.DAY_OF_MONTH));
    }
}
