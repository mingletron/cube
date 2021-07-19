package com.mingletron;

import javafx.application.Application;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ColorPicker;
import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

/**
 * JavaFX App
 */
public class App extends Application {

    double WINDOW_WIDTH = 1000;
    double WINDOW_HEIGHT = 1000;
    Canvas canvas;
    float thetaDelta = 0.01f;
    Scene scene3d;
    String meshName = "";
    Color meshColor = Color.YELLOW;

    private Mesh meshCube = new Mesh();

    // System times in millis
    long t1;
    long t2;
    static String file;

    AnchorPane pane;

    int triangles = 0;
    boolean triangleColourMode = false;
    boolean filledTriangles = false;
    boolean clipping = true;

    public static void main(String[] args) {
        if (args.length > 0) {
            file = args[0];
        }
        launch();
    }

    @Override
    public void start(Stage primaryStage) {

        createMesh();

        // Projection Matrix
        matProj.m[0][0] = fAspectRatio * fFovRad;
        matProj.m[1][1] = fFovRad;
        matProj.m[2][2] = fFar / (fFar / fNear);
        matProj.m[3][2] = (-fFar * fNear) / (fFar - fNear);
        matProj.m[2][3] = 1.0f;
        matProj.m[3][3] = 0.0f;

        canvas = new Canvas();
        canvas.setHeight(WINDOW_HEIGHT);
        canvas.setWidth(WINDOW_WIDTH);

        GraphicsContext graphicsContext2D = canvas.getGraphicsContext2D();

        drawShapes(graphicsContext2D, "#00ff00");

        // VBox vbox = new VBox(canvas);
        var loadButton = new Button("Load");
        ColorPicker colorPicker = new ColorPicker();

        colorPicker.setValue(meshColor);

        pane = new AnchorPane(canvas);
        var meshLabel = new Label("Default Cube");
        meshLabel.setTextFill(Color.WHITE);
        var triLabel = new Label("Triangles: " + triangles);
        triLabel.setTextFill(Color.WHITE);
        var filledCheckBox = new CheckBox();
        filledCheckBox.setText("Filled");
        filledCheckBox.setTextFill(Color.WHITE);
        
        pane.getChildren().add(loadButton);
        pane.getChildren().add(meshLabel);
        pane.getChildren().add(colorPicker);
        pane.getChildren().add(triLabel);
        pane.getChildren().add(filledCheckBox);
      
        pane.setTopAnchor(meshLabel, 5.0);
        pane.setLeftAnchor(meshLabel, 60.0);
        pane.setRightAnchor(colorPicker, 10.0);
        pane.setTopAnchor(triLabel, 5.0);
        pane.setRightAnchor(triLabel, 200.0);
        pane.setLeftAnchor(filledCheckBox, 200.0);
        pane.setTopAnchor(filledCheckBox, 5.0);
       

        scene3d = new Scene(pane);
        primaryStage.setScene(scene3d);
        primaryStage.show();

        colorPicker.setOnAction(action -> {
            meshColor = colorPicker.getValue();
        });

        filledCheckBox.setOnAction(action -> {
            filledCheckBox.setSelected(!filledTriangles);
            filledTriangles = !filledTriangles;
        });


        loadButton.setOnAction(action -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("3D Mesh", "*.obj"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            try {
                meshCube = loadMesh(selectedFile);
                triangles = this.meshCube.tris.size();
                meshLabel.setText(meshName);
                triLabel.setText("Triangles: " + triangles);
                colorPicker.setValue(meshColor);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        });

        triLabel.setOnMouseClicked(mouse -> {
            triangleColourMode = !triangleColourMode;
        });

        ArrayList<String> input = new ArrayList<String>();

        scene3d.setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                String code = e.getCode().toString();
                if (code.equals("ESCAPE")) {
                    System.exit(0);
                }

                // only add once... prevent duplicates
                if (!input.contains(code))
                    input.add(code);
            }
        });

        scene3d.setOnKeyReleased(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                String code = e.getCode().toString();
                input.remove(code);
                // System.out.println(code);
                switch (code) {

                    case "EQUALS":
                        thetaDelta += 0.005;
                        break;

                    case "MINUS":
                        thetaDelta -= 0.005;
                        break;

                    default:
                }
            }
        });

    }

    void drawShapes(GraphicsContext gc, String colour) {

        new AnimationTimer() {
            public void handle(long currentNanoTime) {

                t1 = System.currentTimeMillis();
                if (t2 - t1 < 50) {
                    fTheta += thetaDelta;
                    t2 = System.currentTimeMillis() + 50;
                }

                // background image clears canvas
                gc.setFill(Color.gray(0.5));
                gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
                var matRotZ = new Mat4x4();
                var matRotX = new Mat4x4();

                var vCamera = new Vec3d();
                // Rotation Z
                matRotZ.m[0][0] = (float) Math.cos(fTheta);
                matRotZ.m[0][1] = (float) Math.sin(fTheta);
                matRotZ.m[1][0] = (float) -Math.sin(fTheta);
                matRotZ.m[1][1] = (float) Math.cos(fTheta);
                matRotZ.m[2][2] = 1.0f;
                matRotZ.m[3][3] = 1.0f;

                // Rotation X
                matRotX.m[0][0] = 1.0f;
                matRotX.m[1][1] = (float) Math.cos(fTheta * 0.5f);
                matRotX.m[1][2] = (float) Math.sin(fTheta * 0.5f);
                matRotX.m[2][1] = (float) -Math.sin(fTheta * 0.5f);
                matRotX.m[2][2] = (float) Math.cos(fTheta * 0.5f);
                matRotX.m[3][3] = 1.0f;

                for (Triangle t : meshCube.tris) {

                    triRotatedZ.p[0] = multiplyMatrixVector(t.p[0], matRotZ);
                    triRotatedZ.p[1] = multiplyMatrixVector(t.p[1], matRotZ);
                    triRotatedZ.p[2] = multiplyMatrixVector(t.p[2], matRotZ);

                    triRotatedZX.p[0] = multiplyMatrixVector(triRotatedZ.p[0], matRotX);
                    triRotatedZX.p[1] = multiplyMatrixVector(triRotatedZ.p[1], matRotX);
                    triRotatedZX.p[2] = multiplyMatrixVector(triRotatedZ.p[2], matRotX);

                    triTranslated.p = triRotatedZX.p;
                    triTranslated.p[0].z = triRotatedZX.p[0].z + 3.0f;
                    triTranslated.p[1].z = triRotatedZX.p[1].z + 3.0f;
                    triTranslated.p[2].z = triRotatedZX.p[2].z + 3.0f;

                    var normal = new Vec3d();
                    var line1 = new Vec3d();
                    var line2 = new Vec3d();

                    line1.x = triTranslated.p[1].x - triTranslated.p[0].x;
                    line1.y = triTranslated.p[1].y - triTranslated.p[0].y;
                    line1.z = triTranslated.p[1].z - triTranslated.p[0].z;

                    line2.x = triTranslated.p[2].x - triTranslated.p[0].x;
                    line2.y = triTranslated.p[2].y - triTranslated.p[0].y;
                    line2.z = triTranslated.p[2].z - triTranslated.p[0].z;

                    // Calculate Normals
                    if (clipping) {
                        normal.x = line1.y * line2.z - line1.z * line2.y;
                        normal.y = line1.z * line2.x - line1.x * line2.z;
                        normal.z = line1.x * line2.y - line1.y * line2.x;

                        float l = (float) Math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z);
                        normal.x /= l;
                        normal.y /= l;
                        normal.z /= l;
                    }

                    // Normals can be used to determine which faces are visible by the camera.

                    if (!clipping || (normal.x * (triTranslated.p[0].x - vCamera.x)
                            + normal.y * (triTranslated.p[0].y - vCamera.y)
                            + normal.z * (triTranslated.p[0].z - vCamera.z) < 0.0)) {

                        // Illumination
                        Vec3d lightDirection = new Vec3d(0.0f, 0.0f, -1.0f);
                        float l = (float)  Math.sqrt(lightDirection.x * lightDirection.x + lightDirection.y * lightDirection.y + lightDirection.z * lightDirection.z);
                        lightDirection.x /= l; lightDirection.y /= l; lightDirection.z /= l;

                        float dp = normal.x * lightDirection.x + normal.y * lightDirection.y + normal.z * lightDirection.z;
                        triTranslated.colour = calculateColor(meshColor, dp);

                        // Project triangles from 3D --> 2D        
                        triProjected.p[0] = multiplyMatrixVector(triTranslated.p[0], matProj);
                        triProjected.p[1] = multiplyMatrixVector(triTranslated.p[1], matProj);
                        triProjected.p[2] = multiplyMatrixVector(triTranslated.p[2], matProj);
                        triProjected.colour = triTranslated.colour;

                        // Scale into view
                        triProjected.p[0].x += 1.0f;
                        triProjected.p[0].y += 1.0f;
                        triProjected.p[1].x += 1.0f;
                        triProjected.p[1].y += 1.0f;
                        triProjected.p[2].x += 1.0f;
                        triProjected.p[2].y += 1.0f;

                        triProjected.p[0].x *= 0.5f * (float) WINDOW_WIDTH;
                        triProjected.p[0].y *= 0.5f * (float) WINDOW_HEIGHT;
                        triProjected.p[1].x *= 0.5f * (float) WINDOW_WIDTH;
                        triProjected.p[1].y *= 0.5f * (float) WINDOW_HEIGHT;
                        triProjected.p[2].x *= 0.5f * (float) WINDOW_WIDTH;
                        triProjected.p[2].y *= 0.5f * (float) WINDOW_HEIGHT;

                        if (filledTriangles) {
                            gc.setStroke(triProjected.colour);
                            gc.setFill(triProjected.colour);
                            gc.setLineWidth(0);
                            gc.fillPolygon(
                                    new double[] { triProjected.p[0].x, triProjected.p[1].x, triProjected.p[2].x },
                                    new double[] { triProjected.p[0].y, triProjected.p[1].y, triProjected.p[2].y }, 3);

                        } else {

                            if (triangleColourMode) {
                                gc.setStroke(Color.RED);
                            } else {
                                gc.setStroke(triProjected.colour);
                            }
                            gc.strokeLine((int) triProjected.p[0].x, (int) triProjected.p[0].y,
                                    (int) triProjected.p[1].x, (int) triProjected.p[1].y);
                            if (triangleColourMode) {
                                gc.setStroke(Color.GREEN);
                            } else {
                                gc.setStroke(triProjected.colour);
                            }
                            gc.strokeLine((int) triProjected.p[1].x, (int) triProjected.p[1].y,
                                    (int) triProjected.p[2].x, (int) triProjected.p[2].y);
                            if (triangleColourMode) {
                                gc.setStroke(Color.BLUE);
                            } else {
                                gc.setStroke(triProjected.colour);
                            }
                            gc.strokeLine((int) triProjected.p[2].x, (int) triProjected.p[2].y,
                                    (int) triProjected.p[0].x, (int) triProjected.p[0].y);

                        }
                    }

                }

            }
        }.start();
    }

    // Distance from eyes to screen
    float fNear = 0.1f;
    // Furthest distance into the screen
    float fFar = 1000.0f;
    // Field of view in degrees
    float fFov = 90.0f;
    // Screen aspect ratio
    float fAspectRatio = (float) WINDOW_HEIGHT / (float) WINDOW_WIDTH;
    // Field of view in Radians
    float fFovRad = 1.0f / (float) Math.tan(fFov * 0.5f / 180.0f * Math.PI);
    // Projection Matrix
    Mat4x4 matProj = new Mat4x4();

    // Rotation angle
    float fTheta;

    boolean setUpDone = false;

    Triangle triProjected = new Triangle();
    Triangle triTranslated = new Triangle();
    Triangle triRotatedZ = new Triangle();
    Triangle triRotatedZX = new Triangle();

    /**
     * Multiply a Vector by a Matrix and return a new Vector.
     * 
     * @param i
     * @param m
     * @return
     */
    Vec3d multiplyMatrixVector(Vec3d i, Mat4x4 m) {
        var o = new Vec3d();
        o.x = i.x * m.m[0][0] + i.y * m.m[1][0] + i.z * m.m[2][0] + m.m[3][0];
        o.y = i.x * m.m[0][1] + i.y * m.m[1][1] + i.z * m.m[2][1] + m.m[3][1];
        o.z = i.x * m.m[0][2] + i.y * m.m[1][2] + i.z * m.m[2][2] + m.m[3][2];
        float w = i.x * m.m[0][3] + i.y * m.m[1][3] + i.z * m.m[2][3] + m.m[3][3];
        if (w != 0.0f) {
            o.x /= w;
            o.y /= w;
            o.z /= w;
        }
        return o;
    }

    /**
     * Create a mesh for a cube. Each face of the cube uses 2 triangles. All
     * triangles are defined in a clockwise direction.
     */
    public void createMesh() {
        // South face
        this.meshCube
                .add(new Triangle(new Vec3d(0.0f, 0.0f, 0.0f), new Vec3d(0.0f, 1.0f, 0.0f),
                        new Vec3d(1.0f, 1.0f, 0.0f)))
                .add(new Triangle(new Vec3d(0.0f, 0.0f, 0.0f), new Vec3d(1.0f, 1.0f, 0.0f),
                        new Vec3d(1.0f, 0.0f, 0.0f)));

        // East face
        this.meshCube
                .add(new Triangle(new Vec3d(1.0f, 0.0f, 0.0f), new Vec3d(1.0f, 1.0f, 0.0f),
                        new Vec3d(1.0f, 1.0f, 1.0f)))
                .add(new Triangle(new Vec3d(1.0f, 0.0f, 0.0f), new Vec3d(1.0f, 1.0f, 1.0f),
                        new Vec3d(1.0f, 0.0f, 1.0f)));

        // North face
        this.meshCube
                .add(new Triangle(new Vec3d(1.0f, 0.0f, 1.0f), new Vec3d(1.0f, 1.0f, 1.0f),
                        new Vec3d(0.0f, 1.0f, 1.0f)))
                .add(new Triangle(new Vec3d(1.0f, 0.0f, 1.0f), new Vec3d(0.0f, 1.0f, 1.0f),
                        new Vec3d(0.0f, 0.0f, 1.0f)));

        // West Face
        this.meshCube
                .add(new Triangle(new Vec3d(0.0f, 0.0f, 1.0f), new Vec3d(0.0f, 1.0f, 1.0f),
                        new Vec3d(0.0f, 1.0f, 0.0f)))
                .add(new Triangle(new Vec3d(0.0f, 0.0f, 1.0f), new Vec3d(0.0f, 1.0f, 0.0f),
                        new Vec3d(0.0f, 0.0f, 0.0f)));

        // Top face
        this.meshCube
                .add(new Triangle(new Vec3d(0.0f, 1.0f, 0.0f), new Vec3d(0.0f, 1.0f, 1.0f),
                        new Vec3d(1.0f, 1.0f, 1.0f)))
                .add(new Triangle(new Vec3d(0.0f, 1.0f, 0.0f), new Vec3d(1.0f, 1.0f, 1.0f),
                        new Vec3d(1.0f, 1.0f, 0.0f)));

        // Bottom face
        this.meshCube
                .add(new Triangle(new Vec3d(1.0f, 0.0f, 1.0f), new Vec3d(0.0f, 0.0f, 1.0f),
                        new Vec3d(0.0f, 0.0f, 0.0f)))
                .add(new Triangle(new Vec3d(1.0f, 0.0f, 1.0f), new Vec3d(0.0f, 0.0f, 0.0f),
                        new Vec3d(1.0f, 0.0f, 0.0f)));

        triangles = this.meshCube.tris.size();
    }

    /**
     * 3D Vector representing a point in 3d space
     */
    public static class Vec3d implements Serializable {
        float x;
        float y;
        float z;

        public Vec3d() {
        }

        public Vec3d(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * Triangle class containing an array of 3 Vec3ds
     */
    public static class Triangle implements Serializable {

        Vec3d[] p = new Vec3d[3];
        Color colour;

        public Triangle() {
        }

        public Triangle(Vec3d v1, Vec3d v2, Vec3d v3) {
            this.p = new Vec3d[] { v1, v2, v3 };
        }

        public Triangle(Vec3d v1, Vec3d v2, Vec3d v3, Color col) {
            this.p = new Vec3d[] { v1, v2, v3 };
            this.colour = col;
        }
    }

    /**
     * Mesh containing a list of Triangles
     */
    public static class Mesh implements Serializable {
        ArrayList<Triangle> tris;

        public Mesh() {
            this.tris = new ArrayList<>();
        }

        public List<Triangle> add(Triangle t) {
            this.tris.add(t);
            return tris;
        }

    }

    /**
     * Represents a 4x4 Matrix
     */
    public static class Mat4x4 implements Serializable {
        float[][] m = new float[4][4];
    }

    private Mesh loadMesh(File file) throws IOException {
        triangles = 0;
        List<Vec3d> vectorList = new ArrayList<Vec3d>();
        var mesh = new Mesh();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String currentLine = "";
        while ((currentLine = reader.readLine()) != null) {
            // System.out.println(currentLine);
            String[] split = currentLine.split(" ");
            switch (split[0]) {

                case "o":
                    meshName = split[1];
                    break;

                case "v":
                    // System.out.println(split);
                    vectorList
                            .add(new Vec3d(Float.valueOf(split[1]), Float.valueOf(split[2]), Float.valueOf(split[3])));
                    break;
                case "f":
                    // System.out.println(split);
                    if (split.length > 4) {
                        // quads
                        mesh.add(new Triangle(vectorList.get(getVertIndex(split[1])),
                                vectorList.get(getVertIndex(split[2])), vectorList.get(getVertIndex(split[3]))));

                        mesh.add(new Triangle(vectorList.get(getVertIndex(split[3])),
                                vectorList.get(getVertIndex(split[4])), vectorList.get(getVertIndex(split[1]))));

                    } else {
                        // triangles
                        mesh.add(new Triangle(vectorList.get(getVertIndex(split[1])),
                                vectorList.get(getVertIndex(split[2])), vectorList.get(getVertIndex(split[3]))));

                    }

            }
        }
        reader.close();
        getMaterial(file);
        return mesh;
    }

    private int getVertIndex(String element) {
        // ystem.out.println("Element: " + element);
        if (element.contains("/")) {
            return Integer.valueOf(element.split("/")[0]).intValue() - 1;

        } else {
            return Integer.valueOf(element).intValue() - 1;
        }

    }

    private void getMaterial(File file) throws IOException {
        String matFile = file.getAbsolutePath().replace(".obj", ".mtl");
        BufferedReader reader = new BufferedReader(new FileReader(matFile));
        String currentLine = "";
        while ((currentLine = reader.readLine()) != null) {
            // System.out.println(currentLine);
            String[] split = currentLine.split(" ");
            switch (split[0]) {
                case "Kd":
                    meshColor = new Color(Double.parseDouble(split[1]), Double.parseDouble(split[2]),
                            Double.parseDouble(split[3]), 1.0);
            }
        }
        reader.close();

    }

    private Color calculateColor(Color colour, float lum) {
        lum = Math.abs(lum);
        return new Color(colour.getRed()*lum,
        colour.getGreen()*lum,
        colour.getBlue()*lum,
        1.0);
    }

}
