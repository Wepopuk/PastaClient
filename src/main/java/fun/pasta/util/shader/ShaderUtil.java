package fun.pasta.util.shader;


import fun.pasta.util.shader.exception.UndefinedShader;
import fun.pasta.util.shader.impl.IShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;

import java.io.*;
import java.util.stream.Collectors;

public class ShaderUtil {

    private final int programID;

    public static ShaderUtil smooth = new ShaderUtil("smooth");
    public static ShaderUtil roundedHead = new ShaderUtil("roundedHead");
    public static ShaderUtil roundedImage = new ShaderUtil("roundedImage");
    public static ShaderUtil rounded = new ShaderUtil("rounded");

    public ShaderUtil(String fragmentShaderLoc) {
        programID = ARBShaderObjects.glCreateProgramObjectARB();

        try {
            int fragmentShaderId = switch (fragmentShaderLoc) {
                case "smooth" -> createShader(Shaders.instance.smooth, GL20.GL_FRAGMENT_SHADER);
                case "rounded" -> createShader(Shaders.instance.rounded, GL20.GL_FRAGMENT_SHADER);
                case "roundedHead" -> createShader(Shaders.instance.roundedHead, GL20.GL_FRAGMENT_SHADER);
                case "roundedImage" -> createShader(Shaders.instance.roundedImage, GL20.GL_FRAGMENT_SHADER);
                default -> throw new UndefinedShader(fragmentShaderLoc);
            };

            ARBShaderObjects.glAttachObjectARB(programID, fragmentShaderId);

            ARBShaderObjects.glAttachObjectARB(programID, createShader(Shaders.instance.vertex, GL20.GL_VERTEX_SHADER));

            ARBShaderObjects.glLinkProgramARB(programID);

        } catch (UndefinedShader e) {
            e.fillInStackTrace();
        }
    }

    public void attach() {
        ARBShaderObjects.glUseProgramObjectARB(programID);
    }

    public void detach() {
        GL20.glUseProgram(0);
    }

    public void setUniform(String name, float... args) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programID, name);

        switch (args.length) {
            case 1 -> ARBShaderObjects.glUniform1fARB(loc, args[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
            default -> throw new IllegalArgumentException("Превышено кол.во аргументов: " + name);
        }
    }

    public void setUniform(String name, int... args) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programID, name);

        switch (args.length) {
            case 1 -> ARBShaderObjects.glUniform1iARB(loc, args[0]);
            case 2 -> ARBShaderObjects.glUniform2iARB(loc, args[0], args[1]);
            case 3 -> ARBShaderObjects.glUniform3iARB(loc, args[0], args[1], args[2]);
            case 4 -> ARBShaderObjects.glUniform4iARB(loc, args[0], args[1], args[2], args[3]);
            default -> throw new IllegalArgumentException("Превышено кол.во аргументов: " + name);
        }
    }

    private int createShader(IShader glsl, int shaderType) {
        int shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
        ARBShaderObjects.glShaderSourceARB(shader, readIn(new ByteArrayInputStream(glsl.glsl().getBytes())));
        ARBShaderObjects.glCompileShaderARB(shader);

        if (GL20.glGetShaderi(shader, 35713) == 0) {
            throw new IllegalStateException("Shader " + shaderType + " compile failed");
        }
        return shader;
    }

    private String readIn(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .map(line -> line + '\n')
                .collect(Collectors.joining());
    }
}