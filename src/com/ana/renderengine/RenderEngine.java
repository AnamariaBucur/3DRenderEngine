package com.ana.renderengine;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.awt.Color;

public class RenderEngine {
    public static void main(String[] args) {
        JFrame frame = new JFrame("3D Render Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        //slider for horizontal rotation
        JSlider headingSlider = new JSlider(-180, 180, 0);
        pane.add(headingSlider, BorderLayout.SOUTH);

        //slider for vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        //slider to control roll
        JSlider rollSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(rollSlider, BorderLayout.WEST);

        //slider to control FoV
        JSlider FoVSlider = new JSlider(30, 120, 60);
        pane.add(FoVSlider, BorderLayout.NORTH);

        //panel to display render results
        JPanel renderPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.PINK);
                g2.fillRect(0,0,getWidth(),getHeight());

                //base tetrahedron
                // create 4 triangles and add them to a list

                ArrayList<Triangle> tris = new ArrayList<>();
                tris.add(new Triangle(new Vertex(100,100,100),
                        new Vertex(-100,-100,100),
                        new Vertex(-100,100,-100),
                        Color.WHITE));
                tris.add(new Triangle(new Vertex(100,100,100),
                        new Vertex(-100,-100,100),
                        new Vertex(100,-100,-100),
                        Color.MAGENTA));
                tris.add(new Triangle(new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(100, 100, 100),
                        Color.CYAN));
                tris.add(new Triangle(new Vertex(-100, 100, -100),
                        new Vertex(100, -100, -100),
                        new Vertex(-100, -100, 100),
                        Color.BLUE));
                // This is where the tetrahedron gets inflated to a sphere.
                // An inflation point of 0 yields the original tetrahedron
                // and a higher value leads to a more accurate approximation.
                // The smallest value with an "accurate" approximation is 4.
                // Personally, there is a noticeable slowdown around 7 or higher.
                int inflationPoint = 4;
                for (int i = 0; i < inflationPoint; i++) {
                    tris = inflate(tris);
                }
                // Use matrices to transform view with sliders
                double heading = Math.toRadians(headingSlider.getValue());

                // Heading slider functionality:
                Matrix headingTransform = new Matrix(new double[] {
                        Math.cos(heading), 0, Math.sin(heading),
                        0, 1, 0,
                        -Math.sin(heading), 0, Math.cos(heading)
                });

                //pitch slider functionality
                double pitch = Math.toRadians(pitchSlider.getValue());
                Matrix pitchTransform = new Matrix(new double[] {
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch)
                });

                // roll slider functionality
                double roll = Math.toRadians(rollSlider.getValue());
                Matrix rollTransform = new Matrix(new double[] {
                        Math.cos(roll), -Math.sin(roll), 0,
                        Math.sin(roll),  Math.cos(roll), 0,
                        0, 0, 1
                });
                //combine matrices
                Matrix combinedTransform = headingTransform.multiply(pitchTransform).multiply(rollTransform);

                //projection params
                double fov = Math.toRadians(FoVSlider.getValue());
                double scale = getHeight() / (2 * Math.tan(fov / 2));
                double zOffset = 400;  // camera offset
//                double zNear = 1;
//                double zFar = 1000;
//                double aspectRatio = (double) getWidth() / getHeight();


                //create image to be specified/filled
                BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                double[] zBuffer = new double[image.getWidth() * image.getHeight()];
                // Initialize with extremely far away depths
                for (int i = 0; i < zBuffer.length; i++) {
                    zBuffer[i] = Double.NEGATIVE_INFINITY;
                }

                //Draw triangles
                for (Triangle t : tris) {
                    Vertex v1 = combinedTransform.transform(t.getV1());
                    Vertex v2 = combinedTransform.transform(t.getV2());
                    Vertex v3 = combinedTransform.transform(t.getV3());

                    // To fill in the triangles, we will rasterize (convert to a list of pixels)
                    // via barycentric coordinates. Real 3d engines use hardware rasterization,
                    // which is much more efficient, but we can't access the graphic card here,
                    // so we will do it manually.

                    // Since we are manually assessing each triangle, we must manually
                    // translate the vertices to be centered first.
                    for (Vertex v : new Vertex[]{v1, v2, v3}) {
                        double z = v.getZ() + zOffset;
                        v.setX(v.getX() * scale / z + getWidth() / 2);
                        v.setY(v.getY() * scale / z + getHeight() / 2);
                        v.setZ(z); // keep z positive
                    }

                    Vertex ab = new Vertex(
                            v2.getX() - v1.getX(),
                            v2.getY() - v1.getY(),
                            v2.getZ() - v1.getZ());
                    Vertex ac = new Vertex(
                            v3.getX() - v1.getX(),
                            v3.getY() - v1.getY(),
                            v3.getZ() - v1.getZ());
                    Vertex norm = new Vertex(
                            ab.getY() * ac.getZ() - ab.getZ() * ac.getY(),
                            ab.getZ() * ac.getX() - ab.getX() * ac.getZ(),
                            ab.getX() * ac.getY() - ab.getY() * ac.getX());
                    double normalLength = Math.sqrt(norm.getX() * norm.getX() + norm.getY() * norm.getY() + norm.getZ() * norm.getZ());
                    norm.setX(norm.getX() / normalLength);
                    norm.setY(norm.getY() / normalLength);
                    norm.setZ(norm.getZ() / normalLength);
                    double angleCos = Math.abs(norm.getZ());

                    // Compute rectangular bounds for triangle
                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.getX(), Math.min(v2.getX(), v3.getX()))));
                    int maxX = (int) Math.min(image.getWidth() - 1, Math.floor(Math.max(v1.getX(), Math.max(v2.getX(), v3.getX()))));
                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.getY(), Math.min(v2.getY(), v3.getY()))));
                    int maxY = (int) Math.min(image.getHeight() - 1, Math.floor(Math.max(v1.getY(), Math.max(v2.getY(), v3.getY()))));

                    double triangleArea = (v1.getY() - v3.getY()) * (v2.getX() - v3.getX())
                            + (v2.getY() - v3.getY()) * (v3.getX() - v1.getX());


                    // Color pixels of visible triangles
                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            double b1 = ((y - v3.getY()) * (v2.getX() - v3.getX()) + (v2.getY() - v3.getY()) * (v3.getX() - x)) / triangleArea;
                            double b2 = ((y - v1.getY()) * (v3.getX() - v1.getX()) + (v3.getY() - v1.getY()) * (v1.getX() - x)) / triangleArea;
                            double b3 = ((y - v2.getY()) * (v1.getX() - v2.getX()) + (v1.getY() - v2.getY()) * (v2.getX() - x)) / triangleArea;
                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                // Handle z-buffer
                                double depth = b1 * v1.getZ() + b2 * v2.getZ() + b3 * v3.getZ();
                                int zIndex = y * image.getWidth() + x;
                                if (zBuffer[zIndex] < depth) {
                                    image.setRGB(x, y, getShade(t.getColor(), angleCos).getRGB());
                                    zBuffer[zIndex] = depth;
                                }
                            }
                        }
                    }
                }
                g2.drawImage(image, 0, 0, null);
            }
        };
        pane.add(renderPanel, BorderLayout.CENTER);

        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());
        rollSlider.addChangeListener(e -> renderPanel.repaint());
        FoVSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(800, 800);
        frame.setVisible(true);
    }
    public static Color getShade(Color color, double shade) {
        double magentaLinear = Math.pow(color.getRed(), 2.4) * shade;
        double cyanLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int magenta = (int) Math.pow(magentaLinear, 1.0/2.4);
        int cyan = (int) Math.pow(cyanLinear, 1.0/2.4);
        int blue = (int) Math.pow(blueLinear, 1.0/2.4);

        magenta = Math.min(255, Math.max(0, magenta));
        cyan = Math.min(255, Math.max(0, cyan));
        blue = Math.min(255, Math.max(0, blue));

        return new Color(magenta, cyan, blue);
    }
    // As a bonus, we can create a spherical approximation from the tetrahedron.
    // To do so, we repeatedly subdivide each triangle into four smaller ones and
    // "inflate."
    public static ArrayList<Triangle> inflate(ArrayList<Triangle> tris) {
        ArrayList<Triangle> result = new ArrayList<>();
        for (Triangle t : tris) {
            Vertex m1 = new Vertex(
                    (t.getV1().getX() + t.getV2().getX()) / 2,
                    (t.getV1().getY() + t.getV2().getY()) / 2,
                    (t.getV1().getZ() + t.getV2().getZ()) / 2);
            Vertex m2 = new Vertex(
                    (t.getV2().getX() + t.getV3().getX()) / 2,
                    (t.getV2().getY() + t.getV3().getY()) / 2,
                    (t.getV2().getZ() + t.getV3().getZ()) / 2);
            Vertex m3 = new Vertex(
                    (t.getV1().getX() + t.getV3().getX()) / 2,
                    (t.getV1().getY() + t.getV3().getY()) / 2,
                    (t.getV1().getZ() + t.getV3().getZ()) / 2);
            result.add(new Triangle(t.getV1(), m1, m3, t.getColor()));
            result.add(new Triangle(t.getV2(), m1, m2, t.getColor()));
            result.add(new Triangle(t.getV3(), m2, m3, t.getColor()));
            result.add(new Triangle(m1, m2, m3, t.getColor()));
        }
        for (Triangle t : result) {
            for (Vertex v : new Vertex[]{t.getV1(), t.getV2(), t.getV3()}) {
                double l = Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY() + v.getZ() * v.getZ()) / Math.sqrt(30000);
                v.setX(v.getX() / l);
                v.setY(v.getY() / l);
                v.setZ(v.getZ() / l);
            }
        }
        return result;
    }
}
