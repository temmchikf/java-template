package edu.spbu.matrix;

import java.awt.*;
import java.io.*;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
/**
 * Разряженная матрица
 */

class SMuller implements Runnable {

  int start, step;
  SparseMatrix left, right, res;

  SMuller(int start, int step, SparseMatrix left, SparseMatrix right, SparseMatrix res) {
    this.start = start;
    this.step = step;
    this.left = left;
    this.right = right;
    this.res = res;
  }

  @Override
  public void run() {
    for (Point key : left.SpMat.keySet()) {
      for (int i = start; i < start + step; i++) {
        Point p1 = new Point(i, key.y);
        if (right.SpMat.containsKey(p1)) {
          Point p2 = new Point(key.x, i);
          res.update(p2, key, p1, res, left, right);
        }
      }
    }
  }
}

public class SparseMatrix implements Matrix
{
  public HashMap<Point, Double> SpMat;
  public int cols, rows;

  public SparseMatrix(HashMap<Point, Double> SpMat, int rows, int cols)
  {
    this.SpMat = SpMat;
    this.rows = rows;
    this.cols = cols;
  }

  synchronized public void update(Point p2, Point key, Point p1,SparseMatrix res, SparseMatrix left, SparseMatrix right ) {
    if(res.SpMat.containsKey(p2)){
      double t = res.get2(p2) + left.get2(key) * right.get2(p1);
      res.Put(p2, t);
    }
    else{
      double t = left.get2(key) * right.get2(p1);
      res.Put(p2, t);
    }
  }

  synchronized public Double get2(Point a){
    return (this.SpMat.get(a));
  }

  synchronized public void Put(Point a, Double b){
    this.SpMat.put(a,b);
  }


  /**
   * загружает матрицу из файла
   * @param fileName
   */
  public SparseMatrix(String fileName) {
    try{
      BufferedReader br = new BufferedReader(new FileReader(new File(fileName)));
      SpMat = new HashMap<>();
      String[] curr;
      String line = br.readLine();
      int len = 0;
      int h = 0;
      double elem;
      while(line!= null){
        curr = line.split(" ");
        len = curr.length;
        for(int i = 0; i<len; i++){
          if(!curr[0].isEmpty()){
            elem = Double.parseDouble(curr[i]);
            if(elem!=0){
              Point p = new Point(h, i);
              SpMat.put(p, elem);
            }
          }

        }
        h++;
        line = br.readLine();
      }
      cols = len;
      rows = h;
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  /**
   * однопоточное умнджение матриц
   * должно поддерживаться для всех 4-х вариантов
   *
   * @param o
   * @return
   */
  @Override public Matrix mul(Matrix o) {
    if (o instanceof SparseMatrix) {
      if (this.cols != ((SparseMatrix)o).rows) {
        throw new RuntimeException("Why do you want to multiply matrices of the wrong size?");
      }
      HashMap<Point, Double> res = new HashMap<>();
      SparseMatrix result = new SparseMatrix(res, this.rows, ((SparseMatrix)o).cols);
      SparseMatrix transpSM = ((SparseMatrix) o).transpose();

      for (Point key : SpMat.keySet()) {
        for (int i = 0; i < transpSM.rows; i++) {
          Point p1 = new Point(i, key.y);
          if (transpSM.SpMat.containsKey(p1)) {
            Point p2 = new Point(key.x, i);
            if (result.SpMat.containsKey(p2)) {
              double t = result.SpMat.get(p2) + SpMat.get(key) * transpSM.SpMat.get(p1);
              result.SpMat.put(p2, t);
            } else {
              double t = SpMat.get(key) * transpSM.SpMat.get(p1);
              result.SpMat.put(p2, t);
            }
          }
        }
      }
      return result;
    }
    else if (o instanceof DenseMatrix) {
      if(this.cols != ((DenseMatrix)o).rows) {
        throw new RuntimeException("Why do you want to multiply matrices of the wrong size?");
      }
      HashMap<Point, Double> res = new HashMap<>();
      SparseMatrix result = new SparseMatrix(res, this.rows, ((DenseMatrix)o).cols);
      DenseMatrix transpDM = ((DenseMatrix) o).transpose();

      for(Point key: SpMat.keySet()) {
        for(int i = 0; i < transpDM.rows; i++) {
          if(transpDM.denseMatrix[i][key.y] != 0) {
            Point pf = new Point(key.x, i);
            if(result.SpMat.containsKey(pf)) {
              double tmp = result.SpMat.get(pf) + SpMat.get(key) * transpDM.denseMatrix[i][key.y];
              result.SpMat.put(pf, tmp);
            }
            else {
              double tmp = SpMat.get(key) * transpDM.denseMatrix[i][key.y];
              result.SpMat.put(pf, tmp);
            }
          }
        }
      }
      return result;
    }
    return null;
  }

  public SparseMatrix transpose()
  {
    HashMap<Point,Double> transp =new HashMap<>();
    Point p;
    for(Point key:SpMat.keySet())
    {
      p=new Point(key.y, key.x);
      transp.put(p, SpMat.get(key));
    }
    return new SparseMatrix(transp, rows, cols);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(rows, cols);
    for(Point key: SpMat.keySet()){
      result += 31 + (SpMat.get(key).hashCode()<<2);
    }
    return result;
  }

  /**
   * многопоточное умножение матриц
   *
   * @param o
   * @return
   */
  @Override
  public Matrix dmul(Matrix o) {

    if(o instanceof SparseMatrix){

      if (this.cols != ((SparseMatrix)o).rows) {
        throw new RuntimeException("Введена неправильных размеров матрица");
      }

      HashMap<Point, Double> res = new HashMap<>();
      SparseMatrix result = new SparseMatrix(res, this.rows, ((SparseMatrix)o).cols);
      SparseMatrix sM = ((SparseMatrix) o).transpose();
      int step = sM.rows/4 + 1;
      ArrayList<Thread> threads = new ArrayList<>();


      for(int i=0;i< sM.rows;i+=step){
        SMuller smuller = new SMuller( i,step,this, sM, result) ;
        Thread t = new Thread(smuller);
        threads.add(t);
        t.start();
      }
      for(Thread th:threads){
        try {
          th.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }


      return (result);
    }

    return null;
  }

  /**
   * спавнивает с обоими вариантами
   * @param o
   * @return
   */
  @Override
  public boolean equals(Object o) {

    if(this==o)
      return true;

    if(o instanceof SparseMatrix){

      if(this.hashCode()!=(o.hashCode())){
        System.out.printf("%d/n", this.hashCode());
        System.out.printf("%d/n", o.hashCode());
        return false;
      }

      SparseMatrix sM = (SparseMatrix) o;

      if (rows != sM.rows || cols != sM.cols)
        return false;
      if(SpMat.size()==sM.SpMat.size()){
        for(Point key: SpMat.keySet()){
          if(!sM.SpMat.containsKey(key))
            return false;
          if (Math.abs(SpMat.get(key) - sM.SpMat.get(key)) != 0){
            return false;
          }
        }
        return true;
      }
      return false;
    }

    if(o instanceof DenseMatrix){
      DenseMatrix dM = (DenseMatrix) o;

      if (rows != dM.rows || cols != dM.cols)
        return false;

      int count=0;
      for(int i=0;i<dM.rows;i++)
        for(int j=0;j<dM.cols;j++)
          if(dM.denseMatrix[i][j]!=0)
            count++;
      if(count==SpMat.size()){
        for(Point key: SpMat.keySet()){
          if(dM.denseMatrix[key.x][key.y]==0)
            return false;
          if(dM.denseMatrix[key.x][key.y]!=SpMat.get(key))
            return false;
        }
        return true;
      }
      return false;
    }

    return false;
  }


}
