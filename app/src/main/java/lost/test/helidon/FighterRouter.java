package lost.test.helidon;

import static io.helidon.common.http.HttpMediaType.JSON_UTF_8;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static lost.test.helidon.Result.ok;

import io.ebean.Database;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import lost.test.helidon.query.QFighter;

public class FighterRouter implements HttpService {
    private final Database db;

    public FighterRouter(Database db) {
        this.db = db;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("", this::findAll);
        rules.get("/{name}", this::findByName);
        rules.post("", this::add);
        rules.put("", this::edit);
        rules.delete("/{name}", this::delete);
    }

    private void findAll(ServerRequest req, ServerResponse res) throws Exception {
        var all = db.find(Fighter.class).findList();
        res.headers().contentType(JSON_UTF_8);
        res.send(ok(all));
    }

    private void findByName(ServerRequest req, ServerResponse res) throws Exception {
        var name = req.path().pathParameters().first("name").orElse("");
        var found = new QFighter().name.eq(name).findOneOrEmpty();
        res.headers().contentType(JSON_UTF_8);
        res.send(ok(found.orElse(null)));
    }

    private void add(ServerRequest req, ServerResponse res) throws Exception {

        var fighterCreate = req.content().as(FighterCreate.class);
        var fighterInsert = FighterBuilder.builder()
                .name(fighterCreate.name())
                .addSkill(fighterCreate.skill())
                .createdAt(now(UTC))
                .build();

        try (var tx = db.beginTransaction()) {
            db.save(fighterInsert);
            tx.commit();
        }

        res.headers().contentType(JSON_UTF_8);
        res.send(ok(fighterInsert));
    }

    private void edit(ServerRequest req, ServerResponse res) throws Exception {

        var fighterEdit = req.content().as(FighterEdit.class);

        Fighter fighterUpdate;
        try (var tx = db.beginTransaction()) {
            var found = db.find(Fighter.class)
                    .where()
                    .eq("name", fighterEdit.name())
                    .findOneOrEmpty()
                    .orElseThrow();
            fighterUpdate = FighterBuilder.builder(found)
                    .skill(fighterEdit.skill())
                    .updatedAt(now(UTC))
                    .build();
            db.update(fighterUpdate);
            tx.commit();
        }

        res.headers().contentType(JSON_UTF_8);
        res.send(ok(fighterUpdate));
    }

    private void delete(ServerRequest req, ServerResponse res) throws Exception {

        var name = req.path().pathParameters().first("name").orElse("");

        Fighter fighterDelete = null;
        try (var tx = db.beginTransaction()) {
            var found = new QFighter().name.eq(name).findOneOrEmpty();
            if (found.isPresent()) {
                if (db.delete(found.get())) fighterDelete = found.get();
            }
            tx.commit();
        }

        res.headers().contentType(JSON_UTF_8);
        res.send(ok(fighterDelete));
    }
}
