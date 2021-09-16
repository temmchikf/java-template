package edu.spbu.sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by artemaliev on 07/09/15.
 */
public class IntSort {
  public static void sort (int arr[]) {
      int first = 0;
      int last = arr.length - 1;
      realSort(arr, first, last);
    }

    private static void realSort(int[] arr, int first, int last) {
      if (first < last) {
        int left = first, right = last, middle = arr[(left + right) / 2];
        while (left < right) {
          while (arr[left] < middle) left++;
          while (arr[right] > middle) right--;
          if (left <= right) {
            int tmp = arr[left];
            arr[left] = arr[right];
            arr[right] = tmp;
            left++;
            right--;
          }
        }
        realSort(arr, first, right);
        realSort(arr, left, last);
      }
    }

    public static void sort (List<Integer> list) {
    Collections.sort(list);
  }
}
