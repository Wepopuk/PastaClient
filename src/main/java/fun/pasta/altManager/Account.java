package fun.pasta.altManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.Pasta;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.color.ColorUtil;
import fun.pasta.util.render.RenderUtil2D;
import fun.pasta.fonts.Fonts; // если у тебя Fonts в другом пакете — поправь
import fun.pasta.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Account implements IMinecraft {

    public ResourceLocation skin;
    public String name;

    public Account(String name) {
        this.name = name;

        new Thread(() -> {
            UUID uuid = null;

            try {
                uuid = resolvedUUID(name);
            } catch (IOException e) {
                uuid = UUID.randomUUID();
            }
            skin = DefaultPlayerSkin.getDefaultSkin(uuid);

            Minecraft.getInstance().getSkinManager().loadProfileTextures(new GameProfile(uuid, name), (type, loc, tex) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) {
                    skin = loc;
                }
            }, true);
        }).start();
    }

    public static UUID resolvedUUID(String name) throws IOException {
        UUID uuid;

        InputStreamReader inputStreamReader = new InputStreamReader(new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream(), StandardCharsets.UTF_8);

        try {
            uuid = UUID.fromString(new Gson().fromJson(inputStreamReader, JsonObject.class).get("id").getAsString().replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        } catch (Throwable uuid2) {
            try {
                try {
                    inputStreamReader.close();
                } catch (Throwable throwable) {
                    uuid2.addSuppressed(throwable);
                } throw uuid2;
            } catch (Throwable throwable) {
                return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            }
        }
        inputStreamReader.close();
        return uuid;
    }

    public void draw(MatrixStack matrixStack, int mouseX, int mouseY, float x, float y, float w, float h) {

        boolean currentNick = mc.session.getUsername().equals(name);
        RenderUtil2D.drawOutlineRoundRect(x, y, w, h, 4,
                ColorUtil.rgba(26, 18, 19, currentNick ? 225 : 255), currentNick ? ColorUtil.rgba(255, 54, 65) : ColorUtil.rgba(51, 30, 32));

        /* render skin with account */
        if (skin != null) {
            RenderUtil2D.drawRoundedHead(skin, x + 3, y + 3, 16, 16, 7, 1, 0);
        }

        /* render name account */
        Fonts.mstr.get(12).drawCenterY(name, x + 16 + 5, y + h / 2f, -1);

        /* render delete account */
        Fonts.mstr.get(16).drawCenterY("X", x + w - 14, y + h / 2f, -1);
    }

    public void click(int mouseX, int mouseY, int button, float x, float y, float w, float h) {
        if (MathUtil.isHover(mouseX, mouseY, x, y, w - h, h) && button == 0) {
            mc.session = new Session(name, "", "", "mojang");
            Pasta.instance.lastAccountConfig.updateFile();
        } else if (MathUtil.isHover(mouseX, mouseY, x + w - 16 - 5, y, h, h) && button == 0) {
            Pasta.instance.altManagerScreen.accounts.remove(this);
            Pasta.instance.accountsConfig.saveAccounts(
                    Pasta.instance.altManagerScreen.accounts
            );
        }

    }
}