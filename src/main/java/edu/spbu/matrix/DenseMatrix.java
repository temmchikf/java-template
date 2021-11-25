package edu.spbu.matrix;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.awt.*;
import java.util.HashMap;

/**
 * Плотная матрица
 */

class DMuller implements Runnable{

  int rows, cols;
  int end, start;
  DenseMatrix right, left,res;

  DMuller(int indexS, int indexEn,int cols, DenseMatrix right, DenseMatrix left, DenseMatrix res ){
    this.end = indexEn;
    this.start = indexS;
    this.cols = cols;
    this.right = right;
    this.left = left;
    this.res = res;
    this.rows = indexEn-indexS;
  }

  @Override
  public void run(){
    for(int i=start;i<end;i++) {
      for (int j = 0; j < res.cols; j++) {
        for (int k = 0; k < this.cols; k++) {
          res.denseMatrix[i][j] += left.denseMatrix[i][k] * right.denseMatrix[j][k];
        }
      }
    }
  }

}


public class DenseMatrix implements Matrix {
  int rows, cols;
  double[][] denseMatrix;

  public DenseMatrix(int rows, int cols) {
    this.rows = rows;
    this.cols = cols;
    this.denseMatrix = new double[rows][cols];
  }


  /**
   * @param fileName
   */
  public DenseMatrix(String fileName) {
    ArrayList<double[]> tmp = new ArrayList<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(fileName));
      String line = br.readLine();
      if (line == null) {
        throw new NullPointerException("Why do you want to read an empty matrix?");
      }
      String[] values = line.split(" ");
      this.cols = values.length;
      double[] array = Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
      tmp.add(array);
      this.rows = 1;
      while ((line = br.readLine()) != null) {
        values = line.split(" ");

        if(values.length != this.cols){
          throw new RuntimeException("Why do you want to read a matrix that has a different number of elements in a row?");
        }

        array = Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
        tmp.add(array);
        this.rows++;
      }
      this.denseMatrix = new double[this.rows][this.cols];
      for (int i = 0; i < this.rows; i++) {
        denseMatrix[i] = tmp.get(i);
      }
    } catch (IOException e) {
      System.out.println("Error of reading\n" + e.getMessage());
    }
  }

  /**
   * однопоточное умнджение матриц
   * должно поддерживаться для всех 4-х вариантов
   *
   * @param o
   * @return
   */
  @Override
  public Matrix mul(Matrix o) {
    if(o instanceof DenseMatrix) {
      if (this.cols != ((DenseMatrix) o).rows) {
        throw new RuntimeException("Why do you want to multiply matrices of the wrong size?");
      }

      DenseMatrix m2 = (DenseMatrix) o;
      int resRows = this.getRows();
      int resCols = m2.getCols();
      DenseMatrix res = new DenseMatrix(resRows, resCols);
      for (int i = 0; i < resRows; i++) {
        for (int j = 0; j < resCols; j++) {
          for (int k = 0; k < this.cols; k++) {
            res.denseMatrix[i][j] += this.denseMatrix[i][k] * m2.denseMatrix[k][j];
          }
        }
      }
      return res;
    }
    else if(o instanceof SparseMatrix){
      if(this.cols != ((SparseMatrix)o).rows) {
        throw new RuntimeException("Why do you want to multiply matrices of the wrong size?");
      }
      HashMap<Point, Double> res = new HashMap<>();
      SparseMatrix result = new SparseMatrix(res, this.rows, ((SparseMatrix)o).cols);
      SparseMatrix transpSM = ((SparseMatrix) o).transpose();

      for(Point key: transpSM.SpMat.keySet()) {
        for (int i = 0; i < this.rows; i++) {
          if(denseMatrix[i][key.y] != 0) {
            Point pf = new Point(i, key.x);
            if(result.SpMat.containsKey(pf)) {
              double tmp = result.SpMat.get(pf) + denseMatrix[i][key.y] * transpSM.SpMat.get(key);
              result.SpMat.put(pf, tmp);
            }
            else{
              double tmp = denseMatrix[i][key.y] * transpSM.SpMat.get(key);
              result.SpMat.put(pf, tmp);
            }
          }
        }
      }
      return result;
    }
    return null;
  }

  /**
   * многопоточное умножение матриц
   *
   * @param o
   * @return
   */

  @Override public Matrix dmul(Matrix o) {

    if(o instanceof DenseMatrix){

      if(this.cols!=((DenseMatrix)o).rows){
        throw new RuntimeException("Введена неправильных размеров матрица");
      }

      DenseMatrix result = new DenseMatrix(this.rows, ((DenseMatrix)o).cols);
      DenseMatrix dM = ((DenseMatrix)o).transpose();


      Thread[] threads = new Thread[4];
      int ost = rows%4;
      int j=0;
      for(int i=0;i<4;i++){
        j=rows/4;
        DMuller muller;
        if(i==ost&&i!=0) {
          muller = new DMuller(i * j, j * (i + 1) + ost, cols, dM,this, result);
        }
        else {
          muller = new DMuller(i * j, j * (i + 1), cols, dM,this, result);
        }
        threads[i]= new Thread(muller);
        threads[i].start();
      }
      for(int i=0;i<4;i++){
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      return(result);
    }

    return null;
  }

  /**
   * сравнивает с обоими вариантами
   * @param o
   * @return
   */
  @Override public boolean equals (Object o){

    if (this == o)
      return true;

    if (o instanceof DenseMatrix) {

      DenseMatrix dM = (DenseMatrix) o;

      //проверка на хэшкод
      if (this.hashCode() != (o.hashCode())) {
        return false;
      }

      if (rows != dM.rows || cols != dM.cols)
        return false;
      else {
        for (int i = 0; i < rows; i++)
          for (int j = 0; j < cols; j++) {
            if (denseMatrix[i][j] != dM.denseMatrix[i][j])
              return false;
          }
        return true; // не найдено неравных элементов
      }
    }

    if (o instanceof SparseMatrix) {
      SparseMatrix sM = (SparseMatrix) o;
      if (rows != sM.rows || cols != sM.cols)
        return false;

      for (Point key : sM.SpMat.keySet()) {
        if (denseMatrix[key.x][key.y] == 0)
          return false;
        if (denseMatrix[key.x][key.y] != sM.SpMat.get(key))
          return false;
      }
      return true;
    }

    return false;
  }

  @Override
  public String toString () {
    StringBuilder str = new StringBuilder();
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        str.append(denseMatrix[i][j]);
        str.append(" ");
      }
      str.append("\n");
    }
    return (str.toString());
  }

  public int getCols() {
      return this.cols;
    }

  public int getRows() {
      return this.rows;
    }

  public DenseMatrix transpose () {
        DenseMatrix transp = new DenseMatrix(cols, rows);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                transp.denseMatrix[i][j] = denseMatrix[j][i];
            }
        }
        return transp;
  }

  @Override
  public int hashCode () {
    int result = Objects.hash(rows, cols);
    result = 31 * result + Arrays.deepHashCode(denseMatrix);
    return result;
  }



}
