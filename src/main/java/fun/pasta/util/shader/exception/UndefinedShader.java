package fun.pasta.util.shader.exception;

public class UndefinedShader extends Throwable {

    private String shader;

    @Override
    public String getMessage() {
        return shader;
    }

    public UndefinedShader(String shader) {
        this.shader = shader;
    }
}