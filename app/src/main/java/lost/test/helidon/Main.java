package lost.test.helidon;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import io.helidon.nima.http.media.jackson.JacksonSupport;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.cors.CorsSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    static Database getDatabase() {
        var dataSourceConfig =
            new DataSourceConfig()
                .setUsername("")
                .setPassword("")
                .setPlatform("sqlite")
                .setUrl("jdbc:sqlite:fighter.db");

        var config = new DatabaseConfig();
        config.setDataSourceConfig(dataSourceConfig);

        return DatabaseFactory.create(config);
    }

    static void initData(Database db) {
        LOG.info("初始化数据");
        try (var tx = db.beginTransaction()) {
            db.truncate(Fighter.class);
            db.saveAll(List.of(
                FighterBuilder.builder().name("隆").addSkill("波动拳").createdAt(now(UTC)).build(),
                FighterBuilder.builder().name("肯").addSkill("升龙拳").createdAt(now(UTC)).build()
            ));
            tx.commit();
        }
        LOG.info("初始化数据, 完成");
    }

    record StartupInfo(
        long pid,
        int port, Runtime.Version jvmVersion
    ) {

    }

    public static void main(String[] args) {
        var startupInfo = new StartupInfo(
            ProcessHandle.current().pid(),
            8000,
            Runtime.version()
        );
        LOG.info("Startup info: {}", startupInfo);

        var db = getDatabase();

        initData(db);

        var objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var jacksonSupport = JacksonSupport.create(objectMapper);

        var cors = CorsSupport.builder()
            .allowOrigins("*")
            .allowHeaders("*")
            .allowMethods("*")
            .build();

        WebServer.builder()
            .port(startupInfo.port)
            .mediaContext(c -> c.addMediaSupport(jacksonSupport))
            .routing(r -> r
                .register("/fighter", cors, new FighterRouter(db))
            )
            .build()
            .start();

    }
}
