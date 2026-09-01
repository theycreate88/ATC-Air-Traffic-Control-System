package com.atc.util;

import com.atc.model.Flight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom (non-library) sorting algorithms used to sort the Flights table
 * by Flight ID, Departure Time, or Arrival Time, as required by the spec.
 * Both operate in-place on a List<Flight>.
 */
public final class SortUtils {

    private SortUtils() {
    }

    // ---------------------------------------------------------------
    // Quick Sort
    // ---------------------------------------------------------------
    public static void quickSort(List<Flight> list, Comparator<Flight> cmp) {
        if (list == null || list.size() < 2) return;
        quickSort(list, 0, list.size() - 1, cmp);
    }

    private static void quickSort(List<Flight> list, int low, int high, Comparator<Flight> cmp) {
        if (low < high) {
            int pivotIndex = partition(list, low, high, cmp);
            quickSort(list, low, pivotIndex - 1, cmp);
            quickSort(list, pivotIndex + 1, high, cmp);
        }
    }

    private static int partition(List<Flight> list, int low, int high, Comparator<Flight> cmp) {
        Flight pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    private static void swap(List<Flight> list, int i, int j) {
        Flight tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // ---------------------------------------------------------------
    // Merge Sort
    // ---------------------------------------------------------------
    public static void mergeSort(List<Flight> list, Comparator<Flight> cmp) {
        if (list == null || list.size() < 2) return;
        List<Flight> sorted = mergeSortRecursive(list, cmp);
        for (int i = 0; i < sorted.size(); i++) {
            list.set(i, sorted.get(i));
        }
    }

    private static List<Flight> mergeSortRecursive(List<Flight> list, Comparator<Flight> cmp) {
        if (list.size() <= 1) return list;
        int mid = list.size() / 2;
        List<Flight> left = mergeSortRecursive(new ArrayList<>(list.subList(0, mid)), cmp);
        List<Flight> right = mergeSortRecursive(new ArrayList<>(list.subList(mid, list.size())), cmp);
        return merge(left, right, cmp);
    }

    private static List<Flight> merge(List<Flight> left, List<Flight> right, Comparator<Flight> cmp) {
        List<Flight> result = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        while (i < left.size()) result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        return result;
    }

    // ---------------------------------------------------------------
    // Ready-made comparators for the required sort keys
    // ---------------------------------------------------------------
    public static final Comparator<Flight> BY_FLIGHT_ID =
            Comparator.comparing(Flight::getFlightId);

    public static final Comparator<Flight> BY_DEPARTURE_TIME =
            Comparator.comparingLong(Flight::getDepartureTime);

    public static final Comparator<Flight> BY_ARRIVAL_TIME =
            Comparator.comparingLong(Flight::getExpectedArrivalTime);
}
