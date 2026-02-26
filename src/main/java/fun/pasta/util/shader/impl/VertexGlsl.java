package fun.pasta.util.shader.impl;

import fun.pasta.util.shader.impl.IShader;


public class VertexGlsl implements IShader {

    @Override
    public String glsl() {
        return """
                #version 120
                 void main() {
                     gl_TexCoord[0] = gl_MultiTexCoord0;
                     gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                 }
                """;
    }
}
