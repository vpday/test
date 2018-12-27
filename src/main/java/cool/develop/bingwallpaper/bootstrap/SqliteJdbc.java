package cool.develop.bingwallpaper.bootstrap;

import com.blade.mvc.Const;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author vpday
 * @create 2018/11/23
 */
@Slf4j
public class SqliteJdbc {

    private static final String DB_NAME = "bing-wallpaper.db";
    public static String DB_SRC;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            log.error("load sqlite driver error", e);
        }
    }

    /**
     * 测试连接并导入数据库
     */
    public static void importSql(boolean devMode) {
        String dbPath = Const.CLASSPATH + File.separatorChar + DB_NAME;

        try {
            DB_SRC = "jdbc:sqlite://" + dbPath;

            if (devMode) {
                dbPath = System.getProperty("user.dir") + "/" + DB_NAME;
                DB_SRC = "jdbc:sqlite://" + dbPath;
            }

            log.info("blade dev mode: {}", devMode);
            log.info("load sqlite database path [{}]", dbPath);
            log.info("load sqlite database src [{}]", DB_SRC);

            Connection con = DriverManager.getConnection(DB_SRC);
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='t_bing_wall_paper'");
            int count = rs.getInt(1);
            if (count == 0) {
                String cp = Objects.requireNonNull(SqliteJdbc.class.getClassLoader().getResource("")).getPath();
                InputStreamReader isr = new InputStreamReader(new FileInputStream(cp + "schema.sql"), StandardCharsets.UTF_8);

                String sql = new BufferedReader(isr).lines().collect(Collectors.joining("\n"));
                int r = statement.executeUpdate(sql);
                log.info("initialize import database - {}", r);
            }
            rs.close();
            statement.close();
            con.close();
            log.info("database path is: {}", dbPath);
        } catch (Exception e) {
            log.error("initialize database fail", e);
        }
    }
}
