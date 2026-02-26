package fun.pasta.util.shader;


import fun.pasta.util.shader.impl.*;

public class Shaders {

    public static Shaders instance = new Shaders();

    public IShader vertex = new VertexGlsl();
    public IShader smooth = new SmoothGlsl();
    public IShader roundedHead = new RoundedHeadGlsl();
    public IShader roundedImage = new RoundedImageGlsl();
    public IShader rounded = new RoundedGlsl();
}