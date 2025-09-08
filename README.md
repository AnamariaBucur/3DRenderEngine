# 3D Render Engine

A simple **3D software rendering engine** written in Java.  
It demonstrates how to use basic matrix transformations, rasterization, and z-buffering to render a rotating tetrahedron/sphere approximation.

## Features
- Rendering of a tetrahedron, inflated into a sphere approximation
- Interactive sliders to control:
    - **Heading** 
    - **Pitch** 
    - **Roll**
    - **Field of View (FoV)**
- Custom **z-buffer implementation**
- Rasterization using **barycentric coordinates**
- Written using only **Java Swing & AWT**

## Requirements
- **Java 17+** (project tested with OpenJDK 24)
- No external dependencies

## Running
Compile and run:
```bash
javac -d out src/com/ana/renderengine/*.java
java -cp out com.ana.renderengine.RenderEngine
```
Or just run the main method of RenderEngine inside your IDE.

## Demo

Once started, use the sliders around the window to adjust the rotation and field of view.
The object will re-render in real-time.