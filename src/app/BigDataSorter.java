package app;
import sort.MergeSort;

public class BigDataSorter {
  // The size of initial segments. This is the largest amount of data that 
  // this code can store at any time.
  public static final int MAX_ARRAY_SIZE = 43;
  private Boolean firstTimeRunning = true;
  private int lastInt1;
  private int lastInt2;
  private DataSourceAccessor<Integer> dataAccessor;
  // temporary file names
  private String tempFile1 = "temp1.dat";
  private String tempFile2 = "temp2.dat";
  private String tempFile3 = "temp3.dat";
  
  // Initialize the data accessor object.
  public BigDataSorter(DataSourceAccessor<Integer> da){
      dataAccessor = da;
  }
  
  /*
   * High-level method to carry out the sort. Calls initSegments and merge
   * to carry out phase 1 and phase 2 respectively.
   */
  public void sort() throws Exception {
    // Implement Phase 1: Create initial segments
    int numberOfSegments = initSegments(MAX_ARRAY_SIZE);
    // Implement Phase 2: Merge segments recursively
    merge(numberOfSegments, MAX_ARRAY_SIZE, tempFile1, tempFile2, tempFile3);
  }

  /*
   * Implements phase 1. Data from the original file is processed in segments of 
   * size MAX_ARRAY_SIZE. To process a segment, tempFile1 
   * is opened for write, then data from the original file (which is already open for read)
   * is read in and stored in an array of ints of size MAX_ARRAY_SIZE. 
   * This array is sorted, and then read into tempFile1. This process is repeated until 
   * there is no more data in the original file. Note that the last segment may not be full.
   * A counter keeps track of the number of segments. This number is returned. 
   * Use MergeSort (in package sort) to sort the segments you read in to the array. 
   */
  public int initSegments (int initSegmentSize) throws Exception {
    int numberOfSegments = 0;
    dataAccessor.openTempDSforWrite(tempFile1);
    
    while (dataAccessor.origSrcHasNext()) {
      int intsAdded = 0;
      int[] tempArray = new int[initSegmentSize];

      for (int i = 0; i < tempArray.length && dataAccessor.origSrcHasNext(); i++) {
          tempArray[i] = dataAccessor.origSrcGetNext();
          intsAdded++;
      }

        if (intsAdded == initSegmentSize) {
          MergeSort.mergeSort(tempArray, 0, initSegmentSize - 1);

          for (int j = 0; j < intsAdded; j++) {
            dataAccessor.tempDSwriteNext(tempFile1, tempArray[j]);
          }

        } else {
          int[] shortArray = new int[intsAdded];

          for (int k = 0; k < shortArray.length; k++) {
            shortArray[k] = tempArray[k];
          }

          MergeSort.mergeSort(shortArray, 0, shortArray.length - 1);

          for (int j = 0; j < shortArray.length; j++) {
            dataAccessor.tempDSwriteNext(tempFile1, shortArray[j]);
          }
        }

      numberOfSegments++;
    }

    dataAccessor.tempDSclose(tempFile1);

    return numberOfSegments;
  }

  /*
   * Recursively merge the sorted segments until there is only 1 segment left.
   * Then, copy the file with the single, sorted segment to the result file.
   * This method calls mergeOneStep to carry out a merge, then makes the recursive
   * call to merge, where the number of segments has decreased by half, and the
   * segment size has doubled. The temporary files shift as well. The sorting is done 
   * when there is only one segment. The sorted data is in the file that f1 references, 
   * so copy f1 to the final sorted file.
   */
  public void merge(int numberOfSegments, int segmentSize, 
                      String f1, String f2, String f3) throws Exception {
    if (numberOfSegments > 1) {
      mergeOneStep(numberOfSegments, segmentSize, f1, f2, f3);
      merge((numberOfSegments + 1) / 2, segmentSize * 2, f3, f1, f2);
    }    
    else { //done sorting. 
       dataAccessor.openSortedForWrite();
       copyToResultFile(f1);
    }
  }

  /*
   * This method carries out the work of doing a merge operation. The first step is
   * to open file f1 for read, f2 for write, then call copyHalfFromF1ToF2 to write
   * the first half of the segments in f1 to f2, then f2 is closed. File fi is left 
   * open because it will be used in megeing segments and it's file curor is at the 
   * halfway point in the file after the copy procedure. The next step is to 
   * merge all segments in the second half of f1 with those in f2 and write the merged 
   * segments to f3. To do this, file f2 is open for read and f3 is open for write. Then,
   * mergeSegments is called with half the number of segments.
   * Finally, f1, f2, and f3 are closed.
   */ 
  public void mergeOneStep(int numberOfSegments, int segmentSize, 
                            String f1, String f2, String f3) throws Exception {
      //TODO 2: implement this method.
      dataAccessor.openTempDSforRead(f1);
      dataAccessor.openTempDSforWrite(f2);

      copyHalfFromF1ToF2(numberOfSegments, segmentSize, f1, f2);

      dataAccessor.tempDSclose(f2);
      dataAccessor.openTempDSforRead(f2);
      dataAccessor.openTempDSforWrite(f3);

      mergeSegments(numberOfSegments/2, segmentSize, f1, f2, f3);

      dataAccessor.tempDSclose(f1);
      dataAccessor.tempDSclose(f2);
      dataAccessor.tempDSclose(f3);

  }

  /* Copy the first half of the number of segments from f1 to f2.
   * Assume f1 is open for read, f2 is open for write. */
  public void copyHalfFromF1ToF2(int numberOfSegments, int segmentSize, 
                                  String f1, String f2) throws Exception {
    // TODO 3: Implement this method.
      for (int i=0; i < segmentSize*numberOfSegments/2; i++) {
        int number = dataAccessor.tempDSgetNext(f1);
        dataAccessor.tempDSwriteNext(f2, number);
      }
  }

  /* Merge all segments. Calls mergeTwoSegments until all but possibly the last
   * segment have been merged. If an odd segment remains in f1 it is written to f3.
   * Assume f1, f2 open for read, f3 for write.
  */
  public void mergeSegments(int numberOfSegments, int segmentSize, 
                             String f1, String f2, String f3) throws Exception {
    
    System.out.println(numberOfSegments);
    
    for (int i = 0; i < numberOfSegments; i++) {
      mergeTwoSegments(segmentSize, f1, f2, f3);
    }
    // If f1 has one extra segment, copy it to f3
    while (dataAccessor.tempDShasNext(f1)) {
       dataAccessor.tempDSwriteNext(f3, dataAccessor.tempDSgetNext(f1));
    }
  }

  /* Merges two segments fro f1 and f2, write the merged segments to f3. This is the 
   * basic merge algorithm from merge sort, except this version uses file streams 
   * instead of arrays. That means the segment size has to be tracked so the merge 
   * does not access integers outside of the segmentSize. 
   * See the instructions for more on this adaptation.
   */
  public void mergeTwoSegments(int segmentSize, String f1, String f2, String f3) throws Exception {
    //TODO 4: implement this method.

    int f1NumbersIterated = 0;
    int f2NumbersIterated = 0;

    int f1Integer;
    int f2Integer;

    if (firstTimeRunning) {
      f1Integer = dataAccessor.tempDSgetNext(f1);
      f2Integer = dataAccessor.tempDSgetNext(f2);
      firstTimeRunning = false;
    } else {
      f1Integer = lastInt1;
      f2Integer = lastInt2;
    }

    Boolean f1Empty = false;
    Boolean f2Empty = false;

    while (f1NumbersIterated < segmentSize && f2NumbersIterated < segmentSize) {

      if (f2Integer <= f1Integer) {
        dataAccessor.tempDSwriteNext(f3, f2Integer);

        f2NumbersIterated++;

        if (dataAccessor.tempDShasNext(f2)) {
          f2Integer = dataAccessor.tempDSgetNext(f2);
          lastInt2 = f2Integer;
        } else {
          f2Empty = true;
          break;
        }
      } else {
        dataAccessor.tempDSwriteNext(f3, f1Integer);

        f1NumbersIterated++;

        if (dataAccessor.tempDShasNext(f1)) {
          f1Integer = dataAccessor.tempDSgetNext(f1);
          lastInt1 = f1Integer;
        } else {
          f1Empty = true;
          break;
        }
      }
    }

    if (!f1Empty && !f2Empty) {
      if (f1NumbersIterated == segmentSize) {
        while (f2NumbersIterated < segmentSize) {

          dataAccessor.tempDSwriteNext(f3, f2Integer);
    
          f2NumbersIterated++;
            
          if (dataAccessor.tempDShasNext(f2)) {
            f2Integer = dataAccessor.tempDSgetNext(f2);
            lastInt2 = f2Integer;
          }
    
        }
      } else {
        while (f1NumbersIterated < segmentSize) {

          dataAccessor.tempDSwriteNext(f3, f1Integer);
    
          f1NumbersIterated++;
    
          if (dataAccessor.tempDShasNext(f1)) {
            f1Integer = dataAccessor.tempDSgetNext(f1);
            lastInt1 = f1Integer;
          }
    
        }
      }
    } else if (f1Empty) {
      while (f2NumbersIterated < segmentSize) {
        dataAccessor.tempDSwriteNext(f3, f2Integer);

        f2NumbersIterated++;

        if (dataAccessor.tempDShasNext(f2)) {
          dataAccessor.tempDSgetNext(f2);
        } else {
          break;
        }
      }
    } else if (f2Empty) {
      while (f1NumbersIterated < segmentSize) {
        dataAccessor.tempDSwriteNext(f3, f1Integer);

        f1NumbersIterated++;

        if (dataAccessor.tempDShasNext(f1)) {
          dataAccessor.tempDSgetNext(f1);
        } else {
          break;
        }
      }
    }

  }
  
  /*
   * Copies the data from the file referenced by the argument to the 
   * sorted file, which should contain all of the original data sorted in 
   * ascending order.
   */
  public void copyToResultFile(String fileName)throws Exception{
      // assume file closed- but try closing anyway
      dataAccessor.tempDSclose(fileName);
      dataAccessor.openTempDSforRead(fileName);
     // then re-open for read
      while(dataAccessor.tempDShasNext(fileName))
         dataAccessor.sortedWriteNext(dataAccessor.tempDSgetNext(fileName));
      dataAccessor.sortedDataClose();
      dataAccessor.tempDSclose(fileName);
  }
  
}