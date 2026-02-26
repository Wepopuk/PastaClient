package fun.pasta.altManager;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AccountsConfig {

    private File file;

    public AccountsConfig() {
        File dir = new File(Minecraft.getInstance().gameDir, "Pasta/files");
        if (!dir.exists()) dir.mkdirs();

        file = new File(dir, "accounts.pasta");
    }

    /* ===== load ===== */
    public List<Account> loadAccounts() {
        List<Account> list = new ArrayList<>();

        if (!file.exists()) return list;

        try {
            for (String line : Files.readAllLines(file.toPath())) {
                if (!line.trim().isEmpty()) {
                    list.add(new Account(line.trim()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* ===== save ===== */
    public void saveAccounts(List<Account> accounts) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Account acc : accounts) {
                writer.write(acc.name);
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
