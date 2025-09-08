package com.ana.renderengine;

/**
 * This class handles the core mathematical operations for rotation.
 * It stores a 3x3 matrix and includes methods for multiplying matrices and transforming a vertex.
 */
public class Matrix {
    private double[] values;  //3x3 in column-major order
    public Matrix(double[] values) {
        if (values.length != 9) {
            throw new IllegalArgumentException("Matrix must have 9 elements");
        }
        this.values = values;
    }
    public Matrix multiply(Matrix other) {
        double[] result = new double[9];
        for (int row =0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                double sum = 0;
                for (int i = 0; i < 3; i++) {
                    sum += this.values[row + i * 3] * other.values[i + col * 3];
                }
                result[row + col * 3] = sum;
            }
        }
        return new Matrix(result);
    }
    public Vertex transform(Vertex in) {
        return new Vertex(
                in.getX() * values[0] + in.getY() * values[3] + in.getZ() * values[6],
                in.getX() * values[1] + in.getY() * values[4] + in.getZ() * values[7],
                in.getX() * values[2] + in.getY() * values[5] + in.getZ() * values[8]);
    }
}
