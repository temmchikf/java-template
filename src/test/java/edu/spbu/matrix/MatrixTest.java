package edu.spbu.matrix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MatrixTest
{
  /**
   * ожидается 4 таких теста
   */
  @Test
  public void mulDD() {
    Matrix m1 = new DenseMatrix("m1.txt");
    Matrix m2 = new DenseMatrix("m2.txt");
    Matrix expected = new DenseMatrix("result.txt");
    Matrix actual = m1.dmul(m2);
    assertEquals(expected, actual);
  }

  @Test
  public void mulSS() {
    Matrix m1 = new SparseMatrix("s1.txt");
    Matrix m2 = new SparseMatrix("s2.txt");
    Matrix expected = new SparseMatrix("s1s2result.txt");
    Matrix actual = m1.dmul(m2);
    assertEquals(expected, actual);
  }

  @Test
  public void mulSD() {
    Matrix m1 = new SparseMatrix("s1.txt");
    Matrix m2 = new DenseMatrix("m2.txt");
    Matrix expected = new DenseMatrix("sdresult.txt");
    Matrix actual = m1.mul(m2);
    assertEquals(expected, actual);
  }

  @Test
  public void mulDS() {
    Matrix m1 = new DenseMatrix("m1.txt");
    Matrix m2 = new SparseMatrix("s2.txt");
    Matrix expected = new DenseMatrix("dsresult.txt");
    Matrix actual = m1.mul(m2);
    assertEquals(expected, actual);

  }
}
