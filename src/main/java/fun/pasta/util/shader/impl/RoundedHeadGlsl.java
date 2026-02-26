package fun.pasta.util.shader.impl;

public class RoundedHeadGlsl implements IShader {

    @Override
    public String glsl() {
        return "#version 120\n" +
                "                                                    \n" +
                "                        uniform sampler2D texture;\n" +
                "                        uniform vec2 size;\n" +
                "                        uniform float radius;\n" +
                "                        uniform float hurt_time;\n" +
                "                        uniform float alpha;\n" +
                "                        uniform float startX, endX;\n" +
                "                        uniform float startY, endY;\n" +
                "                                                    \n" +
                "                        uniform float texXSize;\n" +
                "                        uniform float texYSize;\n" +
                "                                                    \n" +
                "                        float signedDistanceField(vec2 p, vec2 b, float r) {\n" +
                "                            return length(max(abs(p) - b, 0.0)) - r;\n" +
                "                        }\n" +
                "                                                    \n" +
                "                        void main() {\n" +
                "                            vec2 tex = gl_TexCoord[0].st;\n" +
                "                            vec2 clippedTexCoord = vec2(\n" +
                "                            mix(startX / texXSize, endX / texXSize, tex.x),\n" +
                "                            mix(startY / texYSize, endY / texYSize, tex.y)\n" +
                "                            );\n" +
                "                            vec4 smpl = texture2D(texture, clippedTexCoord);\n" +
                "                            vec2 pixel = tex * size;\n" +
                "                            vec2 centre = 0.5 * size;\n" +
                "                            float sa = smoothstep(0.0, 1, signedDistanceField(centre - pixel, centre - radius - 1, radius));\n" +
                "                            vec4 c = mix(vec4(smpl.rgb, smpl.a), vec4(smpl.rgb, 0), sa);\n" +
                "                            gl_FragColor = vec4(mix(smpl.rgb, vec3(1.0, 0.0, 0.0), hurt_time), c.a * alpha);\n" +
                "                        }";
    }
}